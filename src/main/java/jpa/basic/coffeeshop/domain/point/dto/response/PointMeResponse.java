package jpa.basic.coffeeshop.domain.point.dto.response;

import java.util.List;

/**
 * 포인트 조회 응답 DTO
 *
 * 커서 기반 페이징
 */
public record PointMeResponse(
        Long userId,
        Long currentPoint,
        List<PointLogItemResponse> history,
        Long nextCursorId,
        boolean hasNext
) {
}
