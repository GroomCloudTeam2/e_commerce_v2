package com.groom.e_commerce.payment.application.service;

import static com.groom.e_commerce.global.presentation.advice.ErrorCode.*;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groom.e_commerce.payment.domain.entity.Payment;
import com.groom.e_commerce.payment.domain.model.PaymentStatus;
import com.groom.e_commerce.payment.domain.repository.PaymentRepository;
import com.groom.e_commerce.payment.infrastructure.config.TossPaymentsProperties;
import com.groom.e_commerce.payment.presentation.dto.request.ReqReadyPayment;
import com.groom.e_commerce.payment.presentation.dto.response.ResReadyPayment;
import com.groom.e_commerce.payment.presentation.exception.PaymentException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentReadyService {

	private static final String PG_TOSS = "TOSS";

	private final PaymentRepository paymentRepository;
	private final TossPaymentsProperties tossProps;

	/**
	 * 결제창 오픈용 ready 정보 제공
	 * - amount는 Payment에 저장된 값을 SSOT로 사용
	 * - 요청 amount는 "검증용"으로만 사용(변조 방지)
	 * - Payment 상태가 READY일 때만 허용
	 */
	@Transactional
	public ResReadyPayment ready(ReqReadyPayment req) {
		UUID orderId = req.orderId();
		Long requestedAmount = req.amount();

		Payment payment = paymentRepository.findByOrderId(orderId)
			.orElseThrow(() -> new PaymentException(
				PAYMENT_NOT_FOUND,
				"Payment not found. orderId=" + orderId
			));

		// READY만 결제창 오픈 허용
		if (payment.getStatus() != PaymentStatus.READY) {
			throw new PaymentException(
				PAYMENT_NOT_CONFIRMABLE,
				"Payment is not READY. status=" + payment.getStatus()
			);
		}

		// 설정 검증(서버 설정 누락 방지)
		if (isBlank(tossProps.clientKey())) {
			throw new PaymentException(PAYMENT_CONFIG_ERROR, "toss.payments.clientKey missing");
		}
		if (isBlank(tossProps.successUrl()) || isBlank(tossProps.failUrl())) {
			throw new PaymentException(PAYMENT_CONFIG_ERROR, "toss.payments.successUrl/failUrl missing");
		}

		// SSOT: 서버에 저장된 금액
		Long storedAmount = payment.getAmount();

		// 요청 amount와 서버 저장 amount가 다르면 차단
		// (프론트/클라이언트가 임의로 금액을 바꿔 결제창 띄우는 걸 막음)
		if (requestedAmount == null || requestedAmount <= 0) {
			throw new PaymentException(PAYMENT_INVALID_AMOUNT, "amount is invalid. amount=" + requestedAmount);
		}
		if (!storedAmount.equals(requestedAmount)) {
			throw new PaymentException(
				PAYMENT_INVALID_AMOUNT,
				"amount mismatch. stored=" + storedAmount + ", requested=" + requestedAmount
			);
		}

		return new ResReadyPayment(
			orderId,
			storedAmount,
			tossProps.clientKey(),
			tossProps.successUrl(),
			tossProps.failUrl()
		);

	}

	private boolean isBlank(String v) {
		return v == null || v.isBlank();
	}
}
