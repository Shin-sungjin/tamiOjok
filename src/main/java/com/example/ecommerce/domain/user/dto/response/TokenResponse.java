package com.example.ecommerce.domain.user.dto.response;

public record TokenResponse(String accessToken, boolean needAdditionalInfo) {
}
