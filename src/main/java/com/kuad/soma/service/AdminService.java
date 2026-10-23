package com.kuad.soma.service;

import com.kuad.soma.entity.Item;
import com.kuad.soma.entity.Receipt;
import com.kuad.soma.enums.OrderStatus;
import com.kuad.soma.repository.ItemRepository;
import com.kuad.soma.repository.ReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final ReceiptRepository receiptRepository;
    private final ItemRepository itemRepository;

    @Transactional(readOnly = true)
    public Page<Receipt> getReceipts(Pageable pageable) {
        // 삭제되지 않은 주문들만 조회
        return receiptRepository.findByStatusNotOrderByCreatedAtDesc(OrderStatus.DELETED, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Receipt> getReceiptById(Long id) {
        return receiptRepository.findById(id).filter(receipt -> receipt.getStatus() != OrderStatus.DELETED);
    }

    // 전체 조회 메서드 (삭제되지 않은 것만)
    @Transactional(readOnly = true)
    public List<Receipt> getAllReceipts() {
        return receiptRepository.findByStatusNotOrderByCreatedAtDesc(OrderStatus.DELETED);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getReceiptSummary() {
        long totalCount = receiptRepository.countByStatusNot(OrderStatus.DELETED);
        long pendingCount = receiptRepository.countByStatus(OrderStatus.PENDING_PAYMENT);
        long completedCount = receiptRepository.countByStatus(OrderStatus.PAYMENT_COMPLETED);
        BigDecimal totalAmount = receiptRepository.sumTotalAmountByStatusNot(OrderStatus.DELETED);

        return Map.of(
                "totalCount", totalCount,
                "pendingCount", pendingCount,
                "completedCount", completedCount,
                "totalAmount", totalAmount
        );
    }

    @Transactional(readOnly = true)
    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    @Transactional
    public Item toggleItemSoldOut(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + itemId));

        item.toggleSoldOut();
        return itemRepository.save(item);
    }

    // 주문 상태 업데이트
    @Transactional
    public Receipt updateReceiptStatus(Long id, OrderStatus status) {
        Receipt receipt = receiptRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. ID: " + id));

        receipt.updateStatus(status);
        return receiptRepository.save(receipt);
    }

    // 주문 삭제 (소프트 삭제)
    @Transactional
    public Receipt deleteReceipt(Long id) {
        Receipt receipt = receiptRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. ID: " + id));

        receipt.updateStatus(OrderStatus.DELETED);
        return receiptRepository.save(receipt);
    }

    // 상태 토글 (입금 대기 ↔ 입금 완료)
    @Transactional
    public Receipt toggleReceiptStatus(Long id) {
        Receipt receipt = receiptRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. ID: " + id));

        if (receipt.getStatus() == OrderStatus.DELETED) throw new IllegalArgumentException("삭제된 주문입니다.");

        OrderStatus newStatus = receipt.getStatus() == OrderStatus.PENDING_PAYMENT
                ? OrderStatus.PAYMENT_COMPLETED
                : OrderStatus.PENDING_PAYMENT;

        receipt.updateStatus(newStatus);
        return receiptRepository.save(receipt);
    }
}
