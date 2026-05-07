package jpa.basic.coffeeshop.domain.menu.dto.response;

import jpa.basic.coffeeshop.domain.menu.entity.Menu;

/**
 * 메뉴 목록 조회 응답 DTO
 */
public record MenuListItemResponse(
        Long menuId,
        String menuName,
        int menuPrice,
        int menuStock,
        String category,
        String menuStatus
) {
    /**
     * Menu 엔터티로부터 응답 DTO를 생성하는 정적 팩토리 메서드
     */
    public static MenuListItemResponse from(Menu menu) {
        return new MenuListItemResponse(
                menu.getId(),
                menu.getMenuName(),
                menu.getMenuPrice(),
                menu.getMenuStock(),
                menu.getCategory().getDescription(),
                menu.getMenuStatus().getDescription()
        );
    }
}
