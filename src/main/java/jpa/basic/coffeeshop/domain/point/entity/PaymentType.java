package jpa.basic.coffeeshop.domain.point.entity;

import lombok.Getter;

@Getter
public enum PaymentType {
    CARD("카드"),
    KAKAO_PAY("카카오페이"),
    TOSS_PAY("토스페이");

    private final String description;

    PaymentType(String description) {
        this.description = description;
    }
}
