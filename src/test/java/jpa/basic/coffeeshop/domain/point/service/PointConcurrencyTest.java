package jpa.basic.coffeeshop.domain.point.service;

import jpa.basic.coffeeshop.domain.point.dto.request.ChargePointRequest;
import jpa.basic.coffeeshop.domain.point.entity.PaymentType;
import jpa.basic.coffeeshop.domain.user.entity.User;
import jpa.basic.coffeeshop.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles({"local", "test"})
class PointConcurrencyTest {

    @Autowired private PointCommandService         pointCommandService;
    @Autowired private UserRepository              userRepository;
    @Autowired private PlatformTransactionManager  transactionManager;

    private User testUser;

    @BeforeEach
    void setUp() {
        // UUID로 매 테스트마다 고유한 이메일 생성
        // 이유: @BeforeEach는 테스트 메서드마다 실행되는데,
        //       파일 기반 H2 DB에 이전 데이터가 남아있으면 이메일 UNIQUE 제약을 위반한다.
        //       UUID를 사용하면 테스트 실행마다 항상 새로운 이메일을 보장한다.
        String uniqueEmail = "test-" + UUID.randomUUID() + "@test.com";
        testUser = userRepository.save(User.createUser(uniqueEmail, "encoded-password"));
    }

    @Test
    @DisplayName("동일 사용자가 동시에 10번 충전해도 최종 잔액이 정확해야 한다")
    void concurrentChargePoint_shouldBeConsistent() throws InterruptedException {
        int   threadCount    = 10;
        long  chargeAmount   = 1_000L;
        long  expectedTotal  = chargeAmount * threadCount;

        ExecutorService executor  = Executors.newFixedThreadPool(threadCount);
        CountDownLatch  ready     = new CountDownLatch(threadCount);
        CountDownLatch  start     = new CountDownLatch(1);
        CountDownLatch  done      = new CountDownLatch(threadCount);
        AtomicInteger   successCount = new AtomicInteger(0);
        AtomicInteger   failCount    = new AtomicInteger(0);

        ChargePointRequest request = new ChargePointRequest(chargeAmount, PaymentType.CARD);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    ready.countDown();
                    start.await();
                    pointCommandService.chargePoint(testUser.getId(), request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        start.countDown();
        done.await();
        executor.shutdown();

        User result = userRepository.findById(testUser.getId()).orElseThrow();

        System.out.println("=== 포인트 동시 충전 테스트 결과 ===");
        System.out.println("성공: " + successCount.get() + "건");
        System.out.println("실패: " + failCount.get() + "건");
        System.out.println("기대 잔액: " + expectedTotal);
        System.out.println("실제 잔액: " + result.getPoint());

        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(failCount.get()).isZero();
        assertThat(result.getPoint()).isEqualTo(expectedTotal);
    }

    @Test
    @DisplayName("잔액보다 큰 금액을 동시에 차감 요청하면 하나만 성공하고 나머지는 실패해야 한다")
    void concurrentSpendPoint_onlyOneShouldSucceed() throws InterruptedException {
        // given: 1000원 충전
        long initialPoint = 1_000L;
        pointCommandService.chargePoint(testUser.getId(),
                new ChargePointRequest(initialPoint, PaymentType.CARD));

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch  ready    = new CountDownLatch(threadCount);
        CountDownLatch  start    = new CountDownLatch(1);
        CountDownLatch  done     = new CountDownLatch(threadCount);
        AtomicInteger   success  = new AtomicInteger(0);
        AtomicInteger   fail     = new AtomicInteger(0);

        // TransactionTemplate 사용
        // 이유: 별도 스레드는 Spring 트랜잭션 컨텍스트가 없다.
        //      findByIdWithLock()은 PESSIMISTIC_WRITE 락을 사용하는데,
        //      트랜잭션 없이 호출하면 락이 동작하지 않아 정합성 검증이 의미없어진다.
        //      TransactionTemplate으로 각 스레드가 독립적인 트랜잭션을 가지게 한다.
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    ready.countDown();
                    start.await();
                    txTemplate.execute(status -> {
                        User user = userRepository.findByIdWithLock(testUser.getId())
                                .orElseThrow();
                        user.spendPoint(initialPoint); // 잔액 부족 시 예외 발생
                        return null;
                    });
                    success.incrementAndGet();
                } catch (Exception e) {
                    fail.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        start.countDown();
        done.await();
        executor.shutdown();

        User result = userRepository.findById(testUser.getId()).orElseThrow();

        System.out.println("=== 포인트 동시 차감 테스트 결과 ===");
        System.out.println("성공: " + success.get() + "건");
        System.out.println("실패: " + fail.get() + "건");
        System.out.println("최종 잔액: " + result.getPoint());

        assertThat(success.get()).isEqualTo(1);
        assertThat(result.getPoint()).isZero();
        assertThat(result.getPoint()).isGreaterThanOrEqualTo(0);
    }
}