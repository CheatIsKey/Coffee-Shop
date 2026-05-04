package jpa.basic.coffeeshop.common.security.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenProvider {

    private static final String ISSUER = "allday-project-commerce";

    @Value("${jwt.secret-key}")
    private String secretKeyString;

    @Value("${jwt.access-token-validity-time}")
    private long accessTokenValidityInMilliseconds;

    @Value("${jwt.refresh-token-validity-time}")
    private long refreshTokenValidityInMilliseconds;

    private SecretKey key;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKeyString);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret-key는 최소 32바이트(256bit) 이상이어야 합니다.");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        log.info("HS256 대칭키가 성공적으로 초기화되었습니다.");
    }

    /** Access Token 발급 */
    public String createAccessToken(Long memberId, String role) {
        return buildToken(memberId, role, accessTokenValidityInMilliseconds);
    }

    /** Refresh Token 발급 (권한 정보 없이 PK만 담음) */
    public String createRefreshToken(Long memberId) {
        return buildToken(memberId, null, refreshTokenValidityInMilliseconds);
    }

    /**
     * 실제 JWT 생성 내부 메서드 — jjwt 0.12.x API
     */
    private String buildToken(Long memberId, String role, long validityTimeInMs) {
        Date now      = new Date();
        Date validity = new Date(now.getTime() + validityTimeInMs);

        var builder = Jwts.builder()
                .subject(String.valueOf(memberId)) // PK를 Subject로 지정
                .issuer(ISSUER)
                .issuedAt(now)
                .expiration(validity)
                .id(UUID.randomUUID().toString());

        if (role != null) {
            builder.claim("role", role); // Custom Claim 추가
        }

        return builder
                .signWith(key)
                .compact();
    }

    /** 토큰에서 회원 PK(Subject) 추출 */
    public Long getMemberId(String token) {
        String subject = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
        return Long.parseLong(subject);
    }

    /** 토큰에서 역할(role) 추출 — Refresh Token은 role이 없어 null 반환 가능 */
    public String getRole(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }

    /** Refresh Token 유효기간을 초(second) 단위로 반환 — 쿠키 maxAge 설정용 */
    public int getRefreshTokenValidityInSeconds() {
        return (int) (refreshTokenValidityInMilliseconds / 1000);
    }

    /** 토큰 검증 */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(key)
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException | SignatureException e) {
            log.warn("잘못된 JWT 서명입니다 (위조 의심): {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT 토큰입니다: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 JWT 토큰입니다: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT 토큰이 잘못되었습니다: {}", e.getMessage());
        }
        return false;
    }
}