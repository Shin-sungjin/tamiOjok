package com.example.ecommerce.global.security.oauth;

import com.example.ecommerce.domain.user.entity.User;
import com.example.ecommerce.domain.user.enums.Role;
import com.example.ecommerce.domain.user.enums.UserStatus;
import com.example.ecommerce.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        OAuth2UserInfo userInfo = OAuth2UserInfo.of(registrationId, oAuth2User.getAttributes());
        User user = findOrCreateUser(userInfo);

        return new CustomOAuth2User(user, oAuth2User.getAttributes());
    }

    private User findOrCreateUser(OAuth2UserInfo userInfo) {
        return userRepository.findByProviderAndProviderId(userInfo.getProvider(), userInfo.getProviderId())
                .orElseGet(() -> userRepository.save(User.builder()
                        .email(userInfo.getEmail())
                        .name(userInfo.getName())
                        .provider(userInfo.getProvider())
                        .providerId(userInfo.getProviderId())
                        .role(Role.USER)
                        .status(UserStatus.NEED_INFO)
                        .build()));
    }
}
