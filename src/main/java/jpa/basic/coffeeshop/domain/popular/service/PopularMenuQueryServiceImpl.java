package jpa.basic.coffeeshop.domain.popular.service;

import jpa.basic.coffeeshop.domain.menu.entity.Menu;
import jpa.basic.coffeeshop.domain.menu.service.MenuQueryService;
import jpa.basic.coffeeshop.domain.popular.dto.response.PopularMenuResponse;
import jpa.basic.coffeeshop.domain.popular.redis.PopularMenuRedisService;
import jpa.basic.coffeeshop.domain.popular.repository.PopularMenuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PopularMenuQueryServiceImpl implements PopularMenuQueryService {

    private final PopularMenuRedisService popularMenuRedisService;
    private final PopularMenuRepository  popularMenuRepository;
    private final MenuQueryService menuQueryService;

    /**
     * 최근 7일 인기 메뉴 Top3 조회
     *
     * 조회 우선순위:
     *  1. Redis ZSET: 최근 7일 일별 ZSTE을 ZUNIONSTORE로 합산 -> Top3
     *  2. PopularMenus 테이블: Redis 결과가 없거나 예외 발생 시 최신 스냅샷 반환
     */
    @Override
    public List<PopularMenuResponse> getPopularMenus() {
        try {
            List<ZSetOperations.TypedTuple<String>> top3 = popularMenuRedisService.getTop3();

            if (!top3.isEmpty()) {
                return buildResponseFromRedis(top3);
            }

            log.info("[PopularMenu] Redis ZSET 비어있음 — DB Fallback");

        } catch (Exception e) {
            log.warn("[PopularMenu] Redis 조회 실패 — DB Fallback. error: {}", e.getMessage());
        }

        return popularMenuRepository.findLatestTop3().stream()
                .map(PopularMenuResponse::from)
                .toList();
    }

    /**
     * Redis ZSET 결과로 응답 DTO를 생성한다.
     */
    private List<PopularMenuResponse> buildResponseFromRedis(List<ZSetOperations.TypedTuple<String>> top3) {
        List<Long> menuIds = top3.stream()
                .map(t -> Long.parseLong(t.getValue()))
                .toList();

        Map<Long, String> nameMap = menuQueryService.getAllMenusById(menuIds).stream()
                .collect(Collectors.toMap(Menu::getId, Menu::getMenuName));

        List<PopularMenuResponse> response = new ArrayList<>();
        for (int i = 0; i < top3.size(); i++) {
            ZSetOperations.TypedTuple<String> tuple = top3.get(i);
            Long menuId = Long.parseLong(tuple.getValue());
            Long totalQuantity = tuple.getScore() != null ? tuple.getScore().longValue() : 0L;

            response.add(new PopularMenuResponse(
                    i + 1,
                    menuId,
                    nameMap.getOrDefault(menuId, "알 수 없는 메뉴"),
                    totalQuantity
            ));
        }
        return response;
    }
}
