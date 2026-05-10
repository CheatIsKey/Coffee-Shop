package jpa.basic.coffeeshop.domain.point.service;

import jpa.basic.coffeeshop.domain.point.dto.response.PointLogItemResponse;
import jpa.basic.coffeeshop.domain.point.dto.response.PointMeResponse;
import jpa.basic.coffeeshop.domain.point.entity.PointLog;
import jpa.basic.coffeeshop.domain.point.repository.PointQueryRepository;
import jpa.basic.coffeeshop.domain.user.entity.User;
import jpa.basic.coffeeshop.domain.user.service.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PointQueryServiceImpl implements PointQueryService {

    private final UserQueryService userQueryService;
    private final PointQueryRepository pointQueryRepository;

    /**
     * 현재 포인트와 변동 내역을 커서 기반으로 조회
     *
     * hasNext 판별 방법:
     * size + 1개를 조회해서 실제 결과 수를 확인한다.
     * - 결과 수 > size  → 다음 페이지 존재 (hasNext = true)
     * - 결과 수 <= size → 마지막 페이지 (hasNext = false)
     *
     * nextCursorId:
     * 현재 페이지의 마지막 항목 ID를 반환한다.
     * 클라이언트는 이 값으로 다음 요청의 cursorId를 구성한다.
     */
    @Override
    public PointMeResponse getMyPoint(Long userId, Long cursorId, int size) {
        User user = userQueryService.getById(userId);

        List<PointLog> logs = pointQueryRepository.findLogsWithCursor(userId, cursorId, size);

        boolean hasNext = logs.size() > size;

        List<PointLog> pagedLogs = hasNext ? logs.subList(0, size) : logs;

        Long nextCursorId = hasNext
                ? pagedLogs.get(pagedLogs.size() - 1).getId()
                : null;

        List<PointLogItemResponse> history = pagedLogs.stream()
                .map(PointLogItemResponse::from)
                .toList();

        return new PointMeResponse(
                user.getId(),
                user.getPoint(),
                history,
                nextCursorId,
                hasNext
        );
    }
}
