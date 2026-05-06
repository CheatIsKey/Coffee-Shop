package jpa.basic.coffeeshop.domain.menu.entity;

import lombok.Getter;

@Getter
public enum MenuStatus {
    ON_SALE("판매중"),
    SOLD_OUT("품절"),
    DISCONTINUED("단종");

    private final String description;

    MenuStatus(String description) {
        this.description = description;
    }
}
