package jpa.basic.coffeeshop.domain.menu.service;

import jpa.basic.coffeeshop.domain.menu.entity.Category;
import jpa.basic.coffeeshop.domain.menu.entity.Menu;
import jpa.basic.coffeeshop.domain.menu.repository.MenuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 재고 차감 동시성 테스트
 *
 * 검증 목표:
 * N명이 동시에 동일 메뉴를 1개씩 주문할 때,
 * 비관적 락으로 재고가 정확하게 차감되어 음수가 되지 않는지 검증한다.
 *
 * 시나리오: 재고 5개 메뉴에 10명이 동시에 주문
 * 기대 결과: 5건 성공, 5건 실패(품절), 최종 재고 0
 */
@SpringBootTest
@ActiveProfiles({"local", "test"})
class MenuStockConcurrencyTest {

    @Autowired private MenuCommandService menuCommandService;
    @Autowired private MenuRepository     menuRepository;

    private Menu testMenu;
    private static final int INITIAL_STOCK = 5;

    @BeforeEach
    void setUp() {
        // UUID로 고유한 메뉴명 생성 — 메뉴명+카테고리 UNIQUE 제약 충돌 방지
        testMenu = menuRepository.save(Menu.builder()
                .menuName("동시성테스트-" + UUID.randomUUID())
                .menuPrice(5_000)
                .menuStock(INITIAL_STOCK)
                .category(Category.COLD_DRINK)
                .build());
    }

    @Test
    @DisplayName("재고 5개인 메뉴에 10명이 동시 주문하면 5건만 성공하고 재고는 0이어야 한다")
    void concurrentStockDecrease_shouldNotGoNegative() throws InterruptedException {
        // given
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch  ready    = new CountDownLatch(threadCount);
        CountDownLatch  start    = new CountDownLatch(1);
        CountDownLatch  done     = new CountDownLatch(threadCount);
        AtomicInteger   success  = new AtomicInteger(0);
        AtomicInteger   fail     = new AtomicInteger(0);

        // 가상의 orderId (실제 주문 없이 재고 차감만 테스트)
        Long fakeOrderId = 9999L;

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    ready.countDown();
                    start.await();
                    menuCommandService.decreaseStockWithLog(testMenu.getId(), 1, fakeOrderId);
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

        // then
        Menu updatedMenu = menuRepository.findById(testMenu.getId()).orElseThrow();

        System.out.println("=== 재고 차감 동시성 테스트 결과 ===");
        System.out.println("초기 재고: " + INITIAL_STOCK);
        System.out.println("동시 요청: " + threadCount + "건");
        System.out.println("성공: " + success.get() + "건");
        System.out.println("실패(품절): " + fail.get() + "건");
        System.out.println("최종 재고: " + updatedMenu.getMenuStock());

        assertThat(success.get()).isEqualTo(INITIAL_STOCK);           // 재고만큼만 성공
        assertThat(fail.get()).isEqualTo(threadCount - INITIAL_STOCK); // 나머지는 실패
        assertThat(updatedMenu.getMenuStock()).isZero();               // 재고 0 (음수 없음)
        assertThat(updatedMenu.getMenuStock()).isGreaterThanOrEqualTo(0); // 절대 음수 아님
    }
}