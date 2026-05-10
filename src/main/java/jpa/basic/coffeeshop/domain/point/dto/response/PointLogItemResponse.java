package jpa.basic.coffeeshop.domain.point.dto.response;

import jpa.basic.coffeeshop.domain.point.entity.PointLog;

import java.time.LocalDateTime;

/**
 * 포인트 변동 내역 단일 항목 응답 DTO
 */
public record PointLogItemResponse(
        Long id,
        String type,
        Long amount,
        Long remainPoint,
        Long chargeId,
        Long orderId,
        LocalDateTime createdAt
) {
    public static PointLogItemResponse from(PointLog log) {
        return new PointLogItemResponse(
                log.getId(),
                log.getType().getDescription(),
                log.getAmount(),
                log.getRemainPoint(),
                log.getChargeId(),
                log.getOrderId(),
                log.getCreatedAt()
        );
    }
}
