package jpa.basic.coffeeshop.domain.point.entity;

import lombok.Getter;

/**
 * 포인트 충전 처리 상태
 *
 * PENDING  : 충전 요청이 생성되었으나 아직 완료되지 않은 상태
 * SUCCESS  : 결제 및 포인트 충전이 완료된 상태
 * FAILED   : 결제 실패로 포인트가 충전되지 않은 상태
 *
 * 이 프로젝트에서는 PG 연동을 생략하므로 PENDING → SUCCESS 흐름만 사용한다.
 * FAILED는 추후 실제 PG 연동 시 사용한다.
 */
@Getter
public enum PaymentStatus {
    PENDING, SUCCESS, FAILED
}
