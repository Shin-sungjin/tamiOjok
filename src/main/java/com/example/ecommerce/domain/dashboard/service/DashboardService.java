package com.example.ecommerce.domain.dashboard.service;

import com.example.ecommerce.domain.analytics.service.SiteVisitService;
import com.example.ecommerce.domain.dashboard.dto.response.DashboardSummaryResponse;
import com.example.ecommerce.domain.delivery.enums.DeliveryStatus;
import com.example.ecommerce.domain.delivery.repository.DeliveryRepository;
import com.example.ecommerce.domain.inquiry.enums.InquiryStatus;
import com.example.ecommerce.domain.inquiry.repository.InquiryRepository;
import com.example.ecommerce.domain.order.enums.OrderStatus;
import com.example.ecommerce.domain.order.repository.OrderRepository;
import com.example.ecommerce.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final OrderRepository orderRepository;
    private final DeliveryRepository deliveryRepository;
    private final InquiryRepository inquiryRepository;
    private final PaymentRepository paymentRepository;
    private final SiteVisitService siteVisitService;

    public DashboardSummaryResponse getSummary() {
        return new DashboardSummaryResponse(
                paymentRepository.sumPaidAmount(),
                orderRepository.countByStatus(OrderStatus.PAYMENT_COMPLETED)
                        + orderRepository.countByStatus(OrderStatus.PREPARING),
                orderRepository.countByStatus(OrderStatus.PENDING_PAYMENT),
                orderRepository.countByStatus(OrderStatus.PREPARING),
                orderRepository.countByStatus(OrderStatus.CANCELLED),
                orderRepository.countPendingShipment(),
                deliveryRepository.countByStatus(DeliveryStatus.IN_TRANSIT),
                deliveryRepository.countByStatus(DeliveryStatus.DELIVERED),
                deliveryRepository.countByStatus(DeliveryStatus.RETURN_REQUESTED),
                inquiryRepository.countByStatus(InquiryStatus.WAITING),
                siteVisitService.countToday(),
                siteVisitService.countTotal()
        );
    }
}
