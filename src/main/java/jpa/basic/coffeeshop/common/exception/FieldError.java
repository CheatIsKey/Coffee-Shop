package jpa.basic.coffeeshop.common.exception;

public record FieldError(
        String field,          // 검증 실패한 필드명
        String rejectedValue,  // 거부된 입력값
        String message         // 검증 실패 메시지 (@NotBlank의 message 속성 등)
) {

    public static FieldError from(org.springframework.validation.FieldError error) {
        return new FieldError(
                error.getField(),
                error.getRejectedValue() != null ? error.getRejectedValue().toString() : null,
                error.getDefaultMessage()
        );
    }
}
