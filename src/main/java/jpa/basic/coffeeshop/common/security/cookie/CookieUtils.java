package jpa.basic.coffeeshop.common.security.cookie;

import jakarta.servlet.http.Cookie;
import jpa.basic.coffeeshop.common.security.auth.AuthConstants;
import org.springframework.stereotype.Component;

@Component
public class CookieUtils {

    /**
     * HttpOnly 쿠키 생성
     * @param name   쿠키 이름
     * @param value  쿠키 값
     * @param maxAge 쿠키 수명 (초 단위)
     */
    public Cookie createCookie(String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath(AuthConstants.COOKIE_PATH_ROOT);
        cookie.setMaxAge(maxAge);
        cookie.setHttpOnly(true);   // JS에서 쿠키 접근 차단 → XSS 방어
        // cookie.setSecure(true);  // 수정 포인트: HTTPS 배포 시 활성화 (HTTP 개발 환경에서는 주석 유지)
        return cookie;
    }

    /**
     * 쿠키 삭제 — maxAge를 0으로 설정하면 브라우저가 즉시 파기
     */
    public Cookie deleteCookie(String name) {
        return createCookie(name, null, 0);
    }
}