package jpa.basic.coffeeshop.domain.point.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jpa.basic.coffeeshop.domain.point.entity.PointLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static jpa.basic.coffeeshop.domain.point.entity.QPointLog.pointLog;

@Repository
@RequiredArgsConstructor
public class PointQueryRepositoryImpl implements PointQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 커서 기반으로 포인트 변동 내역을 조회한다.
     *
     * WHERE 조건:
     * - userId 일치
     * - cursorId가 있으면 id < cursorId (해당 로그 이전 데이터만)
     */
    @Override
    public List<PointLog> findLogsWithCursor(Long userId, Long cursorId, int size) {
        return queryFactory
                .selectFrom(pointLog)
                .where(
                        pointLog.userId.eq(userId),
                        cursorIdLt(cursorId)
                )
                .orderBy(pointLog.id.desc())
                .limit(size + 1L)
                .fetch();
    }

    private BooleanExpression cursorIdLt(Long cursorId) {
        return cursorId != null ? pointLog.id.lt(cursorId) : null;
    }
}
