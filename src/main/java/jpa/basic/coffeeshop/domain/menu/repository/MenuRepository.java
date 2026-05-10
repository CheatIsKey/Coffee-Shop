package jpa.basic.coffeeshop.domain.menu.repository;

import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;
import jpa.basic.coffeeshop.domain.menu.entity.Category;
import jpa.basic.coffeeshop.domain.menu.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    /**
     * 메뉴 생성 중복 체크
     *
     * 동일 이름 + 카테고리의 활성 메뉴 존재 여부
     * isDeleted = false 조건으로 Soft Delete 처리된 메뉴는 제외
     */
    boolean existsByMenuNameAndCategoryAndIsDeletedFalse(String menuName, Category category);

    /**
     * 메뉴 수정 중복 체크
     *
     * 수정 대상 메뉴(id)를 제외한 동일 이름 + 카테고리 활성 메뉴 존재 여부
     */
    boolean existsByMenuNameAndCategoryAndIsDeletedFalseAndIdNot(
            String menuName, Category category, Long id
    );

    /**
     * 재고 차감 시 비관적 락으로 메뉴 조회
     * 다수의 사용자가 동시에 동일 메뉴를 주문할 때 재고 정합성을 보장
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
           SELECT m
           FROM Menu m
           WHERE m.id = :id
           AND m.isDeleted = false
           """)
    Optional<Menu> findByIdWithLock(@Param("id") Long id);
}
