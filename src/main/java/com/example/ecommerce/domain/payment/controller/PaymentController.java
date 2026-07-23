package com.example.ecommerce.domain.payment.controller;

import com.example.ecommerce.domain.payment.dto.request.PaymentConfirmRequest;
import com.example.ecommerce.domain.payment.dto.response.PaymentResponse;
import com.example.ecommerce.domain.payment.enums.PaymentStatus;
import com.example.ecommerce.domain.payment.service.PaymentService;
import com.example.ecommerce.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/confirm")
    public ResponseEntity<PaymentResponse> confirmPayment(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                            @Valid @RequestBody PaymentConfirmRequest request) {
        PaymentResponse response = paymentService.confirmPayment(userDetails.getUserId(), request);
        HttpStatus status = response.status() == PaymentStatus.FAILED ? HttpStatus.CONFLICT : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<PaymentResponse> getPayment(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                        @PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.getPayment(userDetails.getUserId(), orderId));
    }
}
