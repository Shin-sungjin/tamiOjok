package com.example.ecommerce.domain.user.service;

import com.example.ecommerce.domain.user.dto.request.AdditionalInfoRequest;
import com.example.ecommerce.domain.user.dto.request.LoginRequest;
import com.example.ecommerce.domain.user.dto.request.SignupRequest;
import com.example.ecommerce.domain.user.dto.response.TokenResponse;
import com.example.ecommerce.domain.user.entity.User;
import com.example.ecommerce.domain.user.entity.UserAddress;
import com.example.ecommerce.domain.user.enums.Provider;
import com.example.ecommerce.domain.user.enums.Role;
import com.example.ecommerce.domain.user.enums.UserStatus;
import com.example.ecommerce.domain.user.repository.UserAddressRepository;
import com.example.ecommerce.domain.user.repository.UserRepository;
import com.example.ecommerce.global.exception.CustomException;
import com.example.ecommerce.global.exception.ErrorCode;
import com.example.ecommerce.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final UserAddressRepository userAddressRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public Long signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .phoneNumber(request.phoneNumber())
                .provider(Provider.LOCAL)
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();

        return userRepository.save(user).getId();
    }

    @Transactional
    public IssuedTokens login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

        if (user.getPassword() == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        return issueTokens(user);
    }

    @Transactional
    public IssuedTokens reissue(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new CustomException(ErrorCode.EXPIRED_REFRESH_TOKEN);
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getRefreshToken() == null || !user.getRefreshToken().equals(refreshToken)) {
            throw new CustomException(ErrorCode.EXPIRED_REFRESH_TOKEN);
        }

        return issueTokens(user);
    }

    @Transactional
    public void logout(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        user.updateRefreshToken(null);
    }

    @Transactional
    public void completeAdditionalInfo(Long userId, AdditionalInfoRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.completeAdditionalInfo(request.phoneNumber());

        UserAddress address = UserAddress.builder()
                .user(user)
                .recipientName(request.recipientName())
                .recipientPhone(request.recipientPhone())
                .zipcode(request.zipcode())
                .addressMain(request.addressMain())
                .addressDetail(request.addressDetail())
                .isDefault(true)
                .build();

        userAddressRepository.save(address);
    }

    private IssuedTokens issueTokens(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());
        user.updateRefreshToken(refreshToken);

        boolean hasAddress = userAddressRepository.existsByUser(user);
        boolean needAdditionalInfo = user.needsAdditionalInfo() || user.getPhoneNumber() == null || !hasAddress;

        return new IssuedTokens(new TokenResponse(accessToken, needAdditionalInfo), refreshToken);
    }

    public record IssuedTokens(TokenResponse body, String refreshToken) {
    }
}
