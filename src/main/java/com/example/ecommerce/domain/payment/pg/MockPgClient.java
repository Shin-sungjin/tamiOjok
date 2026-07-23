package com.example.ecommerce.domain.payment.pg;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

// TODO: 실제 Toss/PortOne 서버-투-서버 검증 API 연동 전까지의 개발용 placeholder.
@Component
public class MockPgClient implements PgClient {

    @Override
    public PgPaymentResult fetchPayment(String pgTransactionId, BigDecimal clientReportedAmount) {
        return new PgPaymentResult(pgTransactionId, clientReportedAmount, true);
    }

    @Override
    public void cancelPayment(String pgTransactionId, String reason) {
    }
}
