package com.example.ecommerce.domain.order.controller;

import com.example.ecommerce.domain.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @PostMapping("/{orderId}/prepare")
    public ResponseEntity<Void> startPreparing(@PathVariable Long orderId) {
        orderService.startPreparing(orderId);
        return ResponseEntity.noContent().build();
    }
}
