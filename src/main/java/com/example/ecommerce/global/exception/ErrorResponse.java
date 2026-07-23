package com.example.ecommerce.global.exception;

import java.util.List;

public record ErrorResponse(String code, String message, List<String> errors) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, List<String> errors) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), errors);
    }
}
