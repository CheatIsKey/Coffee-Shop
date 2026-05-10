package jpa.basic.coffeeshop.domain.order.dto.response;

import jpa.basic.coffeeshop.domain.order.entity.Order;
import jpa.basic.coffeeshop.domain.order.entity.OrderMenu;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 목록 단일 항목 응답 DTO
 */
public record OrderListItemResponse(
        String orderUid,
        String email,
        String representativeMenuName,      // 주문 내 첫 번째 메뉴명
        int extraCount,                     // 대표 메뉴 외 추가 메뉴 수
        String orderStatus,
        Long totalAmount,
        LocalDateTime createdAt
) {
    public static OrderListItemResponse of(Order order, String email, List<OrderMenu> orderMenus) {
        String representativeName = orderMenus.isEmpty() ? "" : orderMenus.get(0).getMenuName();
        int extraCount = Math.max(0, orderMenus.size() - 1);

        return new OrderListItemResponse(
                order.getOrderUid(),
                email,
                representativeName,
                extraCount,
                order.getOrderStatus().getDescription(),
                order.getTotalAmount(),
                order.getCreatedAt()
        );
    }
}
