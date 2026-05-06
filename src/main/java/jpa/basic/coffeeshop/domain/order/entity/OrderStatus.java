package jpa.basic.coffeeshop.domain.order.entity;

import lombok.Getter;

@Getter
public enum OrderStatus {
    PENDING("주문 대기"),
    COMPLETED("주문 완료"),
    CANCELED("주문 취소");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }
}
