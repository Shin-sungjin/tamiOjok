package com.example.ecommerce.domain.payment.pg;

import java.math.BigDecimal;

public interface PgClient {

    PgPaymentResult fetchPayment(String pgTransactionId, BigDecimal clientReportedAmount);

    void cancelPayment(String pgTransactionId, String reason);
}
