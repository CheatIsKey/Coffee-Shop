package jpa.basic.coffeeshop.domain.order.dto.response;

import jpa.basic.coffeeshop.domain.order.entity.Order;
import jpa.basic.coffeeshop.domain.order.entity.OrderMenu;

import java.util.List;

/**
 * 주문 상세 조회 응답 DTO
 */
public record OrderDetailResponse(
        String orderUid,
        List<OrderMenuDetailResponse> menuDetail,
        Long totalAmount
) {
    public static OrderDetailResponse of(Order order, List<OrderMenu> orderMenus) {
        List<OrderMenuDetailResponse> details = orderMenus.stream()
                .map(OrderMenuDetailResponse::from)
                .toList();

        return new OrderDetailResponse(
                order.getOrderUid(),
                details,
                order.getTotalAmount()
        );
    }
}
