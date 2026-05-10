package jpa.basic.coffeeshop.domain.order.kafka;

import jpa.basic.coffeeshop.domain.order.entity.OutboxEvent;
import jpa.basic.coffeeshop.domain.order.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbox 이벤트 재발행 스케쥴러
 *
 * After Commit Hook에서 Kafka 발행에 실패한 이벤트를 재발행
 * 주기: 5분마다 (fixedDelay = 5분)
 * 대상: published = false + 생성된 지 2분 이상 지난 이벤트들 (2분 이내는 처리 중일 수 있으므로 제외)
 *
 * 다중 인스턴스 환경에서는 인스턴스들이 동시에 실행되면 중복 발행할 수 있다.
 * Kafka 멱등성 Producer(enable.idempotence)와 Consumer 측 중복 처리 로직으로 방어한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventScheduler {

    private final OutboxEventRepository outboxEventRepository;
    private final OrderEventProducer orderEventProducer;

    @Scheduled(fixedDelay = 300_000) // 5분
    @Transactional
    public void republishFailedEvents() {
        // 2분 이상 지난 미발행 이벤트만 대상
        LocalDateTime threshold = LocalDateTime.now();
        List<OutboxEvent> pendingEvents =
                outboxEventRepository.findAllByPublishedFalseAndCreatedAtBefore(threshold);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("[OutboxScheduler] 미발행 이벤트 {}건 재발행 시작", pendingEvents.size());

        int successCount = 0;
        for (OutboxEvent event : pendingEvents) {
            boolean success = orderEventProducer.publishFromOutbox(event);
            if (success) {
                event.markPublished();
                successCount++;
            }
        }

        log.info("[OutboxScheduler] 재발행 완료 - 성공: {}건 / 전체: {}건",
                successCount, pendingEvents.size());
    }
}
