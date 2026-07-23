package com.example.ecommerce.global.security.oauth;

import com.example.ecommerce.domain.user.enums.Provider;
import java.util.Map;

public interface OAuth2UserInfo {

    Provider getProvider();

    String getProviderId();

    String getEmail();

    String getName();

    static OAuth2UserInfo of(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "kakao" -> new KakaoUserInfo(attributes);
            case "naver" -> new NaverUserInfo(attributes);
            case "google" -> new GoogleUserInfo(attributes);
            default -> throw new IllegalArgumentException("지원하지 않는 OAuth Provider 입니다: " + registrationId);
        };
    }
}
