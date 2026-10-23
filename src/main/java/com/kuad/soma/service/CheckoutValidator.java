package com.kuad.soma.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuad.soma.dto.CheckoutRequest;
import com.kuad.soma.dto.OrderRequest;
import com.kuad.soma.entity.Item;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** Server-side allowlist. Update store-catalog.json when changing the store catalog. */
@Component
public class CheckoutValidator {
    private final Map<String, Map<String, Boolean>> catalog;

    public CheckoutValidator(ObjectMapper mapper) throws IOException {
        try (var input = new ClassPathResource("store-catalog.json").getInputStream()) {
            catalog = mapper.readValue(input, new TypeReference<>() {});
        }
    }

    public void validate(CheckoutRequest request) {
        if (request == null) reject();
        text(request.getAccountHolder(), 50);
        text(request.getBankName(), 50);
        if (request.getAccountNumber() == null || (!request.getAccountNumber().matches("[0-9-]{5,30}") || !request.getAccountNumber().replace("-", "").matches("[0-9]{5,30}"))) reject();
        if (request.getPhoneNumber() == null || (!request.getPhoneNumber().matches("[0-9-]{9,15}") || !request.getPhoneNumber().replace("-", "").matches("[0-9]{9,15}"))) reject();
        if (request.getOrders() == null || request.getOrders().isEmpty() || request.getOrders().size() > 50) reject();
        Map<Long, Integer> quantities = new HashMap<>();
        for (OrderRequest order : request.getOrders()) {
            if (order == null || order.getItemId() == null || order.getItemId() < 1
                    || order.getQuantity() == null || order.getQuantity() < 1 || order.getQuantity() > 99) reject();
            if (quantities.merge(order.getItemId(), order.getQuantity(), Integer::sum) > 99) reject();
            if (order.getOptionName() != null && order.getOptionName().length() > 100) reject();
        }
    }

    public void validateItem(Item item, OrderRequest order) {
        Map<String, Boolean> options = catalog.get(item.getName());
        if (options == null) throw new IllegalArgumentException("판매 설정이 없는 상품입니다.");
        String option = order.getOptionName() == null ? "" : order.getOptionName().trim();
        if (options.isEmpty()) {
            if (!option.isEmpty()) throw new IllegalArgumentException("옵션이 없는 상품입니다.");
        } else if (!Boolean.TRUE.equals(options.get(option))) {
            throw new IllegalArgumentException("선택할 수 없거나 품절된 옵션입니다.");
        }
        order.setOptionName(option.isEmpty() ? null : option);
    }

    private static void text(String value, int max) {
        if (value == null || value.isBlank() || value.length() > max) reject();
    }
    private static void reject() { throw new IllegalArgumentException("주문자 정보와 주문 항목을 확인해 주세요. 수량은 상품당 1~99개입니다."); }
}
