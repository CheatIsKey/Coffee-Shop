package jpa.basic.coffeeshop.domain.point.repository;

import jpa.basic.coffeeshop.domain.point.entity.PointCharge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointChargeRepository extends JpaRepository<PointCharge, Long> {
}
