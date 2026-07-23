package com.example.ecommerce.domain.inquiry.controller;

import com.example.ecommerce.domain.inquiry.dto.request.InquiryCreateRequest;
import com.example.ecommerce.domain.inquiry.dto.response.InquiryResponse;
import com.example.ecommerce.domain.inquiry.service.InquiryService;
import com.example.ecommerce.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    @PostMapping
    public ResponseEntity<InquiryResponse> createInquiry(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                           @Valid @RequestBody InquiryCreateRequest request) {
        InquiryResponse response = inquiryService.createInquiry(userDetails.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<InquiryResponse>> getMyInquiries(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                                  Pageable pageable) {
        return ResponseEntity.ok(inquiryService.getMyInquiries(userDetails.getUserId(), pageable));
    }

    @GetMapping("/{inquiryId}")
    public ResponseEntity<InquiryResponse> getMyInquiry(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                          @PathVariable Long inquiryId) {
        return ResponseEntity.ok(inquiryService.getMyInquiry(userDetails.getUserId(), inquiryId));
    }
}
