package com.example.ecommerce.domain.dashboard.dto.response;

import java.math.BigDecimal;

public record DashboardSummaryResponse(
        BigDecimal totalRevenue,
        long paidOrderCount,
        long pendingPaymentOrderCount,
        long preparingOrderCount,
        long cancelledOrderCount,
        long pendingShipmentCount,
        long inTransitCount,
        long deliveredCount,
        long returnRequestedCount,
        long waitingInquiryCount,
        long todayVisitCount,
        long totalVisitCount
) {
}
