package com.example.ecommerce.domain.delivery.controller;

import com.example.ecommerce.domain.delivery.dto.response.DeliveryResponse;
import com.example.ecommerce.domain.delivery.service.DeliveryService;
import com.example.ecommerce.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders/{orderId}/delivery")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @GetMapping
    public ResponseEntity<DeliveryResponse> getDelivery(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                          @PathVariable Long orderId) {
        return ResponseEntity.ok(deliveryService.getDelivery(userDetails.getUserId(), orderId));
    }

    @PostMapping("/return-request")
    public ResponseEntity<Void> requestReturn(@AuthenticationPrincipal CustomUserDetails userDetails,
                                               @PathVariable Long orderId) {
        deliveryService.requestReturn(userDetails.getUserId(), orderId);
        return ResponseEntity.noContent().build();
    }
}
