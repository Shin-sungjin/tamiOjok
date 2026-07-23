package com.example.ecommerce.domain.payment.pg;

import java.math.BigDecimal;

public record PgPaymentResult(String pgTransactionId, BigDecimal paidAmount, boolean paid) {
}
