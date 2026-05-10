package jpa.basic.coffeeshop.domain.order.repository;

import jpa.basic.coffeeshop.domain.order.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * 미발행 상태이면서 특정 시각 이전에 생성된 OutboxEvent 목록 조회
     * Scheduler가 재발행 대상을 찾을 때 사용
     * createdAt 조건으로 방금 생성된 이벤트는 제외
     */
    List<OutboxEvent> findAllByPublishedFalseAndCreatedAtBefore(LocalDateTime threshold);
}
