package jpa.basic.coffeeshop.domain.menu.repository;

import jpa.basic.coffeeshop.domain.menu.entity.Menu;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * QueryDSL 기반 메뉴 조회 전용 인터페이스
 *
 * 복잡한 동적 조건 쿼리와 Offset 페이징 조회
 */
public interface MenuQueryRepository {
    /**
     * Soft Delete 되지 않은 메뉴 목록을 Offset 페이징으로 조회
     * 정렬 기준: ID 오름차순
     */
    Page<Menu> findMenusWithPaging(Pageable pageable);

    /**
     * ID로 Soft Delete 되지 않은 메뉴 단건 조회
     * is_deleted = true 인 메뉴는 제외
     */
    Optional<Menu> findActiveMenuById(Long menuId);
}
