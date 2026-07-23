package com.example.ecommerce.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AdditionalInfoRequest(
        @NotBlank String phoneNumber,
        @NotBlank String recipientName,
        @NotBlank String recipientPhone,
        @NotBlank String zipcode,
        @NotBlank String addressMain,
        String addressDetail
) {
}
