package jpa.basic.coffeeshop.domain.menu.service;

import jpa.basic.coffeeshop.domain.menu.dto.request.CreateMenuRequest;
import jpa.basic.coffeeshop.domain.menu.entity.Menu;
import jpa.basic.coffeeshop.domain.menu.entity.UpdateMenuRequest;

public interface MenuCommandService {

    /**
     * 메뉴 생성
     *  - 관리자만 호출 가능
     *  - Redis 분산 락으로 카테고리 단위 동시 생성을 차단
     *  - DB UNIQUE 제약으로 2차 중복 방지를 보장
     */
    void createMenu(CreateMenuRequest request);

    /**
     * 메뉴 정보 수정
     *  - 관리자만 호출 가능
     *  - 낙관적 락으로 동시 수정 충돌을 감지
     */
    void updateMenu(Long menuId, UpdateMenuRequest request);

    /**
     * 메뉴를 Soft Delete 수행
     *  - 관리자만 호출 가능
     */
    void deleteMenu(Long menuId);

    /**
     * 재고를 비관적 락으로 차감하고 MenuStockLog를 기록
     * Order 도메인에서 주문 생성 시 호출
     */
    Menu decreaseStockWithLog(Long menuId, int quantity, Long orderId);
}
