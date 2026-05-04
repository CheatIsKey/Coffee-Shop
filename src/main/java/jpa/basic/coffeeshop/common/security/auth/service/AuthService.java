package jpa.basic.coffeeshop.common.security.auth.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import jpa.basic.coffeeshop.common.exception.CustomException;
import jpa.basic.coffeeshop.common.exception.ErrorCode;
import jpa.basic.coffeeshop.common.security.auth.AuthConstants;
import jpa.basic.coffeeshop.common.security.auth.AuthTokens;
import jpa.basic.coffeeshop.common.security.auth.dto.request.CreateUserRequest;
import jpa.basic.coffeeshop.common.security.auth.dto.request.LoginUserRequest;
import jpa.basic.coffeeshop.common.security.cookie.CookieUtils;
import jpa.basic.coffeeshop.common.security.jwt.JwtTokenProvider;
import jpa.basic.coffeeshop.domain.user.entity.User;
import jpa.basic.coffeeshop.domain.user.service.UserCommandService;
import jpa.basic.coffeeshop.domain.user.service.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final PasswordEncoder       passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserQueryService userQueryService;
    private final UserCommandService userCommandService;
    private final CookieUtils cookieUtils;

    /**
     * 회원가입
     * 이메일 중복 검사 후 User 저장
     * 가입 직후 바로 로그인 상태가 되도록 UX를 고려해 access_token, refresh_token 발급
     *
     * @param request   : 클라이언트가 보낸 이메일/비밀번호 JSON
     */
    public void signup(CreateUserRequest request) {
        String encodedPassword = passwordEncoder.encode(request.password());
        userCommandService.create(request.email(), encodedPassword);
    }

    /**
     * 로그인
     * 이메일로 사용자를 조회하고 비밀번호를 검증
     * 성공 → access_token(30분) + refresh_token(7일) 발급하고 쿠키에 담기
     *
     * @param request   : 클라이언트가 보낸 이메일/비밀번호 JSON
     * @param response  : 쿠키를 심기 위한 HttpServletResponse
     */
    public void login(LoginUserRequest request, HttpServletResponse response) {
        User user = userQueryService.getByEmail(request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHENTICATED_USER);
        }

        AuthTokens authTokens = AuthTokens.of(
                jwtTokenProvider.createAccessToken(user.getId(), user.getRole().name()),
                jwtTokenProvider.createRefreshToken(user.getId())
        );

        response.addCookie(cookieUtils.createCookie(
                AuthConstants.ACCESS_TOKEN,
                authTokens.accessToken(),
                30 * 60                                                 // 30분
        ));

        response.addCookie(cookieUtils.createCookie(
                AuthConstants.REFRESH_TOKEN,
                authTokens.refreshToken(),
                jwtTokenProvider.getRefreshTokenValidityInSeconds()     // 7일
        ));
    }

    /**
     * 로그아웃
     * 브라우저의 access_token과 refresh_token 쿠키를 maxAge = 0으로 즉시 만료
     *
     * @param response  : 쿠키를 삭제하기 위한 HttpServletResponse
     */
    public void logout(HttpServletResponse response) {
        response.addCookie(cookieUtils.deleteCookie(AuthConstants.ACCESS_TOKEN));
        response.addCookie(cookieUtils.deleteCookie(AuthConstants.REFRESH_TOKEN));
    }

    /**
     * access_token 재발급
     * 쿠키에서 refresh_token을 꺼내 유효성을 검증
     * 성공 → 사용자를 조회하여 새 access_token을 발급
     * 실패 → 두 쿠키 모두 삭제
     *
     * @param request   : 쿠키에서 refresh_token을 읽기 위한 HttpServletRequest
     * @param response  : 새 access_token 쿠키를 심기 위한 HttpServletResponse
     */
    public void reissue(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshToken(request);

        if (refreshToken == null) {
            throw new CustomException(ErrorCode.AUTH_INVALID_REFRESH_TOKEN);
        }

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            response.addCookie(cookieUtils.deleteCookie(AuthConstants.ACCESS_TOKEN));
            response.addCookie(cookieUtils.deleteCookie(AuthConstants.REFRESH_TOKEN));
            throw new CustomException(ErrorCode.AUTH_INVALID_REFRESH_TOKEN);
        }

        Long userId = jwtTokenProvider.getMemberId(refreshToken);
        User user = userQueryService.getById(userId);

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole().name());
        response.addCookie(cookieUtils.createCookie(
                AuthConstants.ACCESS_TOKEN,
                accessToken,
                30 * 60
        ));
    }

    /**
     * 요청 쿠키 배열에서 refresh_token 값을 추출하는 내부 헬퍼 메서드
     */
    private String extractRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        return Arrays.stream(cookies)
                .filter(cookie -> AuthConstants.REFRESH_TOKEN.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
