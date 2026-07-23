package com.example.ecommerce.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 만료되었습니다."),
    NEED_ADDITIONAL_INFO(HttpStatus.FORBIDDEN, "추가 정보 입력이 필요합니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "재고가 부족합니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),
    ORDER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "본인의 주문만 조회/취소할 수 있습니다."),
    EMPTY_ORDER_ITEMS(HttpStatus.BAD_REQUEST, "주문 항목이 비어 있습니다."),
    INVALID_ORDER_STATUS_TRANSITION(HttpStatus.CONFLICT, "현재 주문 상태에서는 처리할 수 없습니다."),
    ORDER_ALREADY_SHIPPED(HttpStatus.CONFLICT, "이미 배송이 시작되어 취소할 수 없습니다. 반품/교환을 이용해주세요."),
    PAYMENT_NOT_ALLOWED(HttpStatus.CONFLICT, "결제 대기 상태의 주문이 아닙니다."),
    PAYMENT_ALREADY_PROCESSED(HttpStatus.CONFLICT, "이미 처리된 결제입니다."),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "결제 내역을 찾을 수 없습니다."),
    DELIVERY_NOT_FOUND(HttpStatus.NOT_FOUND, "배송 정보를 찾을 수 없습니다."),
    DELIVERY_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 배송이 등록된 주문입니다."),
    INVALID_DELIVERY_STATUS_TRANSITION(HttpStatus.CONFLICT, "현재 배송 상태에서는 처리할 수 없습니다."),
    RETURN_NOT_ALLOWED(HttpStatus.CONFLICT, "배송 시작 전이거나 이미 반품 요청된 주문입니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
