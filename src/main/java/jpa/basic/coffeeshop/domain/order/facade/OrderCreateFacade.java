package jpa.basic.coffeeshop.domain.order.facade;

import jpa.basic.coffeeshop.common.exception.CustomException;
import jpa.basic.coffeeshop.common.exception.ErrorCode;
import jpa.basic.coffeeshop.domain.order.dto.request.CreateOrderRequest;
import jpa.basic.coffeeshop.domain.order.dto.response.CreateOrderResponse;
import jpa.basic.coffeeshop.domain.order.service.OrderCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 주문 생성 분산 락 Facade
 *
 * Redis 락 획득 -> OrderCommandService 트랜잭션 (커밋까지 완료) -> Redis 락 해제
 * 락 키: "lock:order:{userId}"
 * 사용자 단위로 락을 걸어 동일 사용자의 동시 중복 주문을 차단
 * 서로 다른 사용자의 주문은 락 키가 다르므로 동시 처리된다.
 *
 * 주의: 이 클래스에 @Transactional을 붙이면 내부 서비스의 커밋이 이 메서드 종료 후로 미뤄져
 *      락 해제 후 커밋되는 문제가 발생한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreateFacade {

    private final OrderCommandService orderCommandService;
    private final RedissonClient redissonClient;

    private static final String LOCK_PREFIX = "lock:order:";
    private static final long LOCK_WAIT_TIME = 3L;
    private static final long LOCK_LEASE_TIME = 5L;

    public CreateOrderResponse createOrder(Long userId, CreateOrderRequest request) {
        String lockKey = LOCK_PREFIX + userId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("[OrderCreateFacade] 분산 락 획득 실패 - lockKey: {}", lockKey);
                throw new CustomException(ErrorCode.TOO_MANY_REQUESTS);
            }
            log.debug("[OrderCreateFacade] 분산 락 획득 - lockKey: {}", lockKey);

            // 트랜잭션 시작 -> 비즈니스 로직 -> 커밋 완료
            return orderCommandService.createOrder(userId, request);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CustomException(ErrorCode.DATABASE_CONNECTION_ERROR);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("[OrderCreateFacade] 분산 락 해제 - lockKey: {}", lockKey);
            }
        }
    }
}
