package com.example.ecommerce.domain.delivery.dto.request;

import com.example.ecommerce.domain.delivery.enums.ReturnReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DeliveryReturnRequest(
        @NotNull ReturnReason reason,
        @Size(max = 1000) String detail
) {
}
