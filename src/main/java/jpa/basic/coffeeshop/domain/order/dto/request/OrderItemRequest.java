package jpa.basic.coffeeshop.domain.order.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 주문 단일 메뉴 항목 요청 DTO
 */
public record OrderItemRequest(

        @NotNull(message = "메뉴 ID는 필수입니다.")
        Long menuId,

        @Positive(message = "주문 수량은 1 이상이어야 합니다.")
        int quantity
) {
}
