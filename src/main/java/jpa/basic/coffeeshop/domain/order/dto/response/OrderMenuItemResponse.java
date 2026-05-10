package jpa.basic.coffeeshop.domain.order.dto.response;

import jpa.basic.coffeeshop.domain.order.entity.OrderMenu;

/**
 * 주문 생성 응답 내 메뉴 항목 DTO
 */
public record OrderMenuItemResponse(
        String menuName,
        int menuPrice,
        int quantity,
        long amount     // menuPrice x quantity
) {
    public static OrderMenuItemResponse from(OrderMenu orderMenu) {
        return new OrderMenuItemResponse(
                orderMenu.getMenuName(),
                orderMenu.getMenuPrice(),
                orderMenu.getQuantity(),
                orderMenu.calculateAmount()
        );
    }
}
