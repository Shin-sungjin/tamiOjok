package com.example.ecommerce.domain.delivery.service;

import com.example.ecommerce.domain.delivery.dto.request.DeliveryCreateRequest;
import com.example.ecommerce.domain.delivery.dto.response.DeliveryResponse;
import com.example.ecommerce.domain.delivery.entity.Delivery;
import com.example.ecommerce.domain.delivery.repository.DeliveryRepository;
import com.example.ecommerce.domain.order.entity.Order;
import com.example.ecommerce.domain.order.enums.OrderStatus;
import com.example.ecommerce.domain.order.service.OrderService;
import com.example.ecommerce.global.exception.CustomException;
import com.example.ecommerce.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final OrderService orderService;

    @Transactional
    public DeliveryResponse createDelivery(Long orderId, DeliveryCreateRequest request) {
        Order order = orderService.getOrderEntityOrThrow(orderId);
        if (order.getStatus() != OrderStatus.PREPARING) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS_TRANSITION);
        }
        if (deliveryRepository.findByOrder(order).isPresent()) {
            throw new CustomException(ErrorCode.DELIVERY_ALREADY_EXISTS);
        }

        Delivery delivery = Delivery.builder()
                .order(order)
                .courierCode(request.courierCode())
                .trackingNumber(request.trackingNumber())
                .build();

        return DeliveryResponse.from(deliveryRepository.save(delivery));
    }

    public DeliveryResponse getDelivery(Long userId, Long orderId) {
        Order order = orderService.getOrderEntityOrThrow(orderId);
        validateOwnership(order, userId);
        return DeliveryResponse.from(getDeliveryOrThrow(order));
    }

    @Transactional
    public void requestReturn(Long userId, Long orderId) {
        Order order = orderService.getOrderEntityOrThrow(orderId);
        validateOwnership(order, userId);
        getDeliveryOrThrow(order).requestReturn();
    }

    private Delivery getDeliveryOrThrow(Order order) {
        return deliveryRepository.findByOrder(order)
                .orElseThrow(() -> new CustomException(ErrorCode.DELIVERY_NOT_FOUND));
    }

    private void validateOwnership(Order order, Long userId) {
        if (!order.isOwnedBy(userId)) {
            throw new CustomException(ErrorCode.ORDER_ACCESS_DENIED);
        }
    }
}
