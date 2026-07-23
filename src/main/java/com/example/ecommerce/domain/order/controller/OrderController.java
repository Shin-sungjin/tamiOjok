package com.example.ecommerce.domain.order.controller;

import com.example.ecommerce.domain.order.dto.request.OrderCreateRequest;
import com.example.ecommerce.domain.order.dto.response.OrderResponse;
import com.example.ecommerce.domain.order.service.OrderService;
import com.example.ecommerce.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                       @Valid @RequestBody OrderCreateRequest request) {
        OrderResponse response = orderService.createOrder(userDetails.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getMyOrders(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                             Pageable pageable) {
        return ResponseEntity.ok(orderService.getMyOrders(userDetails.getUserId(), pageable));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getMyOrder(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                      @PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getMyOrder(userDetails.getUserId(), orderId));
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> cancelOrder(@AuthenticationPrincipal CustomUserDetails userDetails,
                                             @PathVariable Long orderId) {
        orderService.cancelOrder(userDetails.getUserId(), orderId);
        return ResponseEntity.noContent().build();
    }
}
