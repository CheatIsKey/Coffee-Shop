package jpa.basic.coffeeshop.domain.order.dto.response;

import jpa.basic.coffeeshop.domain.order.entity.Order;
import jpa.basic.coffeeshop.domain.order.entity.OrderMenu;
import jpa.basic.coffeeshop.domain.user.entity.User;

import java.util.List;

/**
 * 주문 생성 응답 DTO
 */
public record CreateOrderResponse(
        String orderUid,
        String email,
        List<OrderMenuItemResponse> menuList,
        Long totalAmount
) {
    public static CreateOrderResponse of(Order order, User user, List<OrderMenu> orderMenus) {
        List<OrderMenuItemResponse> menuList = orderMenus.stream()
                .map(OrderMenuItemResponse::from)
                .toList();

        return new CreateOrderResponse(
                order.getOrderUid(),
                user.getEmail(),
                menuList,
                order.getTotalAmount()
        );
    }
}
