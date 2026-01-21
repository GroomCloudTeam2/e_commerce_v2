package com.groom.e_commerce.product.application.event.listener;

import java.util.List;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.groom.e_commerce.product.application.dto.StockManagement;
import com.groom.e_commerce.product.application.event.dto.OrderCancelledEvent;
import com.groom.e_commerce.product.application.event.dto.PaymentCompletedEvent;
import com.groom.e_commerce.product.application.event.dto.PaymentFailEvent;
import com.groom.e_commerce.product.application.event.dto.StockDeductedEvent;
import com.groom.e_commerce.product.application.event.dto.StockDeductionFailedEvent;
import com.groom.e_commerce.product.application.event.publisher.ProductEventPublisher;
import com.groom.e_commerce.product.application.service.ProductServiceV1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Product 도메인 이벤트 리스너
 * Payment, Order 도메인에서 발행한 이벤트를 수신하여 재고 처리를 수행합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventListener {

	private final ProductServiceV1 productServiceV1;
	private final ProductEventPublisher productEventPublisher;

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
		log.info("[Product] PaymentCompletedEvent 수신 - orderId: {}", event.getOrderId());

		try {
			List<StockManagement> stockManagements = event.getItems().stream()
				.map(item -> StockManagement.of(
					item.getProductId(),
					item.getVariantId(),
					item.getQuantity()
				))
				.toList();

			// DB 재고 확정 차감
			productServiceV1.confirmStockBulk(stockManagements);

			// 성공 이벤트 발행
			List<StockDeductedEvent.DeductedItem> deductedItems = event.getItems().stream()
				.map(item -> StockDeductedEvent.DeductedItem.builder()
					.productId(item.getProductId())
					.variantId(item.getVariantId())
					.quantity(item.getQuantity())
					.remainingStock(productServiceV1.getAvailableStock(item.getProductId(), item.getVariantId()))
					.build())
				.toList();

			productEventPublisher.publishStockDeducted(
				StockDeductedEvent.builder()
					.orderId(event.getOrderId())
					.items(deductedItems)
					.build()
			);

			log.info("[Product] 재고 확정 차감 완료 - orderId: {}", event.getOrderId());

		} catch (Exception e) {
			log.error("[Product] 재고 확정 차감 실패 - orderId: {}, error: {}", event.getOrderId(), e.getMessage());

			// 실패 이벤트 발행
			List<StockDeductionFailedEvent.FailedItem> failedItems = event.getItems().stream()
				.map(item -> StockDeductionFailedEvent.FailedItem.builder()
					.productId(item.getProductId())
					.variantId(item.getVariantId())
					.requestedQuantity(item.getQuantity())
					.reason(e.getMessage())
					.build())
				.toList();

			productEventPublisher.publishStockDeductionFailed(
				StockDeductionFailedEvent.builder()
					.orderId(event.getOrderId())
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
		log.info("[Product] PaymentFailEvent 수신 - orderId: {}, reason: {}",
			event.getOrderId(), event.getFailReason());

		try {
			List<StockManagement> stockManagements = event.getItems().stream()
				.map(item -> StockManagement.of(
					item.getProductId(),
					item.getVariantId(),
					item.getQuantity()
				))
				.toList();

			// Redis 가점유 재고 복구
			productServiceV1.releaseStockBulk(stockManagements);

			log.info("[Product] 가점유 재고 복구 완료 - orderId: {}", event.getOrderId());

		} catch (Exception e) {
			log.error("[Product] 가점유 재고 복구 실패 - orderId: {}, error: {}", event.getOrderId(), e.getMessage());
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

		try {
			List<StockManagement> stockManagements = event.getItems().stream()
				.map(item -> StockManagement.of(
					item.getProductId(),
					item.getVariantId(),
					item.getQuantity()
				))
				.toList();

			// Redis + DB 재고 복구
			productServiceV1.restoreStockBulk(stockManagements);

			log.info("[Product] 재고 복구 완료 - orderId: {}", event.getOrderId());

		} catch (Exception e) {
			log.error("[Product] 재고 복구 실패 - orderId: {}, error: {}", event.getOrderId(), e.getMessage());
		}
	}
}