package jpa.basic.coffeeshop.domain.menu.repository;

import jpa.basic.coffeeshop.domain.menu.entity.MenuStockLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuStockLogRepository extends JpaRepository<MenuStockLog, Long> {
}
