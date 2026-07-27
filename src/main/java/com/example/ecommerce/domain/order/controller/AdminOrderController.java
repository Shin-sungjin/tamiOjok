package com.example.ecommerce.domain.order.controller;

import com.example.ecommerce.domain.delivery.dto.response.ReturnReasonStatsResponse;
import com.example.ecommerce.domain.delivery.enums.ReturnReason;
import com.example.ecommerce.domain.order.dto.response.AdminOrderResponse;
import com.example.ecommerce.domain.order.enums.OrderStatus;
import com.example.ecommerce.domain.order.service.OrderService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<Page<AdminOrderResponse>> getOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) ReturnReason returnReason,
            Pageable pageable) {
        return ResponseEntity.ok(orderService.getOrdersForAdmin(status, returnReason, pageable));
    }

    @GetMapping("/return-reason-stats")
    public ResponseEntity<List<ReturnReasonStatsResponse>> getReturnReasonStats() {
        return ResponseEntity.ok(orderService.getReturnReasonStats());
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<AdminOrderResponse> getOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderForAdmin(orderId));
    }

    @PostMapping("/{orderId}/prepare")
    public ResponseEntity<Void> startPreparing(@PathVariable Long orderId) {
        orderService.startPreparing(orderId);
        return ResponseEntity.noContent().build();
    }
}
