package com.kuad.soma.controller;

import com.kuad.soma.dto.*;
import com.kuad.soma.entity.Receipt;
import com.kuad.soma.service.StoreService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/store")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    /**
     * 모든 상품 조회
     */
    @GetMapping("/items")
    public ResponseEntity<List<ItemDTO>> getAllItems() {
        List<ItemDTO> items = storeService.getAllItems();
        return ResponseEntity.ok(items);
    }


    @GetMapping("/items/team/{teamId}")
    public ResponseEntity<List<ItemDTO>> getItemsByTeamId(@PathVariable Long teamId) {
        List<ItemDTO> items = storeService.getItemsByTeamId(teamId);
        return ResponseEntity.ok(items);
    }

    /**
     * 팀 이름으로 상품 조회
     */
    @GetMapping("/items/team/name/{teamName}")
    public ResponseEntity<List<ItemDTO>> getItemsByTeamName(@PathVariable String teamName) {
        List<ItemDTO> items = storeService.getItemsByTeamName(teamName);
        return ResponseEntity.ok(items);
    }

    /**
     * 상품 상세 조회
     */
    @GetMapping("/items/{itemId}")
    public ResponseEntity<ItemDTO> getItemById(@PathVariable Long itemId) {
        Optional<ItemDTO> item = storeService.getItemById(itemId);
        return item.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 주문 생성
     */
    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestBody CheckoutRequest checkoutRequest) {
        try {
            Receipt receipt = storeService.createOrder(checkoutRequest);
            ReceiptDTO receiptDTO = ReceiptDTO.from(receipt);
            return ResponseEntity.status(HttpStatus.CREATED).header("Cache-Control", "no-store").body(receiptDTO);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("주문 처리 중 오류가 발생했습니다."));
        }
    }

    /**
     * 모든 팀 조회
     */
    @GetMapping("/teams")
    public ResponseEntity<List<TeamDTO>> getAllTeams() {
        List<TeamDTO> teamDTOs = storeService.getAllTeamDTOs();
        return ResponseEntity.ok(teamDTOs);
    }

    /**
     * 주문 조회
     */
    @GetMapping("/receipts/{publicToken}")
    public ResponseEntity<?> getReceiptByPublicToken(@PathVariable String publicToken) {
        try {
            Optional<Receipt> receiptOpt = storeService.getReceiptByPublicToken(publicToken);
            if (receiptOpt.isPresent()) {
                ReceiptDTO receiptDTO = ReceiptDTO.from(receiptOpt.get());
                return ResponseEntity.ok().header("Cache-Control", "no-store").body(receiptDTO);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("영수증 정보를 불러오는 중 오류가 발생했습니다."));
        }
    }

    // 에러 응답용 클래스
    @Getter
    @AllArgsConstructor
    static class ErrorResponse {
        private String message;
    }
}
