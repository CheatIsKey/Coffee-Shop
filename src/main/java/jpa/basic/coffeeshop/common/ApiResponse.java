package jpa.basic.coffeeshop.common;

import jpa.basic.coffeeshop.common.exception.ErrorCode;
import jpa.basic.coffeeshop.common.exception.ErrorResponse;
import jpa.basic.coffeeshop.common.exception.FieldError;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ApiResponse<T>(
        boolean success,
        String code,            // 성공: HTTP 상태코드 문자열 / 실패: ErrorCode.code
        T data,                 // 성공: 응답 데이터         / 실패: ErrorResponse
        LocalDateTime timestamp
) {
    // 응답 데이터가 있는 성공
    public static <T> ApiResponse<T> success(HttpStatus status, T data) {
        return new ApiResponse<>(true, String.valueOf(status.value()), data, LocalDateTime.now());
    }

    // 응답 데이터가 없는 성공 (예: 삭제, 수정)
    public static <T> ApiResponse<T> success(HttpStatus status) {
        return new ApiResponse<>(true, String.valueOf(status.value()), null, LocalDateTime.now());
    }

    // 단순 에러 응답
    public static ApiResponse<ErrorResponse> fail(ErrorCode errorCode) {
        return new ApiResponse<>(false, errorCode.getCode(),
                ErrorResponse.from(errorCode.getMessage()), LocalDateTime.now());
    }

    // 필드 검증 오류 포함 에러 응답
    public static ApiResponse<ErrorResponse> fail(ErrorCode errorCode, List<FieldError> fieldErrors) {
        return new ApiResponse<>(false, errorCode.getCode(),
                ErrorResponse.of(errorCode.getMessage(), fieldErrors), LocalDateTime.now());
    }
}
