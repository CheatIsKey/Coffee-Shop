package jpa.basic.coffeeshop.domain.popular.scheduler;

import jakarta.annotation.PostConstruct;
import jpa.basic.coffeeshop.domain.menu.entity.Menu;
import jpa.basic.coffeeshop.domain.menu.service.MenuQueryService;
import jpa.basic.coffeeshop.domain.popular.entity.PopularMenu;
import jpa.basic.coffeeshop.domain.popular.redis.PopularMenuRedisService;
import jpa.basic.coffeeshop.domain.popular.repository.PopularMenuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 인기 메뉴 스케쥴러
 *
 * 역할:
 *  1. Warm-up: 앱 시작 시 DB 스냅샷 -> Redis ZSET 복원
 *  2. Write-back: 1시간마다 Redis ZSET -> DB PopularMenus 스냅샷 저장
 *
 * Warm-up 전략:
 *  - PopularMenus 테이블의 최신 스냅샷(Top3)을 Redis에 로드한다.
 *  - 완벽한 7일치 재구성은 아니지만, 서비스 재시작 직후 빠르게 데이터를 복원한다.
 *  - 이후 Kafka Consumer가 실시간으로 ZSET을 계속 업데이트한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PopularMenuScheduler {

    private final PopularMenuRedisService popularMenuRedisService;
    private final PopularMenuRepository popularMenuRepository;
    private final MenuQueryService  menuQueryService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 앱 시작 시 Redis Warm-up
     *
     * DB의 최신 PopularMenus 스냅샷을 읽어 Redis ZSET을 복원한다.
     * 스냅샷이 없으면 Warm-up을 스킵하고 Kafka Consumer가 채운다.
     */
    @PostConstruct
    public void warmUp() {
        log.info("[PopularMenuScheduler] Redis Warm-up 시작");

        List<PopularMenu> latestTop3 = popularMenuRepository.findLatestTop3();
        if (latestTop3.isEmpty()) {
            log.info("[PopularMenuScheduler] 스냅샷 없음 — Warm-up 스킵");
            return;
        }

        // 스냅샷 일시를 날짜 키로 변환하여 해당 날짜 ZSET에 복원
        String dateKey = latestTop3.get(0).getSnapshotAt().format(DATE_FORMATTER);
        Map<Long, Long> menuQuantityMap = latestTop3.stream()
                .collect(Collectors.toMap(PopularMenu::getMenuId, PopularMenu::getTotalQuantity));

        popularMenuRedisService.warmUpDailyKey(dateKey, menuQuantityMap);
        log.info("[PopularMenuScheduler] Warm-up 완료 - dateKey: {}, count: {}",
                dateKey, menuQuantityMap.size());
    }

    /**
     * 1시간 주기 DB Write-back
     *
     * Redis ZSET Top3를 읽어 PopularMenus 테이블에 스냅샷을 저장한다.
     * Redis가 비어있으면 Write-back을 스킵한다.
     */
    @Scheduled(fixedDelay = 3_600_000) // 1시간
    @Transactional
    public void writeBackToDb() {
        log.info("[PopularMenuScheduler] Write-back 시작");

        List<ZSetOperations.TypedTuple<String>> top3 = popularMenuRedisService.getTop3();
        if (top3.isEmpty()) {
            log.info("[PopularMenuScheduler] Redis ZSET 비어있음 — Write-back 스킵");
            return;
        }

        List<Long> menuIds = top3.stream()
                .map(t -> Long.parseLong(t.getValue()))
                .toList();

        Map<Long, String> nameMap = menuQueryService.getAllMenusById(menuIds)
                .stream().collect(Collectors.toMap(Menu::getId, Menu::getMenuName));

        // PopularMenu 스냅샷 생성
        LocalDateTime now = LocalDateTime.now();
        List<PopularMenu> snapshots = new ArrayList<>();

        for (int i = 0; i < top3.size(); i++) {
            ZSetOperations.TypedTuple<String> tuple = top3.get(i);
            Long menuId = Long.parseLong(tuple.getValue());
            snapshots.add(PopularMenu.builder()
                    .menuId(menuId)
                    .menuName(nameMap.getOrDefault(menuId, "알 수 없는 메뉴"))
                    .totalQuantity(tuple.getScore() != null ? tuple.getScore().longValue() : 0L)
                    .rank(i + 1)
                    .snapshotAt(now)
                    .build());
        }

        popularMenuRepository.saveAll(snapshots);
        log.info("[PopularMenuScheduler] Write-back 완료 - snapshotAt: {}, count: {}",
                now, snapshots.size());
    }
}
