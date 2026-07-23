package com.example.ecommerce.domain.cart.controller;

import com.example.ecommerce.domain.cart.dto.request.CartItemAddRequest;
import com.example.ecommerce.domain.cart.dto.request.CartItemQuantityUpdateRequest;
import com.example.ecommerce.domain.cart.dto.response.CartResponse;
import com.example.ecommerce.domain.cart.service.CartService;
import com.example.ecommerce.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponse> getMyCart(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(cartService.getMyCart(userDetails.getUserId()));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                 @Valid @RequestBody CartItemAddRequest request) {
        CartResponse response = cartService.addItem(userDetails.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponse> updateItemQuantity(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                             @PathVariable Long cartItemId,
                                                             @Valid @RequestBody CartItemQuantityUpdateRequest request) {
        CartResponse response = cartService.updateItemQuantity(userDetails.getUserId(), cartItemId, request.quantity());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<Void> removeItem(@AuthenticationPrincipal CustomUserDetails userDetails,
                                            @PathVariable Long cartItemId) {
        cartService.removeItem(userDetails.getUserId(), cartItemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal CustomUserDetails userDetails) {
        cartService.clearCart(userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }
}
