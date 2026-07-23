package com.example.ecommerce.domain.delivery.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DeliveryCreateRequest(
        @NotBlank String courierCode,
        @NotBlank String trackingNumber
) {
}
