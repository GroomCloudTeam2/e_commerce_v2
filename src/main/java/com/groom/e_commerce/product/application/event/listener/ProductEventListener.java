package com.groom.e_commerce.product.application.event.listener;

import java.util.List;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.groom.e_commerce.product.application.dto.StockManagement;
import com.groom.e_commerce.product.application.event.dto.OrderCancelledEvent;
import com.groom.e_commerce.payment.event.model.PaymentCompletedEvent;
import com.groom.e_commerce.payment.event.model.PaymentFailEvent;
import com.groom.e_commerce.product.application.event.dto.StockDeductedEvent;
import com.groom.e_commerce.product.application.event.dto.StockDeductionFailedEvent;
import com.groom.e_commerce.product.application.event.publisher.ProductEventPublisher;
import com.groom.e_commerce.product.application.service.ProductServiceV1;
import com.groom.e_commerce.product.infrastructure.cache.StockRedisService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Product 도메인 이벤트 리스너
 * Payment, Order 도메인에서 발행한 이벤트를 수신하여 재고 처리를 수행합니다.
 *
 * 상품 정보(items)는 가점유 시점에 Redis에 저장된 매핑을 사용합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventListener {

	private final ProductServiceV1 productServiceV1;
	private final ProductEventPublisher productEventPublisher;
	private final StockRedisService stockRedisService;

	/**
	 * 결제 완료 이벤트 처리
	 * - 가점유된 재고를 DB에서 확정 차감
	 * - 성공 시 StockDeductedEvent 발행
	 * - 실패 시 StockDeductionFailedEvent 발행
	 */
	@Async("eventExecutor")
	@EventListener
	@Transactional
	public void handlePaymentCompleted(PaymentCompletedEvent event) {
		log.info("[Product] PaymentCompletedEvent 수신 - orderId: {}", event.orderId());

		// Redis에서 주문-상품 매핑 조회
		List<StockManagement> stockManagements = stockRedisService.getOrderStockItems(event.orderId());

		if (stockManagements.isEmpty()) {
			log.error("[Product] 주문-상품 매핑을 찾을 수 없음 - orderId: {}", event.orderId());
			productEventPublisher.publishStockDeductionFailed(
				StockDeductionFailedEvent.builder()
					.orderId(event.orderId())
					.failReason("주문-상품 매핑을 찾을 수 없습니다.")
					.failedItems(List.of())
					.build()
			);
			return;
		}

		try {
			// DB 재고 확정 차감
			productServiceV1.confirmStockBulk(stockManagements);

			// 성공 이벤트 발행
			List<StockDeductedEvent.DeductedItem> deductedItems = stockManagements.stream()
				.map(item -> StockDeductedEvent.DeductedItem.builder()
					.productId(item.getProductId())
					.variantId(item.getVariantId())
					.quantity(item.getQuantity())
					.remainingStock(productServiceV1.getAvailableStock(item.getProductId(), item.getVariantId()))
					.build())
				.toList();

			productEventPublisher.publishStockDeducted(
				StockDeductedEvent.builder()
					.orderId(event.orderId())
					.items(deductedItems)
					.build()
			);

			log.info("[Product] 재고 확정 차감 완료 - orderId: {}", event.orderId());

		} catch (Exception e) {
			log.error("[Product] 재고 확정 차감 실패 - orderId: {}, error: {}", event.orderId(), e.getMessage());

			// 실패 이벤트 발행
			List<StockDeductionFailedEvent.FailedItem> failedItems = stockManagements.stream()
				.map(item -> StockDeductionFailedEvent.FailedItem.builder()
					.productId(item.getProductId())
					.variantId(item.getVariantId())
					.requestedQuantity(item.getQuantity())
					.reason(e.getMessage())
					.build())
				.toList();

			productEventPublisher.publishStockDeductionFailed(
				StockDeductionFailedEvent.builder()
					.orderId(event.orderId())
					.failReason(e.getMessage())
					.failedItems(failedItems)
					.build()
			);
		}
	}

	/**
	 * 결제 실패 이벤트 처리
	 * - 가점유된 재고를 Redis에서 복구
	 */
	@Async("eventExecutor")
	@EventListener
	@Transactional
	public void handlePaymentFail(PaymentFailEvent event) {
		log.info("[Product] PaymentFailEvent 수신 - orderId: {}, failCode: {}, failMessage: {}",
			event.orderId(), event.failCode(), event.failMessage());

		// Redis에서 주문-상품 매핑 조회
		List<StockManagement> stockManagements = stockRedisService.getOrderStockItems(event.orderId());

		if (stockManagements.isEmpty()) {
			log.warn("[Product] 주문-상품 매핑을 찾을 수 없음 - orderId: {}", event.orderId());
			return;
		}

		try {
			// Redis 가점유 재고 복구
			productServiceV1.releaseStockBulk(stockManagements);

			// 매핑 삭제
			stockRedisService.deleteOrderStockItems(event.orderId());

			log.info("[Product] 가점유 재고 복구 완료 - orderId: {}", event.orderId());

		} catch (Exception e) {
			log.error("[Product] 가점유 재고 복구 실패 - orderId: {}, error: {}", event.orderId(), e.getMessage());
		}
	}

	/**
	 * 주문 취소 이벤트 처리
	 * - Redis 가용 재고 복구 + DB 실재고 복구
	 */
	@Async("eventExecutor")
	@EventListener
	@Transactional
	public void handleOrderCancelled(OrderCancelledEvent event) {
		log.info("[Product] OrderCancelledEvent 수신 - orderId: {}, reason: {}",
			event.getOrderId(), event.getCancelReason());

		// Redis에서 주문-상품 매핑 조회
		List<StockManagement> stockManagements = stockRedisService.getOrderStockItems(event.getOrderId());

		if (stockManagements.isEmpty()) {
			log.warn("[Product] 주문-상품 매핑을 찾을 수 없음 (이미 처리됨?) - orderId: {}", event.getOrderId());
			return;
		}

		try {
			// Redis + DB 재고 복구
			productServiceV1.restoreStockBulk(stockManagements);

			// 매핑 삭제
			stockRedisService.deleteOrderStockItems(event.getOrderId());

			log.info("[Product] 재고 복구 완료 - orderId: {}", event.getOrderId());

		} catch (Exception e) {
			log.error("[Product] 재고 복구 실패 - orderId: {}, error: {}", event.getOrderId(), e.getMessage());
		}
	}
}
