package com.example.ecommerce.domain.review.controller;

import com.example.ecommerce.domain.review.dto.request.ReviewCreateRequest;
import com.example.ecommerce.domain.review.dto.request.ReviewUpdateRequest;
import com.example.ecommerce.domain.review.dto.response.ReviewResponse;
import com.example.ecommerce.domain.review.service.ReviewService;
import com.example.ecommerce.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                         @Valid @RequestBody ReviewCreateRequest request) {
        ReviewResponse response = reviewService.createReview(userDetails.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<ReviewResponse>> getMyReviews(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                               Pageable pageable) {
        return ResponseEntity.ok(reviewService.getMyReviews(userDetails.getUserId(), pageable));
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> updateReview(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                         @PathVariable Long reviewId,
                                                         @Valid @RequestBody ReviewUpdateRequest request) {
        return ResponseEntity.ok(reviewService.updateReview(userDetails.getUserId(), reviewId, request));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(@AuthenticationPrincipal CustomUserDetails userDetails,
                                              @PathVariable Long reviewId) {
        reviewService.deleteReview(userDetails.getUserId(), reviewId);
        return ResponseEntity.noContent().build();
    }
}
