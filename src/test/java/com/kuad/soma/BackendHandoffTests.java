package com.kuad.soma;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuad.soma.repository.ReceiptRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"admin.password=handoff-test-password", "spring.jpa.show-sql=false", "spring.datasource.url=jdbc:h2:mem:handoff-tests;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"})
@ActiveProfiles("local")
@AutoConfigureMockMvc
class BackendHandoffTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired ReceiptRepository receipts;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @org.junit.jupiter.api.BeforeEach void resetData() {
        receipts.deleteAll();
        jdbc.update("UPDATE item SET sold_out=false");
    }
    String order(String lines) {
        return "{\"accountHolder\":\"TEST\",\"bankName\":\"TEST BANK\",\"accountNumber\":\"000-00000\",\"phoneNumber\":\"010-0000-0000\",\"orders\":"+lines+"}";
    }
    String valid() { return order("[{\"itemId\":1,\"quantity\":2}]"); }
    String login() throws Exception {
        var result = mvc.perform(post("/api/admin/login").contentType("application/json")
                .content("{\"password\":\"handoff-test-password\"}")).andExpect(status().isOk()).andReturn();
        return "Bearer " + mapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }
    @ParameterizedTest
    @ValueSource(strings={"/receipts", "/receipts/summary", "/items", "/receipts/1", "/receipts/export"})
    void adminReadsRequireToken(String path) throws Exception {
        mvc.perform(get("/api/admin"+path)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin"+path).header("Authorization","Bearer fake")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin"+path).header("Authorization",login())).andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(200,404));
    }
    @Test void adminWritesRequireToken() throws Exception {
        mvc.perform(put("/api/admin/items/1/toggle-sold-out")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/admin/receipts/1/toggle-status")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/admin/receipts/1")).andExpect(status().isUnauthorized());
    }
    @Test void loginLogoutAndCors() throws Exception {
        mvc.perform(post("/api/admin/login").contentType("application/json").content("{\"password\":\"wrong\"}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/login").contentType("application/json").content("{}")).andExpect(status().isUnauthorized());
        String token=login();
        mvc.perform(get("/api/admin/items").header("Authorization",token)).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
        mvc.perform(post("/api/admin/logout").header("Authorization",token)).andExpect(status().isNoContent());
        mvc.perform(get("/api/admin/items").header("Authorization",token)).andExpect(status().isUnauthorized());
        mvc.perform(options("/api/admin/items").header("Origin","https://untrusted.invalid").header("Access-Control-Request-Method","GET")).andExpect(status().isForbidden());
        mvc.perform(options("/api/admin/items").header("Origin","http://localhost:5173").header("Access-Control-Request-Method","GET").header("Access-Control-Request-Headers","authorization")).andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","http://localhost:5173"));
    }
    @ParameterizedTest
    @ValueSource(strings={"null","[]","[null]","[{}]","[{\"itemId\":1}]","[{\"itemId\":1,\"quantity\":0}]","[{\"itemId\":1,\"quantity\":-1}]","[{\"itemId\":1,\"quantity\":100}]","[{\"itemId\":1,\"quantity\":2147483647}]","[{\"itemId\":999999,\"quantity\":1}]","[{\"itemId\":3,\"quantity\":1}]","[{\"itemId\":3,\"quantity\":1,\"optionName\":\"BAD\"}]","[{\"itemId\":3,\"quantity\":1,\"optionName\":\"XL (105)\"}]","[{\"itemId\":1,\"quantity\":1,\"optionName\":\"BAD\"}]","[{\"itemId\":1,\"quantity\":50},{\"itemId\":1,\"quantity\":50}]"})
    void invalidOrdersDoNotPersist(String lines) throws Exception {
        long before=receipts.count();
        mvc.perform(post("/api/store/checkout").contentType("application/json").content(order(lines))).andExpect(status().isBadRequest());
        assertThat(receipts.count()).isEqualTo(before);
    }
    @Test void malformedCustomerAndJson() throws Exception {
        for(String field:new String[]{"accountHolder","bankName","accountNumber","phoneNumber"}) {
            var body=mapper.readTree(valid()); ((com.fasterxml.jackson.databind.node.ObjectNode)body).put(field,"");
            mvc.perform(post("/api/store/checkout").contentType("application/json").content(body.toString())).andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/store/checkout").contentType("application/json").content("null")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/store/checkout").contentType("application/json").content("{")).andExpect(status().isBadRequest());
    }
    @Test void validOrderLifecycleAndPrivacy() throws Exception {
        String auth=login();
        var result=mvc.perform(post("/api/store/checkout").contentType("application/json").content(valid())).andExpect(status().isCreated()).andExpect(jsonPath("$.totalAmount").value(50000)).andReturn();
        var receipt=mapper.readTree(result.getResponse().getContentAsString());
        String id=receipt.get("id").asText(), token=receipt.get("publicToken").asText();
        mvc.perform(get("/api/store/receipts/"+token)).andExpect(status().isOk());
        mvc.perform(get("/api/store/receipts/"+id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/store/receipts/user").param("phoneNumber","010-0000-0000")).andExpect(status().isNotFound());
        mvc.perform(put("/api/admin/receipts/"+id+"/toggle-status").header("Authorization",auth)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAYMENT_COMPLETED"));
        mvc.perform(get("/api/admin/receipts/export").header("Authorization",auth)).andExpect(status().isOk()).andExpect(result2 -> assertThat(result2.getResponse().getContentAsByteArray()).startsWith((byte)'P',(byte)'K'));
        mvc.perform(delete("/api/admin/receipts/"+id).header("Authorization",auth)).andExpect(status().isOk());
        mvc.perform(get("/api/store/receipts/"+token)).andExpect(status().isNotFound());
        mvc.perform(put("/api/admin/receipts/"+id+"/toggle-status").header("Authorization",auth)).andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/receipts/"+id).header("Authorization",auth)).andExpect(status().isNotFound());
    }
    @Test void availableOptionAndSoldOutItem() throws Exception {
        mvc.perform(post("/api/store/checkout").contentType("application/json").content(order("[{\"itemId\":3,\"quantity\":1,\"optionName\":\"M (95)\"}]"))).andExpect(status().isCreated()).andExpect(jsonPath("$.totalAmount").value(15000));
        mvc.perform(put("/api/admin/items/1/toggle-sold-out").header("Authorization",login())).andExpect(status().isOk());
        mvc.perform(post("/api/store/checkout").contentType("application/json").content(valid())).andExpect(status().isBadRequest());
    }
    @Test void fractionalQuantityAndClientPrice() throws Exception {
        mvc.perform(post("/api/store/checkout").contentType("application/json")
                .content(order("[{\"itemId\":1,\"quantity\":1.5}]"))).andExpect(status().isBadRequest());
        var body=(com.fasterxml.jackson.databind.node.ObjectNode)mapper.readTree(valid());
        body.put("totalAmount", -1);
        mvc.perform(post("/api/store/checkout").contentType("application/json").content(body.toString()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.totalAmount").value(50000));
    }

    @Test void paginationBounds() throws Exception {
        String auth=login();
        for(String query:new String[]{"page=-1","size=0","size=101"})
            mvc.perform(get("/api/admin/receipts?"+query).header("Authorization",auth)).andExpect(status().isBadRequest());
    }
}
