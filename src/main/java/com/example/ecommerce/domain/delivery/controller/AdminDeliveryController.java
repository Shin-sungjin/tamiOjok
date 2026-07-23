package com.example.ecommerce.domain.delivery.controller;

import com.example.ecommerce.domain.delivery.dto.request.DeliveryCreateRequest;
import com.example.ecommerce.domain.delivery.dto.response.DeliveryResponse;
import com.example.ecommerce.domain.delivery.service.DeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/orders/{orderId}/delivery")
@RequiredArgsConstructor
public class AdminDeliveryController {

    private final DeliveryService deliveryService;

    @PostMapping
    public ResponseEntity<DeliveryResponse> createDelivery(@PathVariable Long orderId,
                                                             @Valid @RequestBody DeliveryCreateRequest request) {
        return ResponseEntity.ok(deliveryService.createDelivery(orderId, request));
    }
}
