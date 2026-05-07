package jpa.basic.coffeeshop.domain.menu.entity;

import lombok.Getter;

@Getter
public enum Category {
    HOT_DRINK("따뜻한 음료"),
    COLD_DRINK("시원한 음료"),
    DESSERT("디저트"),
    CUSTOM_DRINK("커스텀 음료"),
    LIMITED_ITEM("한정 메뉴");

    private final String description;

    Category(String description) {
        this.description = description;
    }
}
