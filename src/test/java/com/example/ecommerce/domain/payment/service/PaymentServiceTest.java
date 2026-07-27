package com.example.ecommerce.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ecommerce.domain.order.entity.Order;
import com.example.ecommerce.domain.order.enums.OrderStatus;
import com.example.ecommerce.domain.order.service.OrderService;
import com.example.ecommerce.domain.payment.dto.request.PaymentConfirmRequest;
import com.example.ecommerce.domain.payment.dto.response.PaymentResponse;
import com.example.ecommerce.domain.payment.entity.Payment;
import com.example.ecommerce.domain.payment.enums.PaymentStatus;
import com.example.ecommerce.domain.payment.pg.PgClient;
import com.example.ecommerce.domain.payment.pg.PgPaymentResult;
import com.example.ecommerce.domain.payment.repository.PaymentRepository;
import com.example.ecommerce.domain.user.entity.User;
import com.example.ecommerce.domain.user.enums.Provider;
import com.example.ecommerce.domain.user.enums.Role;
import com.example.ecommerce.domain.user.enums.UserStatus;
import com.example.ecommerce.global.exception.CustomException;
import com.example.ecommerce.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

// PRD Sequence 3(결제 금액 위변조 검증)의 핵심: 클라이언트가 보고하는 결제금액이
// 아니라 PG 서버 조회 결과(pgClient.fetchPayment)와 주문의 실제 결제 예정 금액을
// 대조해서, 다르면 결제를 실패 처리하고 PG 취소 + 주문 취소까지 이어지는지 검증한다.
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private OrderService orderService;
    @Mock
    private PgClient pgClient;

    @InjectMocks
    private PaymentService paymentService;

    private Order pendingPaymentOrder(Long userId, Long orderId, long paymentAmount) {
        User user = User.builder()
                .email("buyer@test.com")
                .name("구매자")
                .provider(Provider.LOCAL)
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);

        Order order = Order.builder()
                .orderNumber("ORD-TEST-" + orderId)
                .user(user)
                .totalAmount(BigDecimal.valueOf(paymentAmount))
                .discountAmount(BigDecimal.ZERO)
                .orderItems(new ArrayList<>())
                .build();
        ReflectionTestUtils.setField(order, "id", orderId);
        return order; // Order.builder()는 항상 PENDING_PAYMENT로 시작함
    }

    @Test
    void confirmPayment_amountMatchesPgResult_marksPaidAndCompletesOrder() {
        Order order = pendingPaymentOrder(1L, 1L, 89000);
        when(orderService.getOrderEntityOrThrow(1L)).thenReturn(order);
        when(paymentRepository.findByOrder(order)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pgClient.fetchPayment("TXN-1", BigDecimal.valueOf(89000)))
                .thenReturn(new PgPaymentResult("TXN-1", BigDecimal.valueOf(89000), true));

        PaymentResponse response = paymentService.confirmPayment(
                1L, new PaymentConfirmRequest(1L, "MOCK", "TXN-1", BigDecimal.valueOf(89000)));

        assertThat(response.status()).isEqualTo(PaymentStatus.PAID);
        assertThat(response.paidAmount()).isEqualByComparingTo("89000");
        verify(orderService).completePayment(1L);
        verify(orderService, never()).cancelBySystem(any());
        verify(pgClient, never()).cancelPayment(any(), any());
    }

    @Test
    void confirmPayment_pgReportedAmountLowerThanOrderAmount_treatedAsTamperingAndCancelled() {
        // 위변조 시나리오: 클라이언트가 요청한 paidAmount(89000)와 무관하게, PG
        // 서버에 직접 조회한 실제 결제 금액이 주문 금액과 다르면(여기선 100원)
        // 반드시 결제 실패 + PG 취소 + 주문 취소로 이어져야 한다.
        Order order = pendingPaymentOrder(1L, 1L, 89000);
        when(orderService.getOrderEntityOrThrow(1L)).thenReturn(order);
        when(paymentRepository.findByOrder(order)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pgClient.fetchPayment("TXN-2", BigDecimal.valueOf(89000)))
                .thenReturn(new PgPaymentResult("TXN-2", BigDecimal.valueOf(100), true));

        PaymentResponse response = paymentService.confirmPayment(
                1L, new PaymentConfirmRequest(1L, "MOCK", "TXN-2", BigDecimal.valueOf(89000)));

        assertThat(response.status()).isEqualTo(PaymentStatus.FAILED);
        verify(pgClient).cancelPayment("TXN-2", "PAYMENT_AMOUNT_MISMATCH");
        verify(orderService).cancelBySystem(1L);
        verify(orderService, never()).completePayment(any());
    }

    @Test
    void confirmPayment_pgReportsNotPaid_treatedAsFailureRegardlessOfAmount() {
        Order order = pendingPaymentOrder(1L, 1L, 89000);
        when(orderService.getOrderEntityOrThrow(1L)).thenReturn(order);
        when(paymentRepository.findByOrder(order)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pgClient.fetchPayment("TXN-3", BigDecimal.valueOf(89000)))
                .thenReturn(new PgPaymentResult("TXN-3", BigDecimal.valueOf(89000), false));

        PaymentResponse response = paymentService.confirmPayment(
                1L, new PaymentConfirmRequest(1L, "MOCK", "TXN-3", BigDecimal.valueOf(89000)));

        assertThat(response.status()).isEqualTo(PaymentStatus.FAILED);
        verify(pgClient).cancelPayment(eq("TXN-3"), any());
        verify(orderService).cancelBySystem(1L);
    }

    @Test
    void confirmPayment_orderNotInPendingPaymentStatus_throwsPaymentNotAllowed() {
        Order order = pendingPaymentOrder(1L, 1L, 89000);
        order.completePayment(); // PENDING_PAYMENT -> PAYMENT_COMPLETED, 재결제 시도 상황을 흉내
        when(orderService.getOrderEntityOrThrow(1L)).thenReturn(order);

        assertThatThrownBy(() -> paymentService.confirmPayment(
                1L, new PaymentConfirmRequest(1L, "MOCK", "TXN-4", BigDecimal.valueOf(89000))))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PAYMENT_NOT_ALLOWED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_COMPLETED);
    }

    @Test
    void confirmPayment_alreadyHasPayment_throwsPaymentAlreadyProcessed() {
        // 결제 완료 콜백이 중복 호출되는 경우(네트워크 재시도 등) 이중 결제를 막는다.
        Order order = pendingPaymentOrder(1L, 1L, 89000);
        when(orderService.getOrderEntityOrThrow(1L)).thenReturn(order);
        when(paymentRepository.findByOrder(order)).thenReturn(Optional.of(mock(Payment.class)));

        assertThatThrownBy(() -> paymentService.confirmPayment(
                1L, new PaymentConfirmRequest(1L, "MOCK", "TXN-5", BigDecimal.valueOf(89000))))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PAYMENT_ALREADY_PROCESSED);
    }

    @Test
    void confirmPayment_calledByNonOwner_throwsAccessDenied() {
        Order order = pendingPaymentOrder(1L, 1L, 89000); // user 1 소유
        when(orderService.getOrderEntityOrThrow(1L)).thenReturn(order);

        assertThatThrownBy(() -> paymentService.confirmPayment(
                2L, new PaymentConfirmRequest(1L, "MOCK", "TXN-6", BigDecimal.valueOf(89000))))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_ACCESS_DENIED);
    }
}
