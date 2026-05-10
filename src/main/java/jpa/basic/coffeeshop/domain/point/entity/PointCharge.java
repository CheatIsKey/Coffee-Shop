package jpa.basic.coffeeshop.domain.point.entity;

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
        name = "point_charges",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_point_charges_pg_tid", columnNames = {"pg_tid"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointCharge extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentType paymentType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @Column(name = "pg_tid", length = 100, unique = true)
    private String pgTid;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Builder
    public PointCharge(Long amount, PaymentType paymentType, String pgTid, Long userId) {
        this.amount = amount;
        this.paymentType = paymentType;
        this.userId = userId;
        this.pgTid = pgTid;
        this.paymentStatus = PaymentStatus.PENDING;
    }

    // ──────────────────────────────────────────────────────────────────
    // 도메인 메서드
    // ──────────────────────────────────────────────────────────────────

    /**
     * 결제를 완료 처리
     * PENDING -> SUCCESS 전환 및 실제 결제 완료 일시를 기록
     */
    public void completePayment() {
        if (this.paymentStatus != PaymentStatus.PENDING) {
            throw new CustomException(ErrorCode.POINT_STATUS_NOT_PENDING);
        }
        this.paymentStatus = PaymentStatus.SUCCESS;
        this.paidAt = LocalDateTime.now();
    }

    /**
     * 결제를 실패 처리
     */
    public void failPayment() {
        if (this.paymentStatus != PaymentStatus.PENDING) {
            throw new CustomException(ErrorCode.POINT_STATUS_NOT_PENDING);
        }
        this.paymentStatus = PaymentStatus.FAILED;
    }
}
