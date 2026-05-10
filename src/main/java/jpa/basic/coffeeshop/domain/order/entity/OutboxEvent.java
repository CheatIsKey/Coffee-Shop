package jpa.basic.coffeeshop.domain.order.entity;

import jakarta.persistence.*;
import jpa.basic.coffeeshop.domain.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Outbox Pattern 이벤트 엔터티
 *
 * Order 저장과 같은 트랜잭션에 함께 저장되어,
 * Kafka 발행 실패 시에도 이벤트를 유실하지 않는다.
 *
 * 1. Order 저장 + OutboxEvent 저장
 * 2. 커밋 후 즉시 Kafka 발행 시도 (After Commit Hook)
 *      - 성공: published = true
 *      - 실패: published = false 유지 -> Scheduler가 처리
 * 3. OutboxScheduler (5분마다): published = false 이벤트 재발행
 */
@Getter
@Entity
@Table(
        name = "outbox_events",
        // 스케쥴러가 빠르게 조회하기 위한 인덱스
        indexes = {
                @Index(name = "idx_outbox_published_created", columnList = "published, created_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 이벤트 발생 도메인
     * 다른 도메인의 이벤트도 같은 테이블로 관리 가능
     */
    @Column(nullable = false, length = 50, name = "aggregate_type")
    private String aggregateType;

    /**
     * 도메인 PK
     * 추후 이벤트와 원본 데이터를 연결할 때 사용
     */
    @Column(nullable = false, name = "aggregate_id")
    private Long aggregateId;

    /**
     * 이벤트 유형
     * Consumer가 어떤 처리를 해야 하는지 식별할 때 사용
     */
    @Column(nullable = false, length = 100, name = "event_type")
    private String eventType;

    /**
     * Kafka로 전송할 JSON 페이로드
     * ObjectMapper로 직렬화한 OrderCreatedEvent JSON 문자열
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    /**
     * Kafka 발행 완료 여부
     */
    @Column(nullable = false)
    private boolean published = false;

    /**
     * 실제 Kafka 발행 완료 일시
     */
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Builder
    public OutboxEvent(String aggregateType, Long aggregateId, String eventType, String payload) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
    }

    /**
     * Kafka 발행 완료 처리
     */
    public void markPublished() {
        this.published = true;
        this.publishedAt = LocalDateTime.now();
    }
}
