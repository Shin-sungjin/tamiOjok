package com.example.ecommerce.domain.user.controller;

import com.example.ecommerce.domain.user.dto.request.AdditionalInfoRequest;
import com.example.ecommerce.domain.user.dto.response.UserResponse;
import com.example.ecommerce.domain.user.service.AuthService;
import com.example.ecommerce.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(UserResponse.from(userDetails.getUser()));
    }

    @PostMapping("/me/additional-info")
    public ResponseEntity<Void> completeAdditionalInfo(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                         @Valid @RequestBody AdditionalInfoRequest request) {
        authService.completeAdditionalInfo(userDetails.getUserId(), request);
        return ResponseEntity.noContent().build();
    }
}
