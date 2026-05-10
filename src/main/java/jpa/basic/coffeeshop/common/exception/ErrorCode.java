package jpa.basic.coffeeshop.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // 인증/인가 에러 코드 (A###) ──────────────────────────────────
    AUTH_USER_NOT_FOUND(HttpStatus.UNAUTHORIZED, "A001", "로그인 정보가 올바르지 않습니다."),
    AUTH_UNAUTHENTICATED_USER(HttpStatus.UNAUTHORIZED, "A002", "로그인 정보가 올바르지 않습니다."),
    AUTH_INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "유효하지 않은 Refresh Token 입니다."),
    AUTH_FORBIDDEN_ACCESS(HttpStatus.FORBIDDEN, "A004", "접근 권한이 없습니다."),

    // 공통 에러 코드 (C###) ──────────────────────────────────
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "입력값이 올바르지 않습니다."),
    DATABASE_CONNECTION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버가 응답하지 않습니다."),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "C003", "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),

    // 사용자 관련 에러 코드 (U###) ──────────────────────────────────
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "U001", "이미 존재하는 이메일입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U002", "해당 유저는 존재하지 않습니다."),
    INSUFFICIENT_POINT(HttpStatus.BAD_REQUEST, "U003", "잔여 포인트가 부족합니다."),

    // 포인트 관련 에러 코드 (P###) ──────────────────────────────────
    INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "P001", "충전 금액은 1 이상이어야 합니다."),
    POINT_STATUS_NOT_PENDING(HttpStatus.BAD_REQUEST, "P002", "충전 상태가 결제 대기 상태가 아닙니다."),

    // 메뉴 관련 에러 코드 (M###) ──────────────────────────────────
    MENU_OUT_OF_STOCK(HttpStatus.CONFLICT, "M001", "재고가 부족합니다."),
    MENU_NOT_FOUND(HttpStatus.NOT_FOUND, "M002", "존재하지 않는 메뉴입니다."),
    MENU_ALREADY_EXISTS(HttpStatus.CONFLICT, "M003", "이미 존재하는 메뉴입니다."),
    MENU_OPTIMISTIC_LOCK_CONFLICT(HttpStatus.CONFLICT, "M004", "다른 관리자가 수정 중입니다. 잠시 후 다시 시도해주세요."),

    // 주문 관련 에러 코드 (O###) ──────────────────────────────────
    ORDER_STATUS_NOT_PENDING(HttpStatus.BAD_REQUEST, "O001", "주문 상태가 주문 대기 상태가 아닙니다.")

    ;


    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
