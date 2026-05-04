package jpa.basic.coffeeshop.common.security.auth;

public record AuthTokens(
        String accessToken,
        String refreshToken
) {

    public static AuthTokens of(String accessToken, String refreshToken) {
        return new AuthTokens(accessToken, refreshToken);
    }
}