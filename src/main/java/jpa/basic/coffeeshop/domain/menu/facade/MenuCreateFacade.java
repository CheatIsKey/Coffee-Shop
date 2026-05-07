package jpa.basic.coffeeshop.domain.menu.facade;

import jpa.basic.coffeeshop.common.exception.CustomException;
import jpa.basic.coffeeshop.common.exception.ErrorCode;
import jpa.basic.coffeeshop.domain.menu.dto.request.CreateMenuRequest;
import jpa.basic.coffeeshop.domain.menu.service.MenuCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 메뉴 생성 분산 락 Facade
 *
 * 트랜잭션의 커밋 완료 이후에 Redis 락을 해제하기 위해
 * 락 획득/해제 로직을 트랜잭션 바깥으로 분리한 클래스
 *
 * 1. Redis 락 획득
 * 2. 트랜잭션 시작
 * 3. 중복 체크 + 메뉴 저장
 * 4. 트랜잭션 커밋
 * 5. Redis 락 해제
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MenuCreateFacade {

    private final MenuCommandService menuCommandService;
    private final RedissonClient redissonClient;

    /**
     * 락 키 포맷 "lock:menu:create:{category}"
     * 중복 등록 기준이 (메뉴명 + 카테고리)이므로 카테고리 단위로 락을 설정
     */
    private static final String LOCK_PREFIX = "lock:menu:create:";

    /**
     * 락 획득 최대 대기 시간 (초)
     * 이 시간 안에 락을 획득하지 못하면 TOO_MANY_REQUESTS 예외를 반환
     *
     * 부하 테스트(k6)를 통해 측정된 대기 시간을 기준으로 조정하도록 계획
     */
    private static final long LOCK_WAIT_TIME = 3L;

    /**
     * 락 자동 해제 시간 (초)
     * 작업 중 서버 장애로 finally 블록이 실행되지 않더라도 일정 시간 후 락이 자동 해제
     */
    private static final long LOCK_LEASE_TIME = 5L;

    /**
     * 분산 락을 획득한 후 메뉴 생성 서비스를 호출
     */
    public void createMenu(CreateMenuRequest request) {
        String lockKey = LOCK_PREFIX + request.category().name();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);

            if (!acquired) {
                // 대기 시간 내에 락을 획득하지 못한 경우
                log.warn("[MenuCreateFacade] 분산 락 획득 실패 - lockKey: {}", lockKey);
                throw new CustomException(ErrorCode.TOO_MANY_REQUESTS);
            }

            log.debug("[MenuCreateFacade] 분산 락 획득 성공 - lockKey: {}", lockKey);

            menuCommandService.createMenu(request);
        } catch (InterruptedException e) {
            // 락 획득 대기 중 스레드가 외부로부터 인터럽트된 경우
            // 상위 호출자에게 인터럽트 상태를 전파하기 위해 반드시 인터럽트 상태 복원
            Thread.currentThread().interrupt();
            log.error("[MenuCreateFacade] 락 대기 중 인터럽트 발생 - lockKey: {}", lockKey);
            throw new CustomException(ErrorCode.DATABASE_CONNECTION_ERROR);
        } finally {
            // 현재 스레드가 실제로 락을 보유 중인지 체크
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("[MenuCreateFacade] 분산 락 해제 완료 - lockKey: {}", lockKey);
            }
        }
    }
}
