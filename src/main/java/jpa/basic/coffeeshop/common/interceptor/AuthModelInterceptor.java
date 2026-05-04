package jpa.basic.coffeeshop.common.interceptor;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jpa.basic.coffeeshop.common.security.auth.LoginUserInfo;
import jpa.basic.coffeeshop.common.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.Arrays;

/**
 * JWT 쿠키에서 인증 정보를 추출하여 Thymeleaf 모델에 주입하는 인터셉터.
 * STATELESS 세션 환경에서 ${user} 모델 어트리뷰트로 로그인 상태를 전달합니다.
 * API 스펙에는 영향을 주지 않습니다.
 */
@Component
@RequiredArgsConstructor
public class AuthModelInterceptor implements HandlerInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) {

        // API 요청이거나 ModelAndView가 없으면 스킵
        if (modelAndView == null || request.getRequestURI().startsWith("/api/")) {
            return;
        }

        String token = resolveToken(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            Long   userId = jwtTokenProvider.getMemberId(token);
            String role   = jwtTokenProvider.getRole(token);

            LoginUserInfo userInfo = LoginUserInfo.builder()
                    .id(userId)
                    .role(role)
                    .build();

            modelAndView.addObject("user", userInfo);
        }
    }

    private String resolveToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        return Arrays.stream(cookies)
                .filter(cookie -> "access_token".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}