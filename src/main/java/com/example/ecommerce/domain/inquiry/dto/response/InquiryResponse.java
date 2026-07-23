package com.example.ecommerce.domain.inquiry.dto.response;

import com.example.ecommerce.domain.inquiry.entity.Inquiry;
import com.example.ecommerce.domain.inquiry.enums.InquiryStatus;
import java.time.LocalDateTime;

public record InquiryResponse(
        Long id,
        Long orderId,
        String category,
        String title,
        String content,
        String answer,
        InquiryStatus status,
        LocalDateTime createdAt,
        LocalDateTime answeredAt
) {
    public static InquiryResponse from(Inquiry inquiry) {
        return new InquiryResponse(
                inquiry.getId(),
                inquiry.getOrder() != null ? inquiry.getOrder().getId() : null,
                inquiry.getCategory(), inquiry.getTitle(), inquiry.getContent(), inquiry.getAnswer(),
                inquiry.getStatus(), inquiry.getCreatedAt(), inquiry.getAnsweredAt());
    }
}
