package com.kuad.soma.controller;

import com.kuad.soma.dto.PageResponseDto;
import com.kuad.soma.dto.ItemDTO;
import com.kuad.soma.dto.ReceiptDTO;
import com.kuad.soma.entity.Item;
import com.kuad.soma.entity.Receipt;
import com.kuad.soma.enums.OrderStatus;
import com.kuad.soma.service.AdminService;
import com.kuad.soma.service.ExcelExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final com.kuad.soma.service.AdminAuthService auth;

    private final AdminService adminService;
    private final ExcelExportService excelExportService;

    /**
     * 관리자 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<?> adminLogin(@RequestBody Map<String, String> creds) {
        return auth.login(creds.get("password"))
                .<ResponseEntity<?>>map(token -> ResponseEntity.ok().header("Cache-Control", "no-store")
                        .body(Map.of("success", true, "token", token)))
                .orElseGet(() -> ResponseEntity.status(401).body(Map.of("success", false, "message", "Invalid password")));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(jakarta.servlet.http.HttpServletRequest request) {
        auth.logout(com.kuad.soma.config.AdminAuthInterceptor.token(request));
        return ResponseEntity.noContent().build();
    }

    /**
     * 주문 내역 페이징 조회 (DTO) - 삭제되지 않은 것만
     */
    @GetMapping(value = "/receipts", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PageResponseDto<ReceiptDTO>> getReceipts(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        int p = (page == null ? 0 : page);
        int s = (size == null ? 20 : size);

        if (p < 0 || s < 1 || s > 100) return ResponseEntity.badRequest().build();

        Page<Receipt> receiptPage = adminService.getReceipts(PageRequest.of(p, s));

        List<ReceiptDTO> dtoList = receiptPage.getContent().stream()
                .map(ReceiptDTO::from)
                .collect(Collectors.toList());

        PageResponseDto<ReceiptDTO> response = new PageResponseDto<>(
                dtoList,
                receiptPage.getTotalPages(),
                receiptPage.getTotalElements()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 관리자 주문 요약
     */
    @GetMapping(value = "/receipts/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getReceiptSummary() {
        return ResponseEntity.ok(adminService.getReceiptSummary());
    }

    /**
     * 관리자 상품 목록 조회
     */
    @GetMapping(value = "/items", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ItemDTO>> getItems() {
        List<ItemDTO> items = adminService.getAllItems().stream()
                .map(ItemDTO::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(items);
    }

    /**
     * 상품 품절 상태 토글
     */
    @PutMapping("/items/{id}/toggle-sold-out")
    public ResponseEntity<ItemDTO> toggleItemSoldOut(@PathVariable Long id) {
        try {
            Item updatedItem = adminService.toggleItemSoldOut(id);
            return ResponseEntity.ok(ItemDTO.from(updatedItem));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 단일 주문 상세 조회 (DTO)
     */
    @GetMapping(value = "/receipts/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReceiptDTO> getReceiptById(@PathVariable Long id) {
        return adminService.getReceiptById(id)
                .map(ReceiptDTO::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 주문 상태 토글 (입금 대기 ↔ 입금 완료)
     */
    @PutMapping("/receipts/{id}/toggle-status")
    public ResponseEntity<ReceiptDTO> toggleReceiptStatus(@PathVariable Long id) {
        try {
            Receipt updatedReceipt = adminService.toggleReceiptStatus(id);
            return ResponseEntity.ok(ReceiptDTO.from(updatedReceipt));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 주문 삭제 (소프트 삭제)
     */
    @DeleteMapping("/receipts/{id}")
    public ResponseEntity<Map<String, Object>> deleteReceipt(@PathVariable Long id) {
        try {
            Receipt deletedReceipt = adminService.deleteReceipt(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "주문이 삭제되었습니다.",
                    "receipt", ReceiptDTO.from(deletedReceipt)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Excel 내보내기 (입금 대기, 입금 완료 상태만)
     */
    @GetMapping("/receipts/export")
    public ResponseEntity<byte[]> exportReceiptsToExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            ByteArrayOutputStream out = excelExportService.exportReceiptsToExcel(startDate, endDate);
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=receipts_" + LocalDate.now() + ".xlsx")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(out.toByteArray());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
