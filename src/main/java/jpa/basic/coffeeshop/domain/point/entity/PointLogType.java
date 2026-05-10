package jpa.basic.coffeeshop.domain.point.entity;

import lombok.Getter;

/**
 * 포인트 변동 유형
 *
 * CHARGE       : 현금 충전으로 인한 포인트 증가
 * USE          : 주문 결제로 인한 포인트 차감
 * CANCEL       : 주문 취소로 인한 포인트 환불 (현재 프로젝트 범위 외, 확장 대비)
 * ADMIN_ADJUST : 관리자 수동 조정 (현재 프로젝트 범위 외, 확장 대비)
 */
@Getter
public enum PointLogType {
    CHARGE("충전"),
    USE("사용"),
    CANCEL("취소"),
    ADMIN_ADJUST("관리자 조정");

    private final String description;

    PointLogType(String description) {
        this.description = description;
    }
}
