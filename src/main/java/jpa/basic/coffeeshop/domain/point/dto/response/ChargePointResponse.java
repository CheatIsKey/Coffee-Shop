package jpa.basic.coffeeshop.domain.point.dto.response;

import jpa.basic.coffeeshop.domain.point.entity.PointCharge;

import java.time.LocalDateTime;

/**
 * 포인트 충전 응답 DTO
 */
public record ChargePointResponse(
        String pgTid,
        Long chargeAmount,
        Long remainPoint,
        LocalDateTime createdAt
) {
    public static ChargePointResponse of(PointCharge pointCharge, Long remainPoint) {
        return new ChargePointResponse(
                pointCharge.getPgTid(),
                pointCharge.getAmount(),
                remainPoint,
                pointCharge.getPaidAt()
        );
    }
}
