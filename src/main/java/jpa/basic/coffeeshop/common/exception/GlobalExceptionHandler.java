package jpa.basic.coffeeshop.common.exception;

import jpa.basic.coffeeshop.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 커스텀 예외 처리
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleCustomException(CustomException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        log.warn("[API - CUSTOM] {} - {}", errorCode.getCode(), errorCode.getMessage());

        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode));
    }

    /**
     * 검증 실패 통합 처리
     *
     * MethodArgumentNotValidException (@RequestBody @Valid 실패)과
     * BindException (@ModelAttribute @Valid 실패) 을 모두 처리
     *
     * MethodArgumentNotValidException은 BindException의 하위 클래스이므로
     * 부모 타입으로 통합 핸들링이 가능
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleMethodArgumentNotValid(BindException exception) {
        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
        List<FieldError> fieldErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::from)
                .toList();

        log.warn("[API - VALIDATION] {} - {}", errorCode.getCode(), errorCode.getMessage());

        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode, fieldErrors));
    }

    /**
     * DB UNIQUE 제약 위반 처리
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleDataIntegrityViolation(
            DataIntegrityViolationException exception
    ) {
        log.warn("[API - DATA_INTEGRITY] {}", exception.getMessage());

        String message = exception.getMessage() != null ? exception.getMessage() : "";

        // 주문 fingerprint 중복
        if (message.contains("UK_ORDERS_USER_FINGERPRINT")) {
            return ResponseEntity.status(ErrorCode.ORDER_ALREADY_EXISTS.getStatus())
                    .body(ApiResponse.fail(ErrorCode.ORDER_ALREADY_EXISTS));
        }

        // 메뉴 이름 + 카테고리 중복
        if (message.contains("UK_MENUS_NAME_CATEGORY_DELETED")) {
            return ResponseEntity.status(ErrorCode.MENU_ALREADY_EXISTS.getStatus())
                    .body(ApiResponse.fail(ErrorCode.MENU_ALREADY_EXISTS));
        }

        // 그 외 DB 제약 위반 -> 일반 에러
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.fail(ErrorCode.INVALID_INPUT_VALUE));
    }
}
