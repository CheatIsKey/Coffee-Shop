package jpa.basic.coffeeshop.common.exception;

import lombok.Builder;

import java.util.List;

@Builder
public record ErrorResponse(
        String message,
        List<FieldError> fieldErrors
) {
    // 단순 에러 메시지만 있을 때
    public static ErrorResponse from(String message) {
        return ErrorResponse.builder()
                .message(message)
                .build();
    }

    // 필드 검증 오류 목록이 있을 때
    public static ErrorResponse of(String message, List<FieldError> fieldErrors) {
        return ErrorResponse.builder()
                .message(message)
                .fieldErrors(fieldErrors)
                .build();
    }
}
