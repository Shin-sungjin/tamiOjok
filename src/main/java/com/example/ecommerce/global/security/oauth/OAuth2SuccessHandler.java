package com.example.ecommerce.global.security.oauth;

import com.example.ecommerce.domain.user.entity.User;
import com.example.ecommerce.domain.user.repository.UserAddressRepository;
import com.example.ecommerce.global.security.jwt.JwtTokenProvider;
import com.example.ecommerce.global.security.jwt.RefreshTokenCookieProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenCookieProvider refreshTokenCookieProvider;
    private final UserAddressRepository userAddressRepository;

    @Value("${app.oauth2.redirect-uri:http://localhost:3000/oauth2/redirect}")
    private String redirectUri;

    @Value("${app.oauth2.need-info-uri:http://localhost:3000/oauth2/additional-info}")
    private String needInfoUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException, ServletException {
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        User user = oAuth2User.getUser();

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());
        user.updateRefreshToken(refreshToken);

        response.addCookie(refreshTokenCookieProvider.create(refreshToken));

        boolean hasAddress = userAddressRepository.existsByUser(user);
        boolean needsInfo = user.needsAdditionalInfo() || user.getPhoneNumber() == null || !hasAddress;

        String targetUrl = UriComponentsBuilder.fromUriString(needsInfo ? needInfoUri : redirectUri)
                .queryParam("accessToken", accessToken)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
