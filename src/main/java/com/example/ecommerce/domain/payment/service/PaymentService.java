package com.example.ecommerce.domain.payment.service;

import com.example.ecommerce.domain.order.entity.Order;
import com.example.ecommerce.domain.order.enums.OrderStatus;
import com.example.ecommerce.domain.order.service.OrderService;
import com.example.ecommerce.domain.payment.dto.request.PaymentConfirmRequest;
import com.example.ecommerce.domain.payment.dto.response.PaymentResponse;
import com.example.ecommerce.domain.payment.entity.Payment;
import com.example.ecommerce.domain.payment.pg.PgClient;
import com.example.ecommerce.domain.payment.pg.PgPaymentResult;
import com.example.ecommerce.domain.payment.repository.PaymentRepository;
import com.example.ecommerce.global.exception.CustomException;
import com.example.ecommerce.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final PgClient pgClient;

    @Transactional
    public PaymentResponse confirmPayment(Long userId, PaymentConfirmRequest request) {
        Order order = orderService.getOrderEntityOrThrow(request.orderId());
        validateOwnership(order, userId);

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_ALLOWED);
        }
        if (paymentRepository.findByOrder(order).isPresent()) {
            throw new CustomException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
        }

        Payment payment = paymentRepository.save(Payment.builder()
                .order(order)
                .pgProvider(request.pgProvider())
                .pgTransactionId(request.pgTransactionId())
                .requestedAmount(order.getPaymentAmount())
                .build());

        PgPaymentResult pgResult = pgClient.fetchPayment(request.pgTransactionId(), request.paidAmount());

        if (!pgResult.paid() || pgResult.paidAmount().compareTo(order.getPaymentAmount()) != 0) {
            pgClient.cancelPayment(request.pgTransactionId(), "PAYMENT_AMOUNT_MISMATCH");
            payment.markFailed();
            orderService.cancelBySystem(order.getId());
            return PaymentResponse.from(payment);
        }

        payment.markPaid(pgResult.paidAmount());
        orderService.completePayment(order.getId());

        return PaymentResponse.from(payment);
    }

    public PaymentResponse getPayment(Long userId, Long orderId) {
        Order order = orderService.getOrderEntityOrThrow(orderId);
        validateOwnership(order, userId);

        Payment payment = paymentRepository.findByOrder(order)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));
        return PaymentResponse.from(payment);
    }

    private void validateOwnership(Order order, Long userId) {
        if (!order.isOwnedBy(userId)) {
            throw new CustomException(ErrorCode.ORDER_ACCESS_DENIED);
        }
    }
}
