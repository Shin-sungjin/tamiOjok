package com.example.ecommerce.domain.payment.dto.response;

import com.example.ecommerce.domain.payment.entity.Payment;
import com.example.ecommerce.domain.payment.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long orderId,
        String pgProvider,
        PaymentStatus status,
        BigDecimal requestedAmount,
        BigDecimal paidAmount,
        LocalDateTime paidAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(), payment.getOrder().getId(), payment.getPgProvider(), payment.getStatus(),
                payment.getRequestedAmount(), payment.getPaidAmount(), payment.getPaidAt());
    }
}
