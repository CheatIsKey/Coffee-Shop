package jpa.basic.coffeeshop.domain.menu.dto.response;

import jpa.basic.coffeeshop.domain.menu.entity.Menu;

import java.time.LocalDateTime;

/**
 * 메뉴 상세 조회 응답 DTO
 */
public record MenuDetailResponse(
        Long menuId,
        String menuName,
        int menuPrice,
        int menuStock,
        String category,
        String menuStatus,
        LocalDateTime createdAt
) {
    public static MenuDetailResponse from(Menu menu) {
        return new MenuDetailResponse(
                menu.getId(),
                menu.getMenuName(),
                menu.getMenuPrice(),
                menu.getMenuStock(),
                menu.getCategory().getDescription(),
                menu.getMenuStatus().getDescription(),
                menu.getCreatedAt()
        );
    }
}
