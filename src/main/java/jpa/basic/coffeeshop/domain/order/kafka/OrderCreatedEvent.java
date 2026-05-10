package jpa.basic.coffeeshop.domain.order.kafka;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Kafka로 발행되는 주문 생성 이벤트 페이로드
 *
 * 사용자 식별값(userId), 메뉴ID, 결제금액을 전송
 * 다건 주문을 지원하므로 items 리스트로 전달
 *
 * orderUid를 Kafka 메시지 키로 사용하여,
 * 동일 orderUid 메시지는 동일 Partition으로 라우팅 되어 순서 보장
 */
public record OrderCreatedEvent(
        Long userId,
        String orderUid,
        Long totalAmount,
        List<OrderItem> items,
        LocalDateTime createdAt
) {
    public record OrderItem(
            Long menuId,
            String menuName,
            int quantity,
            long amount         // 해당 메뉴의 총 금액 (price x quantity)
    ) {}
}
