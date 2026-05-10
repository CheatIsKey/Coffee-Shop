package jpa.basic.coffeeshop.domain.order.repository;

import jpa.basic.coffeeshop.domain.order.entity.OrderMenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderMenuRepository extends JpaRepository<OrderMenu, Long> {

    List<OrderMenu> findAllByOrderId(Long orderId);
}
