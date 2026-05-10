package jpa.basic.coffeeshop.domain.popular.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * 인기 메뉴 Redis ZSET 서비스
 *
 * Redis 키 설계:
 *  - "popular:menu:daily:{yyyyMMdd}" 일별 ZSET
 *  - TTL: 8일 (7일 데이터 + 1일 여유)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PopularMenuRedisService {

    private final StringRedisTemplate redisTemplate;

    private static final String DAILY_KEY_PREFIX = "popular:menu:daily:";
    private static final String TEMP_KEY_PREFIX = "popular:menu:7day:temp:";
    private static final long KEY_TTL_DAYS = 8L;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 주문된 메뉴의 점수를 당일 ZSET에 누적한다.
     * Kafka Consumer가 order.created 이벤트 수신 시 호출한다.
     *
     * ZINCRBY key increment member:
     *  - 해당 member(menuId)의 score를 quantity만큼 증가시킨다.
     *  - 키가 없으면 자동 생성된다.
     */
    public void incrementMenuScore(Long menuId, int quantity) {
        String todayKey = DAILY_KEY_PREFIX + LocalDate.now().format(DATE_FORMATTER);
        redisTemplate.opsForZSet().incrementScore(todayKey, String.valueOf(menuId), quantity);
        // 키가 새로 생성될 때마다 TTL을 설정한다. 기존 키라도 매번 갱신하여 TTL 누락을 방지
        redisTemplate.expire(todayKey, KEY_TTL_DAYS, TimeUnit.DAYS);
        log.debug("[Redis] ZINCRBY {} {} {}", todayKey, quantity, menuId);
    }

    /**
     * 최근 7일 ZSET을 합산하여 Top3를 반환한다.
     * ZUNIONSTORE 동작:
     *  1. 최근 7일 키를 임시 키에 합산 저장
     *  2. 임시 키에서 score 내림차순 Top3 조회
     *  3. 임시 키 삭제
     *
     * 임시 키에 UUID를 사용하는 이유:
     *  - 다수 인스턴스가 동시에 ZUNIONSTORE를 실행할 때 서로 다른 임시 키를 쓰므로 충돌이 없다.
     */
    public List<ZSetOperations.TypedTuple<String>> getTop3() {
        List<String> last7DayKeys = getLastDayKeys();
        String tempKey = TEMP_KEY_PREFIX + UUID.randomUUID();

        try {
            // 7일 일별 키를 합산 -> 임시 키에 저장
            Long unionCount = redisTemplate.opsForZSet().unionAndStore(
                    last7DayKeys.get(0),
                    last7DayKeys.subList(1, last7DayKeys.size()),
                    tempKey
            );

            if (unionCount == null || unionCount == 0) {
                return Collections.emptyList();
            }

            Set<ZSetOperations.TypedTuple<String>> result =
                    redisTemplate.opsForZSet().reverseRangeWithScores(tempKey, 0, 2);

            return result != null ? new ArrayList<>(result) : Collections.emptyList();
        } finally {
            // 임시 키는 반드시 삭제한다.
            redisTemplate.delete(tempKey);
        }
    }

    /**
     * Warm-up: DB 스냅샷 데이터를 Redis ZSET에 복원한다.
     * 애플리케이션 시작 시 PopularMenuScheduler가 호출한다.
     */
    public void warmUpDailyKey(String dateKey, Map<Long, Long> menuQuantityMap) {
        String key = DAILY_KEY_PREFIX + dateKey;
        menuQuantityMap.forEach((menuId, quantity) -> {
            redisTemplate.opsForZSet().add(key, String.valueOf(menuId), quantity);
        });
        redisTemplate.expire(key, KEY_TTL_DAYS, TimeUnit.DAYS);
        log.info("[Redis Warm-up] key: {}, menuCount: {}", key, menuQuantityMap.size());
    }

    /**
     * 최근 7일의 일별 ZSET 키 목록을 생성한다.
     * 오늘부터 역순으로 7개를 반환
     */
    private List<String> getLastDayKeys() {
        return IntStream.range(0, 7)
                .mapToObj(i -> DAILY_KEY_PREFIX + LocalDate.now().minusDays(i).format(DATE_FORMATTER))
                .toList();
    }
}
