package com.example.ecommerce.domain.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record OrderCreateRequest(
        @NotEmpty @Valid List<OrderItemRequest> items,
        Long userCouponId
) {
    public record OrderItemRequest(
            @NotNull Long productId,
            @Positive int quantity
    ) {
    }
}
