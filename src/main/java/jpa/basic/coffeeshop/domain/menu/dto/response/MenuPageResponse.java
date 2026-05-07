package jpa.basic.coffeeshop.domain.menu.dto.response;

import jpa.basic.coffeeshop.domain.menu.entity.Menu;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 메뉴 목록 Offset 페이지 응답 DTO
 */
public record MenuPageResponse(
        List<MenuListItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
    public static MenuPageResponse from(Page<Menu> menuPage, int pageNumber) {
        List<MenuListItemResponse> content = menuPage.getContent()
                .stream()
                .map(MenuListItemResponse::from)
                .toList();

        return new MenuPageResponse(
                content,
                pageNumber,
                menuPage.getSize(),
                menuPage.getTotalElements(),
                menuPage.getTotalPages(),
                menuPage.hasNext()
        );
    }
}
