package jpa.basic.coffeeshop.domain.menu.repository;

import jpa.basic.coffeeshop.domain.menu.entity.Category;
import jpa.basic.coffeeshop.domain.menu.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

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


}
