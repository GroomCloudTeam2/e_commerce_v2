package com.groom.e_commerce.order.application.service;

import com.groom.e_commerce.order.domain.entity.Order;
import com.groom.e_commerce.order.domain.event.outbound.OrderCancelledEvent;
import com.groom.e_commerce.order.domain.event.outbound.OrderConfirmedEvent;
import com.groom.e_commerce.order.domain.event.outbound.OrderCreatedEvent;
import com.groom.e_commerce.order.domain.repository.OrderRepository;
import com.groom.e_commerce.order.presentation.dto.request.OrderCreateRequest;
import com.groom.e_commerce.order.presentation.dto.response.OrderResponse;

import java.awt.print.Pageable;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 주문을 생성하고, OrderCreatedEvent를 발행합니다.
     */
    @Transactional
    public UUID createOrder(UUID userId, OrderCreateRequest request) {
        // ... 사용자 및 주소 확인, 상품 정보 조회 등 로직 ...
        // Order order = Order.create(...)
        // Order order = new Order(); // Placeholder for actual creation
		Order order = new Order(userId, "ORD-" + UUID.randomUUID().toString().substring(0, 8),
			request.getTotalAmount(), request.toOrderItems());
        Order savedOrder = orderRepository.save(order);
        UUID orderId = savedOrder.getOrderId();

        // 트랜잭션이 성공적으로 커밋된 후 결제 요청 이벤트를 발행합니다.
        eventPublisher.publishEvent(new OrderCreatedEvent(orderId));
        log.info("주문(ID: {})이 생성되었습니다. 결제 프로세스를 시작합니다.", orderId);

        return orderId;
    }

	@Transactional(readOnly = true)
	public
	Page<OrderResponse> getMyOrders(UUID buyerId, Pageable pageable) {
		return orderRepository.findAllByBuyerId(buyerId, pageable)
			.map(OrderResponse::from);
	}

	@Transactional(readOnly = true)
	public OrderResponse getOrder(UUID orderId) {
		return OrderResponse.from(findOrderById(orderId));
	}

	@Transactional(readOnly = true)
	public List<OrderResponse> getOrdersByProduct(UUID productId) {
		return orderRepository.findAllByProductId(productId).stream()
			.map(OrderResponse::from)
			.toList();
	}

    /**
     * 결제 완료를 처리합니다.
     * @param orderId 주문 ID
     */
    public void completePayment(UUID orderId) {
        Order order = findOrderById(orderId);
        if (order.markPaid()) {
            orderRepository.save(order);
            log.info("주문(ID: {}) 상태를 PAID로 변경했습니다.", orderId);
        } else {
            log.warn("주문(ID: {})은 PENDING 상태가 아니므로 상태를 변경하지 않았습니다. (현재 상태: {}). 중복 이벤트이거나 로직 오류일 수 있습니다.",
                orderId, order.getStatus());
        }
    }

    /**
     * 결제 실패를 처리합니다.
     * @param orderId 주문 ID
     */
    public void failPayment(UUID orderId) {
        Order order = findOrderById(orderId);
        if (order.markFailed()) {
            orderRepository.save(order);
            log.info("주문(ID: {}) 상태를 FAILED로 변경했습니다.", orderId);
        } else {
            log.warn("주문(ID: {})은 PENDING 상태가 아니므로 상태를 변경하지 않았습니다. (현재 상태: {}).",
                orderId, order.getStatus());
        }
    }

    /**
     * 주문을 확정합니다. (재고 차감 완료)
     * @param orderId 주문 ID
     */
    public void confirmOrder(UUID orderId) {
        Order order = findOrderById(orderId);
        if (order.markConfirmed()) {
            orderRepository.save(order);
            // 주문 확정 이벤트 발행 (장바구니 정리 등 후속 처리를 위해)
            eventPublisher.publishEvent(new OrderConfirmedEvent(orderId));
            log.info("주문(ID: {})이 최종 확정되었습니다. (상태: CONFIRMED)", orderId);
        } else {
            log.warn("주문(ID: {})은 PAID 상태가 아니므로 확정 처리하지 않았습니다. (현재 상태: {}).",
                orderId, order.getStatus());
        }
    }

    /**
     * 주문을 취소합니다. (환불 성공 시)
     * @param orderId 주문 ID
     */
    public void cancelOrder(UUID orderId) {
        Order order = findOrderById(orderId);
        if (order.markCancelled()) {
            orderRepository.save(order);
            eventPublisher.publishEvent(new OrderCancelledEvent(orderId));
            log.info("주문(ID: {})이 최종 취소되었습니다. (상태: CANCELLED)", orderId);
        } else {
            log.warn("주문(ID: {})을 취소 상태로 변경할 수 없습니다. (현재 상태: {}).",
                orderId, order.getStatus());
        }
    }

    /**
     * 환불 실패 시 주문 상태를 MANUAL_CHECK로 변경합니다.
     * @param orderId 주문 ID
     */
    public void failRefund(UUID orderId) {
        Order order = findOrderById(orderId);
        if (order.needsManualCheck()) {
            orderRepository.save(order);
            log.error("주문(ID: {})의 환불에 실패하여 수동 확인이 필요합니다. (상태: MANUAL_CHECK)", orderId);
        } else {
            log.warn("주문(ID: {})을 수동 확인 상태로 변경할 수 없습니다. (현재 상태: {}).",
                orderId, order.getStatus());
        }
    }

    private Order findOrderById(UUID orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. ID: " + orderId));
    }
}

