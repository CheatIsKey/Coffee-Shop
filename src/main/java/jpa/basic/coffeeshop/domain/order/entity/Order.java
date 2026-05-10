package jpa.basic.coffeeshop.domain.order.entity;

import jakarta.persistence.*;
import jpa.basic.coffeeshop.common.exception.CustomException;
import jpa.basic.coffeeshop.common.exception.ErrorCode;
import jpa.basic.coffeeshop.domain.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "orders",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_orders_user_fingerprint_deleted",
                        columnNames = {"user_id", "order_fingerprint", "deleted_at"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30, unique = true)
    private String orderUid;

    @Column(nullable = false)
    private Long totalAmount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 64, name = "order_fingerprint")
    private String orderFingerprint;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(nullable = false, name = "is_deleted")
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt = LocalDateTime.of(1970, 1,  1, 0, 0, 0);

    @Builder
    public Order(String orderUid, Long totalAmount, OrderStatus orderStatus, Long userId, String orderFingerprint) {
        this.orderUid = orderUid;
        this.totalAmount = totalAmount;
        this.orderStatus = orderStatus;
        this.userId = userId;
        this.orderFingerprint = orderFingerprint;
    }

    // ──────────────────────────────────────────────────────────────────
    // 도메인 메서드
    // ──────────────────────────────────────────────────────────────────

    /**
     * 주문을 완료 상태로 전환하고 최종 결제 금액을 기록
     * PENDING -> COMPLETED
     */
    public void complete(Long totalAmount) {
        if (orderStatus != OrderStatus.PENDING) {
            throw new CustomException(ErrorCode.ORDER_STATUS_NOT_PENDING);
        }
        this.totalAmount = totalAmount;
        this.orderStatus = OrderStatus.COMPLETED;
    }

    /**
     * 주문을 Soft Delete 처리
     * 낙관적 락이 동시 삭제 요청을 감지
     */
    public void softDelete() {
        if (this.isDeleted) {
            throw new CustomException(ErrorCode.ORDER_ALREADY_DELETED);
        }
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
        this.orderStatus = OrderStatus.CANCELED;
    }
}
