package jpa.basic.coffeeshop.domain.point.service;

import jpa.basic.coffeeshop.domain.point.dto.response.PointMeResponse;

public interface PointQueryService {

    /**
     * 로그인한 사용자의 현재 포인트와 변동 내역을 커서 기반으로 조회
     */
    PointMeResponse getMyPoint(Long userId, Long cursorId, int size);
}
