package jpa.basic.coffeeshop.domain.order.repository;

import jpa.basic.coffeeshop.domain.order.dto.request.AdminOrderSearchRequest;
import jpa.basic.coffeeshop.domain.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface OrderQueryRepository {

    /**
     * 일반 사용자 - 본인 주문 목록 커서 기반 조회
     * 무한 스크롤: orderUid 기준 내림차순
     */
    List<Order> findOrdersByUserWithCursor(Long userId, Long cursorId, int size);

    /**
     * 관리자 - 전체 주문 목록 동적 쿼리 + Offset 페이징
     * orderUid / 기간 / 이메일 필터 조합 가능
     */
    Page<Order> findOrdersForAdmin(AdminOrderSearchRequest request, Pageable pageable);

    /**
     * 주문 상세 조회
     * 일반 사용자는 본인 주문만 조회 가능
     * 삭제된 주문, PENDING 주문 제외
     */
    Optional<Order> findCompletedOrderByUid(String orderUid);
}
