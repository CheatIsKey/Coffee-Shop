package jpa.basic.coffeeshop.domain.point.service;

import jpa.basic.coffeeshop.domain.point.dto.request.ChargePointRequest;
import jpa.basic.coffeeshop.domain.point.dto.response.ChargePointResponse;

public interface PointCommandService {

    /**
     * 포인트 충전
     */
    ChargePointResponse chargePoint(Long userId, ChargePointRequest request);

    /**
     * 주문 결제로 인한 포인트 사용 로그를 기록한다.
     * OrderCommandService에서 호출
     */
    void recordOrderUsage(Long userId, Long orderId, Long amount, Long remainPoint);
}
