package jpa.basic.coffeeshop.domain.point.repository;

import jpa.basic.coffeeshop.domain.point.entity.PointLog;

import java.util.List;

/**
 * 포인트 조회 전용 QueryDSL 인터페이스
 */
public interface PointQueryRepository {

    /**
     * 커서 기반 포인트 변동 내역을 조회한다.
     *
     * 커서 방식 설명:
     * - cursorId가 null이면 가장 최신 데이터부터 조회한다.
     * - cursorId가 있으면 해당 ID보다 작은(이전) 데이터를 조회한다.
     * - ID 내림차순 정렬로 최신순을 보장한다.
     */
    List<PointLog> findLogsWithCursor(Long userId, Long cursorId, int size);
}
