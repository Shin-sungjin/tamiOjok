package com.example.ecommerce.domain.inquiry.dto.request;

import jakarta.validation.constraints.NotBlank;

public record InquiryCreateRequest(
        @NotBlank String category,
        @NotBlank String title,
        @NotBlank String content,
        Long orderId
) {
}
