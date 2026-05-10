package jpa.basic.coffeeshop.domain.point.repository;

import jpa.basic.coffeeshop.domain.point.entity.PointLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointLogRepository extends JpaRepository<PointLog, Long> {
}
