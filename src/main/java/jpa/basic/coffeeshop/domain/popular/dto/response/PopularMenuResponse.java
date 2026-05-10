package jpa.basic.coffeeshop.domain.popular.dto.response;

import jpa.basic.coffeeshop.domain.popular.entity.PopularMenu;

/**
 * 인기 메뉴 응답 DTO
 */
public record PopularMenuResponse(
        int rank,
        Long menuId,
        String menuName,
        Long totalQuantity
) {
    /**
     * DB Fallback 시 PopularMenu 엔터티로부터 생성하는 정적 팩토리 메서드
     */
    public static PopularMenuResponse from(PopularMenu popularMenu) {
        return new PopularMenuResponse(
                popularMenu.getRank(),
                popularMenu.getMenuId(),
                popularMenu.getMenuName(),
                popularMenu.getTotalQuantity()
        );
    }
}
