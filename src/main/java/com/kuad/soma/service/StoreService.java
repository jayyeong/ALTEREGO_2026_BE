package com.kuad.soma.service;

import com.kuad.soma.dto.CheckoutRequest;
import com.kuad.soma.dto.ItemDTO;
import com.kuad.soma.dto.OrderRequest;
import com.kuad.soma.dto.TeamDTO;
import com.kuad.soma.entity.Item;
import com.kuad.soma.entity.Order;
import com.kuad.soma.entity.Receipt;
import com.kuad.soma.repository.ItemRepository;
import com.kuad.soma.repository.OrderRepository;
import com.kuad.soma.repository.ReceiptRepository;
import com.kuad.soma.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final CheckoutValidator checkoutValidator;
    private final ItemRepository itemRepository;
    private final OrderRepository orderRepository;
    private final ReceiptRepository receiptRepository;
    private final TeamRepository teamRepository;

    /**
     * 모든 굿즈 조회 (DTO로 변환)
     */
    @Transactional(readOnly = true)
    public List<ItemDTO> getAllItems() {
        return itemRepository.findAll().stream()
                .map(ItemDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * 팀별 굿즈 조회 (DTO로 변환)
     */
    @Transactional(readOnly = true)
    public List<ItemDTO> getItemsByTeamId(Long teamId) {
        return itemRepository.findByTeamId(teamId).stream()
                .map(ItemDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * 팀명으로 굿즈 조회 (DTO로 변환)
     */
    @Transactional(readOnly = true)
    public List<ItemDTO> getItemsByTeamName(String teamName) {
        return itemRepository.findByTeam_Name(teamName).stream()
                .map(ItemDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * 굿즈 상세 조회 (DTO로 변환)
     */
    @Transactional(readOnly = true)
    public Optional<ItemDTO> getItemById(Long itemId) {
        return itemRepository.findById(itemId)
                .map(ItemDTO::from);
    }

    /**
     * 모든 팀 정보를 DTO로 조회
     */
    @Transactional(readOnly = true)
    public List<TeamDTO> getAllTeamDTOs() {
        return teamRepository.findAll().stream()
                .map(TeamDTO::from)
                .collect(Collectors.toList());
    }


    /**
     * 주문 생성 (체크아웃)
     */
    @Transactional
    public Receipt createOrder(CheckoutRequest checkoutRequest) {
        checkoutValidator.validate(checkoutRequest);
        {
            // 영수증 생성
            Receipt receipt = Receipt.builder()
                    .accountHolder(checkoutRequest.getAccountHolder())
                    .bankName(checkoutRequest.getBankName())  // 은행명 추가
                    .accountNumber(checkoutRequest.getAccountNumber())
                    .phoneNumber(checkoutRequest.getPhoneNumber())
                    .totalAmount(BigDecimal.ZERO)
                    .orders(new ArrayList<>()) // ArrayList 명시적 초기화
                    .build();

            Receipt savedReceipt = receiptRepository.save(receipt);

            // 주문 항목 생성 및 총 금액 계산
            BigDecimal totalAmount = BigDecimal.ZERO;

            for (OrderRequest orderRequest : checkoutRequest.getOrders()) {
                Item item = itemRepository.findById(orderRequest.getItemId())
                        .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + orderRequest.getItemId()));

                if (item.isSoldOut()) {
                    throw new IllegalArgumentException("품절된 상품입니다: " + item.getName());
                }

                checkoutValidator.validateItem(item, orderRequest);

                BigDecimal itemPrice = item.getPrice();
                BigDecimal orderTotalPrice = itemPrice.multiply(BigDecimal.valueOf(orderRequest.getQuantity()));

                Order order = Order.builder()
                        .item(item)
                        .receipt(savedReceipt) // 여기서 receipt 설정
                        .quantity(orderRequest.getQuantity())
                        .optionName(orderRequest.getOptionName())
                        .totalPrice(orderTotalPrice)
                        .build();

                // 저장된 receipt에 order 추가
                savedReceipt.getOrders().add(order);
                orderRepository.save(order);

                totalAmount = totalAmount.add(orderTotalPrice);
            }

            // 총 금액 업데이트
            savedReceipt.updateTotalAmount(totalAmount);
            return receiptRepository.save(savedReceipt);
        }
    }

    /**
     * 사용자 주문 완료 페이지용 토큰 조회
     */
    @Transactional(readOnly = true)
    public Optional<Receipt> getReceiptByPublicToken(String publicToken) {
        if (publicToken == null || !publicToken.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) return Optional.empty();
        return receiptRepository.findByPublicToken(publicToken)
                .filter(receipt -> receipt.getStatus() != com.kuad.soma.enums.OrderStatus.DELETED);
    }

}
