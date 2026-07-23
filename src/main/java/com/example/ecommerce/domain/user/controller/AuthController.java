package com.example.ecommerce.domain.user.controller;

import com.example.ecommerce.domain.user.dto.request.LoginRequest;
import com.example.ecommerce.domain.user.dto.request.SignupRequest;
import com.example.ecommerce.domain.user.dto.response.TokenResponse;
import com.example.ecommerce.domain.user.service.AuthService;
import com.example.ecommerce.domain.user.service.AuthService.IssuedTokens;
import com.example.ecommerce.global.exception.CustomException;
import com.example.ecommerce.global.exception.ErrorCode;
import com.example.ecommerce.global.security.CustomUserDetails;
import com.example.ecommerce.global.security.jwt.RefreshTokenCookieProvider;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieProvider refreshTokenCookieProvider;

    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request,
                                                HttpServletResponse response) {
        IssuedTokens tokens = authService.login(request);
        response.addCookie(refreshTokenCookieProvider.create(tokens.refreshToken()));
        return ResponseEntity.ok(tokens.body());
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = RefreshTokenCookieProvider.COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        if (refreshToken == null) {
            throw new CustomException(ErrorCode.EXPIRED_REFRESH_TOKEN);
        }

        IssuedTokens tokens = authService.reissue(refreshToken);
        response.addCookie(refreshTokenCookieProvider.create(tokens.refreshToken()));
        return ResponseEntity.ok(tokens.body());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal CustomUserDetails userDetails,
                                        HttpServletResponse response) {
        authService.logout(userDetails.getUserId());
        response.addCookie(refreshTokenCookieProvider.expire());
        return ResponseEntity.noContent().build();
    }
}
