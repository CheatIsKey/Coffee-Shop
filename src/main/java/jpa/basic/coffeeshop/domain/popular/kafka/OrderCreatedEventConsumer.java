package jpa.basic.coffeeshop.domain.popular.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import jpa.basic.coffeeshop.domain.order.kafka.OrderCreatedEvent;
import jpa.basic.coffeeshop.domain.popular.redis.PopularMenuRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 주문 생성 이벤트 Kafka Consumer
 *
 * 역할:
 *  - order.created 토픽에서 주문 이벤트를 수신하여
 *    메뉴별 주문 수량을 Redis ZSET에 누적한다.
 *
 * 실패 처리:
 *  - try-catch로 예외를 잡아 로그만 기록한다.
 *  - 예외를 던지면 Kafka가 해당 메시지를 재처리(재전송)하기 때문에
 *    Redis ZINCRBY가 중복 실행되어 집계 오류가 발생할 수 있다.
 *  - 현재는 At-Least-Once 정책을 수용하고, 향후 DLQ(Dead Letter Queue) 도입을 고려한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedEventConsumer {

    private final PopularMenuRedisService popularMenuRedisService;
    private final ObjectMapper objectMapper;

    /**
     * order.created 토픽 메시지를 수신하여 Redis ZSET을 업데이트한다.
     */
    @KafkaListener(
            topics = "${kafka.topic.order-created}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(String payload) {
        try {
            OrderCreatedEvent event = objectMapper.readValue(payload, OrderCreatedEvent.class);
            
            // 주문 내 각 메뉴의 수량을 당일 ZSET에 누적
            for (OrderCreatedEvent.OrderItem item : event.items()) {
                popularMenuRedisService.incrementMenuScore(item.menuId(), item.quantity());
            }

            log.info("[KafkaConsumer] order.created 처리 완료 - orderUid: {}, itemCount: {}",
                    event.orderUid(), event.items().size());

        } catch (Exception e) {
            // 파싱 실패 등의 예외: 로그만 기록하고 offset 커밋 진행
            log.error("[KafkaConsumer] 처리 실패 - payload: {}, error: {}", payload, e.getMessage());
        }
    }
}
