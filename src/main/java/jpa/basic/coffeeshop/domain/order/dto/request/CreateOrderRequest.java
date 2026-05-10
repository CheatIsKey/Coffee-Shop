package jpa.basic.coffeeshop.domain.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 주문 생성 요청 DTO
 * 한 번의 주문으로 여러 종류의 메뉴를 구매할 수 있다.
 */
public record CreateOrderRequest(

        @NotEmpty(message = "주문 항목은 1개 이상이어야 합니다.")
        @Valid
        List<OrderItemRequest> items
) {
}
