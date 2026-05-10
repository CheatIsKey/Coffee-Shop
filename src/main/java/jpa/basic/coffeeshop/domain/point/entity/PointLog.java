package jpa.basic.coffeeshop.domain.point.entity;

import jakarta.persistence.*;
import jpa.basic.coffeeshop.domain.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * chargeId / orderId 구분:
 * - type = CHARGE      → chargeId에 PointCharge.id 저장, orderId = null
 * - type = USE/CANCEL  → orderId에 Order.id 저장, chargeId = null
 * - type = ADMIN_ADJUST → 둘 다 null 가능
 */
@Getter
@Entity
@Table(name = "point_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointLog extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PointLogType type;

    @Column(nullable = false)
    private Long amount;

    @Column(name = "remain_point", nullable = false)
    private Long remainPoint;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "charge_id")
    private Long chargeId;

    @Builder
    public PointLog(PointLogType type, Long amount, Long remainPoint, Long userId, Long orderId, Long chargeId) {
        this.type = type;
        this.amount = amount;
        this.remainPoint = remainPoint;
        this.userId = userId;
        this.orderId = orderId;
        this.chargeId = chargeId;
    }
}
