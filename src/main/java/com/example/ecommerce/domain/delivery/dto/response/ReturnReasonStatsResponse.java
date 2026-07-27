package com.example.ecommerce.domain.delivery.dto.response;

import com.example.ecommerce.domain.delivery.enums.ReturnReason;

public record ReturnReasonStatsResponse(ReturnReason reason, long count) {
}
