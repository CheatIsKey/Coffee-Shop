package jpa.basic.coffeeshop.domain.point.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jpa.basic.coffeeshop.domain.point.entity.PaymentType;

/**
 * 포인트 충전 요청 DTO
 */
public record ChargePointRequest(
        @NotNull(message = "충전 금액은 필수입니다.")
        @Positive(message = "충전 금액은 1 이상이어야 합니다.")
        Long amount,

        @NotNull(message = "결제 수단은 필수입니다.")
        PaymentType paymentType
) {
}
