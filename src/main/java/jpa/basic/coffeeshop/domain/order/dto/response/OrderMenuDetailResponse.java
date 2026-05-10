package jpa.basic.coffeeshop.domain.order.dto.response;

import jpa.basic.coffeeshop.domain.order.entity.OrderMenu;

public record OrderMenuDetailResponse(
        String menuName,
        int quantity,
        int menuPrice,
        long totalAmount
) {
    public static OrderMenuDetailResponse from(OrderMenu orderMenu) {
        return new OrderMenuDetailResponse(
                orderMenu.getMenuName(),
                orderMenu.getQuantity(),
                orderMenu.getMenuPrice(),
                orderMenu.calculateAmount()
        );
    }
}
