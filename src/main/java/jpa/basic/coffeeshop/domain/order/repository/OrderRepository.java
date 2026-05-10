package jpa.basic.coffeeshop.domain.order.repository;

import jpa.basic.coffeeshop.domain.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderUid(String orderUid);
}
