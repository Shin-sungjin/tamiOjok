package com.example.ecommerce.global.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * nginx가 프론트/백엔드를 같은 origin으로 프록시하는 배포 구조에서는, 요청의 실제 Host
 * (X-Forwarded-Host 기반 — server.forward-headers-strategy=framework로 이미 신뢰 중)와
 * Origin 헤더가 일치하면 same-origin 요청으로 간주해 허용한다. Cloudflare Quick Tunnel처럼
 * 접속 URL이 재기동마다 바뀌어도 화이트리스트를 매번 갱신할 필요가 없다.
 * Host와 다른 Origin(진짜 cross-site 요청, 예: 악성 사이트)은 정적 화이트리스트
 * (app.cors.allowed-origins — vite dev 서버 등)에 있을 때만 허용한다.
 */
public class OriginAwareCorsConfigurationSource implements CorsConfigurationSource {

    private final List<String> staticAllowedOrigins;

    public OriginAwareCorsConfigurationSource(List<String> staticAllowedOrigins) {
        this.staticAllowedOrigins = staticAllowedOrigins;
    }

    @Override
    public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
        // Spring의 DefaultCorsProcessor는 이 메서드가 null을 반환하면 preflight가 아닌
        // 실제 요청(GET/POST 등)에 대해서는 CORS 검증 자체를 건너뛰고 통과시켜 버린다
        // (client-side 방어로만 취급). 그래서 허용하지 않을 origin이라도 매번 설정
        // 객체 자체는 반환하되 allowedOrigins만 비워서, checkOrigin()이 명시적으로
        // 거부(403)하도록 한다 — 그래야 실제 요청도 서버 단에서 확실히 막힌다.
        String origin = request.getHeader(HttpHeaders.ORIGIN);

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        if (origin != null && isAllowed(origin, request)) {
            configuration.setAllowedOrigins(List.of(origin));
        }

        return configuration;
    }

    boolean isAllowed(String origin, HttpServletRequest request) {
        return origin.equals(requestOrigin(request)) || staticAllowedOrigins.contains(origin);
    }

    static String requestOrigin(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        boolean isDefaultPort = ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
        return isDefaultPort ? scheme + "://" + host : scheme + "://" + host + ":" + port;
    }
}
