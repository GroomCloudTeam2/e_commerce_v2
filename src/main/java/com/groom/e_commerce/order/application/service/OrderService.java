package com.groom.e_commerce.order.application.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groom.e_commerce.order.domain.entity.Order;
import com.groom.e_commerce.order.domain.event.outbound.OrderCreatedEvent;
import com.groom.e_commerce.order.domain.repository.OrderRepository;
import com.groom.e_commerce.order.infrastructure.client.ProductClient;
import com.groom.e_commerce.order.infrastructure.client.UserClient;
import com.groom.e_commerce.order.domain.entity.OrderItem;
import com.groom.e_commerce.order.infrastructure.client.dto.UserAddressResponse;
import com.groom.e_commerce.order.presentation.dto.request.OrderCreateRequest;
import com.groom.e_commerce.order.presentation.dto.response.OrderResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserClient userClient;
    private final ProductClient productClient;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 주문을 생성하고, OrderCreatedEvent를 발행합니다.
     */
    @Transactional
    public UUID createOrder(UUID userId, OrderCreateRequest request) {
        // 1. 사용자 검증 (Synchronous)
        userClient.isValidUser(userId, userId);

        // 2. 주소 정보 조회 (Snapshot용)
        UserAddressResponse address = userClient.getUserAddress(userId, userId);

        // 3. Order 엔티티 생성 (Transient)
        Order order = Order.builder()
                .buyerId(userId)
                .orderNumber(generateOrderNumber())
                .totalPaymentAmount(request.getTotalAmount())
                .recipientName(address.getRecipientName())
                .recipientPhone(address.getRecipientPhone())
                .zipCode(address.getZipCode())
                .shippingAddress(address.getAddress() + " " + address.getDetailAddress())
                .shippingMemo("부재 시 문 앞에 놓아주세요") // TODO: Request에서 받거나 기본값
                .build();

        // 4. OrderItem 생성 및 추가
        for (var itemRequest : request.getItems()) {
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .productId(itemRequest.getProductId())
                    .variantId(itemRequest.getVariantId())
                    .ownerId(UUID.randomUUID()) // TODO: Product Owner ID 필요 (ProductClient에서 받아오거나 Request에 포함)
                    .productTitle(itemRequest.getProductTitle())
                    .productThumbnail(itemRequest.getProductThumbnail())
                    .optionName(itemRequest.getOptionName())
                    .unitPrice(itemRequest.getUnitPrice())
                    .quantity(itemRequest.getQuantity())
                    .build();

            order.addItem(orderItem);

            // 재고 가점유 요청
            productClient.reserveStock(new com.groom.e_commerce.order.infrastructure.client.dto.StockReserveRequest(
                    itemRequest.getProductId(),
                    itemRequest.getVariantId(),
                    itemRequest.getQuantity()));
        }

        // 5. DB 저장
        Order savedOrder = orderRepository.save(order);
        UUID orderId = savedOrder.getOrderId();

        // 6. 결제 요청 이벤트 발행
        eventPublisher.publishEvent(new OrderCreatedEvent(orderId, order.getTotalPaymentAmount()));

        log.info("주문(ID: {})이 생성되었습니다. 결제 프로세스를 시작합니다.", orderId);
        return orderId;
    }

    private String generateOrderNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int randomPart = ThreadLocalRandom.current().nextInt(100000, 999999);
        return datePart + "-" + randomPart;
    }

    public Page<OrderResponse> getMyOrders(UUID buyerId, Pageable pageable) {
        return orderRepository.findAllByBuyerId(buyerId, pageable)
                .map(OrderResponse::from);
    }

    public OrderResponse getOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId) // Assuming findById works with UUID as per previous analysis
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. ID: " + orderId));

        return OrderResponse.from(order);
    }

    public List<OrderResponse> getOrdersByProduct(UUID productId) {
        return orderRepository.findAllByProductId(productId).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional
    public void cancelOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. ID: " + orderId));
        order.cancel();
        orderRepository.save(order);
    }

    public com.groom.e_commerce.order.presentation.dto.internal.OrderValidationResponse getOrderForPayment(
            UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. ID: " + orderId));
        return new com.groom.e_commerce.order.presentation.dto.internal.OrderValidationResponse(
                order.getOrderId(),
                order.getTotalPaymentAmount(),
                order.getStatus());
    }
}
