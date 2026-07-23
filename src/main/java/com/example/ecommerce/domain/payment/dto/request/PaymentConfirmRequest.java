package com.example.ecommerce.domain.payment.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PaymentConfirmRequest(
        @NotNull Long orderId,
        @NotBlank String pgProvider,
        @NotBlank String pgTransactionId,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal paidAmount
) {
}
