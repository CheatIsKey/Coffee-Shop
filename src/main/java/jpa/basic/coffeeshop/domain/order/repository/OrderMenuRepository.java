package jpa.basic.coffeeshop.domain.order.repository;

import jpa.basic.coffeeshop.domain.order.entity.OrderMenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderMenuRepository extends JpaRepository<OrderMenu, Long> {

    List<OrderMenu> findAllByOrderId(Long orderId);

    /**
     * IN 절 일괄 조회
     *
     * 주문 ID 목록으로 OrderMenu를 한 번에 조회한다.
     */
    List<OrderMenu> findAllByOrderIdIn(List<Long> orderIds);
}
