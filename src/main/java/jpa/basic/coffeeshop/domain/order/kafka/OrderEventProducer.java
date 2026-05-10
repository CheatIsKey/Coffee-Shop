package jpa.basic.coffeeshop.domain.order.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import jpa.basic.coffeeshop.domain.order.entity.OutboxEvent;
import jpa.basic.coffeeshop.domain.order.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * Kafka 주문 이벤트 발행 컴포넌트
 *
 * 발행 경로:
 *  1. After Commit Hook (즉시 발행): 주문 생성 트랜잭션 커밋 직후 호출
 *      -> 성공 시 OutboxEvent.published = true (새 트랜잭션)
 *      -> 실패 시 로그만 기록, Scheduler가 재발행 처리
 *  2. OutboxScheduler (재발행)    : published = false 이벤트를 주기적으로 재발행
 *      -> 동일 메서드(publishEvent)를 공유
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxEventRepository  outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topic.order-created}")
    private String orderCreatedTopic;

    /**
     * After Commit Hook에서 호출 - 즉시 발행 시도
     * 새 트랜잭션(REQUIRES_NEW)을 열어 OutboxEvent 상태를 독립적으로 업데이트한다.
     *
     * After Commit Hook은 원래 트랜잭션이 이미 종료된 후 실행되기 때문에
     * OutboxEvent 상태 업데이트를 위해 새 트랜잭션(REQUIRES_NEW)을 준다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void tryPublishAfterCommit(Long outboxEventId, OrderCreatedEvent event) {
        try {
            // ObjectMapper로 직렬화 후 String으로 발행
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(orderCreatedTopic, event.orderUid(), payload).get(3, TimeUnit.SECONDS);

            // 발행 성공: OutboxEvent published = true 업데이트
            // 변경 감지로 자동 UPDATE
            outboxEventRepository.findById(outboxEventId).ifPresent(OutboxEvent::markPublished);

            log.info("[Kafka] 즉시 발행 성공 - topic: {}, orderUid: {}",
                    orderCreatedTopic, event.orderUid());

        } catch (Exception e) {
            // 발행 실패: 로그만 기록
            // OutboxScheduler가 5분 후 재발행 처리
            log.warn("[Kafka] 즉시 발행 실패 - outboxEventId: {}, Scheduler 처리 예정. error: {}",
                    outboxEventId, e.getMessage());
        }
    }

    /**
     * OutboxScheduler에서 호출 - 재발행
     *
     * OutboxEvent.payload는 이미 JSON String이므로 역직렬화 없이 그대로 Kafka로 발행
     * orderUid는 payload에서 꺼내지 않고 aggregateId 기반 키를 사용
     */
    public boolean publishFromOutbox(OutboxEvent outboxEvent) {
        try {
            // payload JSON String을 그대로 발행
            kafkaTemplate.send(
                    orderCreatedTopic,
                    String.valueOf(outboxEvent.getAggregateId()),  // key: orderId
                    outboxEvent.getPayload()                       // value: JSON String
            ).get(3, TimeUnit.SECONDS);

            log.info("[Kafka] Outbox 재발행 성공 - outboxEventId: {}", outboxEvent.getId());
            return true;

        } catch (Exception e) {
            log.warn("[Kafka] Outbox 재발행 실패 - outboxEventId: {}, error: {}",
                    outboxEvent.getId(), e.getMessage());
            return false;
        }
    }
}
