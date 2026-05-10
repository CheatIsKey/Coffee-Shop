package jpa.basic.coffeeshop.domain.point.service;

import jpa.basic.coffeeshop.domain.point.dto.request.ChargePointRequest;
import jpa.basic.coffeeshop.domain.point.dto.response.ChargePointResponse;

public interface PointCommandService {

    /**
     * 포인트 충전
     */
    ChargePointResponse chargePoint(Long userId, ChargePointRequest request);
}
