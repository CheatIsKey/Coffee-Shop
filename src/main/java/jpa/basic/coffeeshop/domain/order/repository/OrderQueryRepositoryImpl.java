package jpa.basic.coffeeshop.domain.order.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Wildcard;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jpa.basic.coffeeshop.domain.order.dto.request.AdminOrderSearchRequest;
import jpa.basic.coffeeshop.domain.order.entity.Order;
import jpa.basic.coffeeshop.domain.order.entity.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static jpa.basic.coffeeshop.domain.order.entity.QOrder.*;
import static jpa.basic.coffeeshop.domain.user.entity.QUser.*;

@Repository
@RequiredArgsConstructor
public class OrderQueryRepositoryImpl implements OrderQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 일반 사용자 본인 주문 커서 기반 조회
     */
    @Override
    public List<Order> findOrdersByUserWithCursor(Long userId, Long cursorId, int size) {
        return queryFactory
                .selectFrom(order)
                .where(
                        isOrderOwner(userId),
                        isNotDeleted(),
                        cursorIdLt(cursorId)
                )
                .orderBy(order.id.desc())
                .limit(size + 1)
                .fetch();
    }

    /**
     * 관리자 전체 주문 동적 검색 + Offset 페이징
     * email 필터 적용
     */
    @Override
    public Page<Order> findOrdersForAdmin(AdminOrderSearchRequest request, Pageable pageable) {
        BooleanExpression[] conditions = {
                isNotDeleted(),
                orderUidEq(request.orderUid()),
                createdAtGoe(request.startDate()),
                createdAtLoe(request.endDate()),
                userEmailEq(request.email())
        };

        List<Order> content = queryFactory
                .selectFrom(order)
                .leftJoin(user).on(order.userId.eq(user.id))
                .where(conditions)
                .orderBy(order.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(Wildcard.count)
                .from(order)
                .leftJoin(user).on(order.userId.eq(user.id))
                .where(conditions)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * 주문 상세 조회 (COMPLETED + 미삭제 주문만)
     * PENDING 상태는 결제가 완료되지 않은 상태이므로 상세 조회에서 제외
     */
    @Override
    public Optional<Order> findCompletedOrderByUid(String orderUid) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(order)
                        .where(
                                orderUidEq(orderUid),
                                isCompleted(),
                                isNotDeleted()
                        )
                        .fetchOne()
        );
    }

    private BooleanExpression isOrderOwner(Long userId) {
        return userId != null ? order.userId.eq(userId) : null;
    }

    private BooleanExpression isNotDeleted() {
        return order.isDeleted.isFalse();
    }

    private BooleanExpression cursorIdLt(Long cursorId) {
        return cursorId != null ? order.id.lt(cursorId) : null;
    }

    private BooleanExpression orderUidEq(String orderUid) {
        return StringUtils.hasText(orderUid) ? order.orderUid.eq(orderUid) : null;
    }

    private BooleanExpression createdAtGoe(LocalDate startDate) {
        return startDate != null ? order.createdAt.goe(startDate.atStartOfDay()) : null;
    }

    private BooleanExpression createdAtLoe(LocalDate endDate) {
        return endDate != null ? order.createdAt.loe(endDate.atTime(23, 59, 59)) : null;
    }

    private BooleanExpression userEmailEq(String email) {
        return StringUtils.hasText(email) ? user.email.eq(email) : null;
    }

    private BooleanExpression isCompleted() {
        return order.orderStatus.eq(OrderStatus.COMPLETED);
    }
}
