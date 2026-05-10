package jpa.basic.coffeeshop.domain.menu.repository;

import com.querydsl.core.types.dsl.Wildcard;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jpa.basic.coffeeshop.domain.menu.entity.Menu;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static jpa.basic.coffeeshop.domain.menu.entity.QMenu.menu;

@Repository
@RequiredArgsConstructor
public class MenuQueryRepositoryImpl implements MenuQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * is_deleted = false 조건으로 전체 메뉴를 ID 오름차순 정렬 후 Offset 페이징 조회
     */
    @Override
    public Page<Menu> findMenusWithPaging(Pageable pageable) {
        List<Menu> content = queryFactory
                .selectFrom(menu)
                .where(menu.isDeleted.isFalse())
                .orderBy(menu.id.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(Wildcard.count)
                .from(menu)
                .where(menu.isDeleted.isFalse())
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * ID와 is_deleted = false 복합 조건으로 메뉴 단건 조회
     */
    @Override
    public Optional<Menu> findActiveMenuById(Long menuId) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(menu)
                        .where(
                                menu.id.eq(menuId),
                                menu.isDeleted.isFalse()
                        )
                        .fetchOne()
        );
    }
}
