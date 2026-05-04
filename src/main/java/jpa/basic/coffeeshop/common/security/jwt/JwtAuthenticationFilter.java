package jpa.basic.coffeeshop.common.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jpa.basic.coffeeshop.common.security.auth.AuthConstants;
import jpa.basic.coffeeshop.common.security.auth.LoginUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            Long   memberId = jwtTokenProvider.getMemberId(token);
            String role     = jwtTokenProvider.getRole(token);

            // ArgumentResolver에서 authentication.getPrincipal()로 꺼낼 수 있도록 DTO로 감쌈
            LoginUserInfo loginUser = LoginUserInfo.builder()
                    .id(memberId)
                    .role(role)
                    .build();

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    loginUser,  // Principal = LoginUserInfo
                    null,
                    Collections.singletonList(
                            new SimpleGrantedAuthority(StringUtils.hasText(role) ? role : "ROLE_USER")
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Security Context에 '{}' 인증 정보를 저장했습니다. (uri: {})", memberId, request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        return Arrays.stream(cookies)
                .filter(cookie -> AuthConstants.ACCESS_TOKEN.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}