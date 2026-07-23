package com.example.ecommerce.domain.inquiry.controller;

import com.example.ecommerce.domain.inquiry.dto.request.InquiryAnswerRequest;
import com.example.ecommerce.domain.inquiry.dto.response.InquiryResponse;
import com.example.ecommerce.domain.inquiry.enums.InquiryStatus;
import com.example.ecommerce.domain.inquiry.service.InquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/inquiries")
@RequiredArgsConstructor
public class AdminInquiryController {

    private final InquiryService inquiryService;

    @GetMapping
    public ResponseEntity<Page<InquiryResponse>> getInquiries(
            @RequestParam(required = false) InquiryStatus status, Pageable pageable) {
        return ResponseEntity.ok(inquiryService.getInquiries(status, pageable));
    }

    @PostMapping("/{inquiryId}/answer")
    public ResponseEntity<InquiryResponse> answerInquiry(@PathVariable Long inquiryId,
                                                           @Valid @RequestBody InquiryAnswerRequest request) {
        return ResponseEntity.ok(inquiryService.answerInquiry(inquiryId, request));
    }
}
