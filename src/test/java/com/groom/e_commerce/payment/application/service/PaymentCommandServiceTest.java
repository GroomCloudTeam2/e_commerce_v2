package com.groom.e_commerce.payment.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.groom.e_commerce.payment.domain.entity.Payment;
import com.groom.e_commerce.payment.domain.entity.PaymentCancel;
import com.groom.e_commerce.payment.domain.model.PaymentStatus;
import com.groom.e_commerce.payment.domain.repository.PaymentCancelRepository;
import com.groom.e_commerce.payment.domain.repository.PaymentRepository;
import com.groom.e_commerce.payment.event.publisher.PaymentEventPublisher;
import com.groom.e_commerce.payment.infrastructure.feign.TossPaymentsClient;
import com.groom.e_commerce.payment.infrastructure.feign.TossPaymentsClient.TossCancelRequest;
import com.groom.e_commerce.payment.infrastructure.feign.TossPaymentsClient.TossCancelResponse;
import com.groom.e_commerce.payment.infrastructure.feign.TossPaymentsClient.TossConfirmRequest;
import com.groom.e_commerce.payment.infrastructure.feign.TossPaymentsClient.TossConfirmResponse;
import com.groom.e_commerce.payment.presentation.dto.request.ReqCancelPayment;
import com.groom.e_commerce.payment.presentation.dto.request.ReqConfirmPayment;
import com.groom.e_commerce.payment.presentation.dto.response.ResCancelResult;
import com.groom.e_commerce.payment.presentation.dto.response.ResPayment;
import com.groom.e_commerce.payment.presentation.exception.PaymentException;
import com.groom.e_commerce.payment.presentation.exception.TossApiException;

@ExtendWith(MockitoExtension.class)
class PaymentCommandServiceTest {

	@Mock PaymentRepository paymentRepository;
	@Mock PaymentCancelRepository paymentCancelRepository;
	@Mock TossPaymentsClient tossPaymentsClient;
	@Mock PaymentEventPublisher paymentEventPublisher;

	@InjectMocks PaymentCommandService paymentCommandService;

	UUID orderId;
	String paymentKey;
	Long amount;

	@BeforeEach
	void setUp() {
		orderId = UUID.randomUUID();
		paymentKey = "pay_" + UUID.randomUUID();
		amount = 10_000L;
	}

	@Nested
	@DisplayName("createReady")
	class CreateReadyTests {

		@Test
		@DisplayName("orderId로 Payment가 이미 있으면 no-op (save 호출 안 함)")
		void createReady_idempotent_noop() {
			Payment existing = mock(Payment.class);
			when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(existing));

			paymentCommandService.createReady(orderId, amount);

			verify(paymentRepository, never()).save(any(Payment.class));
		}

		@Test
		@DisplayName("orderId로 Payment가 없으면 READY 저장")
		void createReady_saveReady() {
			when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

			paymentCommandService.createReady(orderId, amount);

			verify(paymentRepository).save(any(Payment.class));
		}
	}

	@Nested
	@DisplayName("confirm")
	class ConfirmTests {

		@Test
		@DisplayName("이미 PAID면 멱등: Toss 호출 없이 기존 결과 반환")
		void confirm_whenAlreadyPaid_returnExisting() {
			Payment paid = mock(Payment.class);
			when(paid.getStatus()).thenReturn(PaymentStatus.PAID);

			when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(paid));

			ReqConfirmPayment req = new ReqConfirmPayment(paymentKey, orderId, amount);

			ResPayment res = paymentCommandService.confirm(req);

			assertThat(res).isNotNull();
			verifyNoInteractions(tossPaymentsClient);
			verify(paymentEventPublisher, never()).publishPaymentCompleted(any(), any(), anyLong());
			verify(paymentEventPublisher, never()).publishPaymentFailed(any(), any(), anyLong(), any(), any());
		}

		@Test
		@DisplayName("CANCELLED면 승인 불가 예외")
		void confirm_whenCancelled_throw() {
			Payment p = mock(Payment.class);
			when(p.getStatus()).thenReturn(PaymentStatus.CANCELLED);
			when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(p));

			ReqConfirmPayment req = new ReqConfirmPayment(paymentKey, orderId, amount);

			assertThatThrownBy(() -> paymentCommandService.confirm(req))
				.isInstanceOf(PaymentException.class);

			verifyNoInteractions(tossPaymentsClient);
		}

		@Test
		@DisplayName("READY가 아니면 승인 불가 예외")
		void confirm_whenNotReady_throw() {
			Payment p = mock(Payment.class);
			when(p.getStatus()).thenReturn(null); // ✅ UNKNOWN 대신 null
			when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(p));

			ReqConfirmPayment req = new ReqConfirmPayment(paymentKey, orderId, amount);

			assertThatThrownBy(() -> paymentCommandService.confirm(req))
				.isInstanceOf(PaymentException.class);

			verifyNoInteractions(tossPaymentsClient);
		}

		@Test
		@DisplayName("Toss confirm에서 TossApiException(정책 실패) → FAILED 저장 + 실패 이벤트 발행 + 예외 전파")
		void confirm_whenTossApiException_markFailed_publish_andThrow() {
			Payment ready = mock(Payment.class);
			when(ready.getStatus()).thenReturn(PaymentStatus.READY);
			when(ready.getAmount()).thenReturn(amount);

			when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(ready));

			// TossApiException 구성은 프로젝트 구현에 따라 다를 수 있음.
			// 여기서는 mock으로 처리.
			TossApiException tossEx = mock(TossApiException.class);
			// e.getErrorCode().getCode(), getMessage() 호출하니까 중첩 mock 필요
			var errorCode = mock(com.groom.e_commerce.global.presentation.advice.ErrorCode.class);
			when(tossEx.getErrorCode()).thenReturn(errorCode);
			when(errorCode.getCode()).thenReturn("TOSS_DENIED");
			when(errorCode.getMessage()).thenReturn("Denied");

			when(tossPaymentsClient.confirm(any(TossConfirmRequest.class))).thenThrow(tossEx);

			ReqConfirmPayment req = new ReqConfirmPayment(paymentKey, orderId, 999999L); // 요청값 조작해도 service는 payment.amount 사용

			assertThatThrownBy(() -> paymentCommandService.confirm(req))
				.isSameAs(tossEx);

			verify(ready).markFailed(eq("TOSS_DENIED"), eq("Denied"));
			verify(paymentRepository).save(ready);
			verify(paymentEventPublisher).publishPaymentFailed(eq(orderId), eq(paymentKey), eq(amount), eq("TOSS_DENIED"), eq("Denied"));
		}

		@Test
		@DisplayName("Toss confirm에서 일반 예외(타임아웃 등) → UNKNOWN 실패 처리 + 실패 이벤트 + PaymentException")
		void confirm_whenUnknownException_markFailed_publish_andThrowPaymentException() {
			Payment ready = mock(Payment.class);
			when(ready.getStatus()).thenReturn(PaymentStatus.READY);
			when(ready.getAmount()).thenReturn(amount);
			when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(ready));

			when(tossPaymentsClient.confirm(any(TossConfirmRequest.class)))
				.thenThrow(new RuntimeException("timeout"));

			ReqConfirmPayment req = new ReqConfirmPayment(paymentKey, orderId, amount);

			assertThatThrownBy(() -> paymentCommandService.confirm(req))
				.isInstanceOf(PaymentException.class);

			verify(ready).markFailed(eq("TOSS_CONFIRM_UNKNOWN"), contains("timeout"));
			verify(paymentRepository).save(ready);
			verify(paymentEventPublisher).publishPaymentFailed(eq(orderId), eq(paymentKey), eq(amount), eq("TOSS_CONFIRM_UNKNOWN"), contains("timeout"));
		}

		@Test
		@DisplayName("Toss confirm 성공(status=DONE) → PAID 저장 + 완료 이벤트 발행")
		void confirm_whenDone_markPaid_publishCompleted() {
			Payment ready = mock(Payment.class);
			when(ready.getStatus()).thenReturn(PaymentStatus.READY);
			when(ready.getAmount()).thenReturn(amount);
			when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(ready));

			TossConfirmResponse tossRes = mock(TossConfirmResponse.class);
			when(tossRes.status()).thenReturn("DONE");
			when(tossRes.approvedAt()).thenReturn("2026-01-21T09:00:00+09:00");

			when(tossPaymentsClient.confirm(any(TossConfirmRequest.class))).thenReturn(tossRes);

			ReqConfirmPayment req = new ReqConfirmPayment(paymentKey, orderId, 123L);

			ResPayment res = paymentCommandService.confirm(req);

			assertThat(res).isNotNull();
			verify(ready).markPaid(eq(paymentKey), eq(amount));
			verify(paymentRepository).save(ready);
			verify(paymentEventPublisher).publishPaymentCompleted(eq(orderId), eq(paymentKey), eq(amount));
			verify(paymentEventPublisher, never()).publishPaymentFailed(any(), any(), anyLong(), any(), any());
		}

		@Test
		@DisplayName("Toss confirm 2xx지만 status!=DONE → FAILED 저장 + 실패 이벤트 + PaymentException")
		void confirm_whenNotDone_markFailed_publishFailed_andThrow() {
			Payment ready = mock(Payment.class);
			when(ready.getStatus()).thenReturn(PaymentStatus.READY);
			when(ready.getAmount()).thenReturn(amount);
			when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(ready));

			TossConfirmResponse tossRes = mock(TossConfirmResponse.class);
			when(tossRes.status()).thenReturn("WAITING_FOR_DEPOSIT"); // 예시
			when(tossPaymentsClient.confirm(any(TossConfirmRequest.class))).thenReturn(tossRes);

			ReqConfirmPayment req = new ReqConfirmPayment(paymentKey, orderId, amount);

			assertThatThrownBy(() -> paymentCommandService.confirm(req))
				.isInstanceOf(PaymentException.class);

			verify(ready).markFailed(eq("TOSS_NOT_DONE"), contains("WAITING_FOR_DEPOSIT"));
			verify(paymentRepository).save(ready);
			verify(paymentEventPublisher).publishPaymentFailed(eq(orderId), eq(paymentKey), eq(amount), eq("TOSS_NOT_DONE"), contains("WAITING_FOR_DEPOSIT"));
		}

		@Test
		@DisplayName("confirm 요청 amount는 믿지 않고, Payment.amount로 TossConfirmRequest가 만들어진다")
		void confirm_amountUsesPaymentAmount_notRequestAmount() {
			Payment ready = mock(Payment.class);
			when(ready.getStatus()).thenReturn(PaymentStatus.READY);
			when(ready.getAmount()).thenReturn(10_000L);
			when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(ready));

			TossConfirmResponse tossRes = mock(TossConfirmResponse.class);
			when(tossRes.status()).thenReturn("DONE");
			when(tossRes.approvedAt()).thenReturn("2026-01-21T09:00:00+09:00");
			when(tossPaymentsClient.confirm(any(TossConfirmRequest.class))).thenReturn(tossRes);

			ReqConfirmPayment req = new ReqConfirmPayment(paymentKey, orderId, 999_999L);

			paymentCommandService.confirm(req);

			ArgumentCaptor<TossConfirmRequest> captor = ArgumentCaptor.forClass(TossConfirmRequest.class);
			verify(tossPaymentsClient).confirm(captor.capture());

			TossConfirmRequest actual = captor.getValue();
			assertThat(actual.amount()).isEqualTo(10_000L);
			assertThat(actual.orderId()).isEqualTo(orderId.toString());
			assertThat(actual.paymentKey()).isEqualTo(paymentKey);
		}
	}

	@Nested
	@DisplayName("cancel")
	class CancelTests {

		@Test
		@DisplayName("이미 CANCELLED면 멱등 성공 반환(외부 호출 없음)")
		void cancel_whenAlreadyCancelled_returnSuccess() {
			Payment p = mock(Payment.class);
			when(p.getStatus()).thenReturn(PaymentStatus.CANCELLED);

			when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(p));

			ReqCancelPayment req = new ReqCancelPayment(orderId, "whatever");
			ResCancelResult res = paymentCommandService.cancel(req);

			assertThat(res).isNotNull();
			verifyNoInteractions(tossPaymentsClient);
		}

		@Test
		@DisplayName("PAID가 아니면 PG 호출 없이 종료")
		void cancel_whenNotPaid_noop() {
			Payment p = mock(Payment.class);
			when(p.getStatus()).thenReturn(PaymentStatus.READY);

			when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(p));

			ReqCancelPayment req = new ReqCancelPayment(orderId, "x");
			ResCancelResult res = paymentCommandService.cancel(req);

			assertThat(res).isNotNull();
			verifyNoInteractions(tossPaymentsClient);
		}

		@Test
		@DisplayName("PAID인데 paymentKey 없으면 내부 오류 예외")
		void cancel_whenPaidButMissingPaymentKey_throw() {
			Payment p = mock(Payment.class);
			when(p.getStatus()).thenReturn(PaymentStatus.PAID);
			when(p.getPaymentKey()).thenReturn("   "); // blank
			when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(p));

			ReqCancelPayment req = new ReqCancelPayment(orderId, "x");

			assertThatThrownBy(() -> paymentCommandService.cancel(req))
				.isInstanceOf(PaymentException.class);

			verifyNoInteractions(tossPaymentsClient);
		}

		@Test
		@DisplayName("Toss cancel TossApiException → refundFailed 기록 + 실패 이벤트 + 실패 결과 반환")
		void cancel_whenTossApiException_markRefundFailed_publish_andReturnFail() {
			Payment p = mock(Payment.class);
			when(p.getStatus()).thenReturn(PaymentStatus.PAID);
			when(p.getPaymentKey()).thenReturn(paymentKey);
			when(p.getAmount()).thenReturn(amount);
			when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(p));

			TossApiException tossEx = mock(TossApiException.class);
			var errorCode = mock(com.groom.e_commerce.global.presentation.advice.ErrorCode.class);
			when(tossEx.getErrorCode()).thenReturn(errorCode);
			when(errorCode.getCode()).thenReturn("CANCEL_DENIED");
			when(errorCode.getMessage()).thenReturn("Denied");

			when(tossPaymentsClient.cancel(eq(paymentKey), any(TossCancelRequest.class))).thenThrow(tossEx);

			ReqCancelPayment req = new ReqCancelPayment(orderId, "reason");
			ResCancelResult res = paymentCommandService.cancel(req);

			assertThat(res).isNotNull();
			verify(p).markRefundFailed(eq("CANCEL_DENIED"), eq("Denied"));
			verify(paymentRepository).save(p);
			verify(paymentEventPublisher).publishRefundFailed(eq(orderId), eq(paymentKey), eq(amount), eq("CANCEL_DENIED"), eq("Denied"));
		}

		@Test
		@DisplayName("Toss cancel 성공(status=CANCELED) → PaymentCancel 저장 + Payment CANCELLED 전이 + 성공 이벤트")
		void cancel_whenCanceled_saveCancel_markCancelled_publishSucceeded() {
			Payment p = mock(Payment.class);
			when(p.getStatus()).thenReturn(PaymentStatus.PAID);
			when(p.getPaymentKey()).thenReturn(paymentKey);
			when(p.getAmount()).thenReturn(amount);
			when(p.getPaymentId()).thenReturn(UUID.randomUUID());
			when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(p));

			TossCancelResponse tossRes = mock(TossCancelResponse.class);
			when(tossRes.status()).thenReturn("CANCELED");
			when(tossPaymentsClient.cancel(eq(paymentKey), any(TossCancelRequest.class))).thenReturn(tossRes);

			ReqCancelPayment req = new ReqCancelPayment(orderId, "USER_CANCEL");
			ResCancelResult res = paymentCommandService.cancel(req);

			assertThat(res).isNotNull();

			verify(paymentCancelRepository).save(any(PaymentCancel.class));
			verify(p).markCancelled();
			verify(paymentRepository).save(p);
			verify(paymentEventPublisher).publishRefundSucceeded(eq(orderId), eq(paymentKey), eq(amount));
		}

		@Test
		@DisplayName("Toss cancel 2xx지만 status!=CANCELED → refundFailed 기록 + 실패 이벤트 + 실패 결과")
		void cancel_whenNotCanceled_markRefundFailed_publish_andReturnFail() {
			Payment p = mock(Payment.class);
			when(p.getStatus()).thenReturn(PaymentStatus.PAID);
			when(p.getPaymentKey()).thenReturn(paymentKey);
			when(p.getAmount()).thenReturn(amount);
			when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(p));

			TossCancelResponse tossRes = mock(TossCancelResponse.class);
			when(tossRes.status()).thenReturn("DONE"); // 말이 안 되는 상태 예시
			when(tossPaymentsClient.cancel(eq(paymentKey), any(TossCancelRequest.class))).thenReturn(tossRes);

			ReqCancelPayment req = new ReqCancelPayment(orderId, "x");
			ResCancelResult res = paymentCommandService.cancel(req);

			assertThat(res).isNotNull();
			verify(p).markRefundFailed(eq("TOSS_CANCEL_NOT_CANCELED"), contains("DONE"));
			verify(paymentEventPublisher).publishRefundFailed(eq(orderId), eq(paymentKey), eq(amount), eq("TOSS_CANCEL_NOT_CANCELED"), contains("DONE"));
		}
	}
}
