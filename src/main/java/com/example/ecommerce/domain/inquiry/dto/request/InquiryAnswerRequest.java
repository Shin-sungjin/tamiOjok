package com.example.ecommerce.domain.inquiry.dto.request;

import jakarta.validation.constraints.NotBlank;

public record InquiryAnswerRequest(
        @NotBlank String answer
) {
}
