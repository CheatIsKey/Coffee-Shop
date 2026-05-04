package jpa.basic.coffeeshop.common.security.auth;

import lombok.Builder;

@Builder
public record LoginUserInfo(
        Long id,
        String role
) {
}