package jpa.basic.coffeeshop.domain.menu.repository;

import jpa.basic.coffeeshop.domain.menu.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRepository extends JpaRepository<Menu, Long> {
}
