package com.example.ecommerce.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

class OriginAwareCorsConfigurationSourceTest {

    private final OriginAwareCorsConfigurationSource source =
            new OriginAwareCorsConfigurationSource(List.of("http://localhost:5173"));

    @Test
    void allowsRequestWhenOriginMatchesForwardedHost() {
        // nginx가 forward-headers-strategy=framework를 통해 실제 접속 도메인을
        // request.getServerName()/getScheme()에 반영해준 상황을 흉내낸다.
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Origin", "https://random-tunnel-url.trycloudflare.com");
        request.setScheme("https");
        request.setServerName("random-tunnel-url.trycloudflare.com");
        request.setServerPort(443);

        CorsConfiguration configuration = source.getCorsConfiguration(request);

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins()).containsExactly("https://random-tunnel-url.trycloudflare.com");
    }

    @Test
    void allowsRequestWhenOriginIsInStaticWhitelist() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Origin", "http://localhost:5173");
        request.setScheme("http");
        request.setServerName("backend");
        request.setServerPort(8080);

        CorsConfiguration configuration = source.getCorsConfiguration(request);

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins()).containsExactly("http://localhost:5173");
    }

    @Test
    void rejectsCrossSiteOriginThatMatchesNeitherHostNorWhitelist() {
        // getCorsConfiguration()이 null을 반환하면 Spring이 preflight가 아닌 실제
        // 요청은 그냥 통과시켜 버리므로(클라이언트 측 방어로만 취급), 여기서는 항상
        // non-null 설정을 반환하되 allowedOrigins를 비워서 checkOrigin()이 명시적으로
        // 거부(403)하게 만드는 게 계약이다 — 그걸 검증한다.
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Origin", "https://evil.example.com");
        request.setScheme("https");
        request.setServerName("random-tunnel-url.trycloudflare.com");
        request.setServerPort(443);

        CorsConfiguration configuration = source.getCorsConfiguration(request);

        assertThat(configuration).isNotNull();
        assertThat(configuration.checkOrigin("https://evil.example.com")).isNull();
    }

    @Test
    void doesNotAllowAnyOriginWhenNoOriginHeaderPresent() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        CorsConfiguration configuration = source.getCorsConfiguration(request);

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins()).isNull();
    }
}
