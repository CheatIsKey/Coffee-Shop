package jpa.basic.coffeeshop.common.security.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginUserRequest(
        @Email @NotBlank(message = "이메일은 필수로 입력해야 합니다.")
        String email,

        @NotBlank @Size(min = 8, max = 20, message = "비밀번호는 최소 8자 ~ 최대 20자입니다.")
        String password
) {
}