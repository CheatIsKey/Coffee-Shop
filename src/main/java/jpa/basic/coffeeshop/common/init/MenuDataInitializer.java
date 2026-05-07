package jpa.basic.coffeeshop.common.init;

import jakarta.annotation.PostConstruct;
import jpa.basic.coffeeshop.domain.menu.entity.Category;
import jpa.basic.coffeeshop.domain.menu.entity.Menu;
import jpa.basic.coffeeshop.domain.menu.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 로컬 개발 환경 전용 메뉴 Mock 데이터 초기화 컴포넌트
 * 애플리케이션 시작 시 5개 Mock 메뉴를 자동 등록
 *
 * @Profile("local"): 로컬 환경에서만 동작
 * 멱등성 보장: 메뉴가 이미 1건 이상 존재하면 초기화를 스킵하여 중복 등록을 방지
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class MenuDataInitializer {

    private final MenuRepository menuRepository;

    @PostConstruct
    @Transactional
    public void init() {
        if (menuRepository.count() > 0) {
            log.info("[MenuDataInitializer] 메뉴 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
            return;
        }

        List<Menu> mockMenus = List.of(
                Menu.builder()
                        .menuName("아메리카노")
                        .menuPrice(4_500)
                        .menuStock(1_000)
                        .category(Category.HOT_DRINK)
                        .build(),

                Menu.builder()
                        .menuName("딸기 라떼")
                        .menuPrice(6_500)
                        .menuStock(50)
                        .category(Category.COLD_DRINK)
                        .build(),

                Menu.builder()
                        .menuName("바스크 치즈 케이크")
                        .menuPrice(7_900)
                        .menuStock(10)
                        .category(Category.DESSERT)
                        .build(),

                Menu.builder()
                        .menuName("디카페인 오트 라떼")
                        .menuPrice(5_800)
                        .menuStock(200)
                        .category(Category.CUSTOM_DRINK)
                        .build(),

                Menu.builder()
                        .menuName("1.5L 대용량 아이스티")
                        .menuPrice(3_500)
                        .menuStock(5)
                        .category(Category.LIMITED_ITEM)
                        .build()
        );

        menuRepository.saveAll(mockMenus);
        log.info("[MenuDataInitializer] Mock 메뉴 {}개 등록 완료", mockMenus.size());
    }
}
