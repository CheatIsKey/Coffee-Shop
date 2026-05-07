package jpa.basic.coffeeshop.domain.menu.service;

import jpa.basic.coffeeshop.domain.menu.dto.response.MenuDetailResponse;
import jpa.basic.coffeeshop.domain.menu.dto.response.MenuPageResponse;

public interface MenuQueryService {

    /**
     * 삭제되지 않은 메뉴 목록을 Offset 페이징으로 조회
     *
     * @param page  : 페이지 번호 (1-indexed)
     * @param size  : 페이지당 항목 수
     * @return      : 페이지 응답 DTO
     */
    MenuPageResponse getMenus(int page, int size);

    /**
     * 메뉴 ID로 단건 조회
     * 삭제된 메뉴 또는 존재하지 않는 메뉴는 예외 발생
     *
     * @param menuId    : 조회할 메뉴 ID
     * @return          : 메뉴 상세 응답 DTO
     */
    MenuDetailResponse getMenuById(Long menuId);
}
