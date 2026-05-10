package jpa.basic.coffeeshop.domain.popular.service;

import jpa.basic.coffeeshop.domain.menu.entity.Category;
import jpa.basic.coffeeshop.domain.menu.entity.Menu;
import jpa.basic.coffeeshop.domain.menu.service.MenuQueryService;
import jpa.basic.coffeeshop.domain.popular.dto.response.PopularMenuResponse;
import jpa.basic.coffeeshop.domain.popular.entity.PopularMenu;
import jpa.basic.coffeeshop.domain.popular.redis.PopularMenuRedisService;
import jpa.basic.coffeeshop.domain.popular.repository.PopularMenuRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PopularMenuFallbackTest {

    @InjectMocks private PopularMenuQueryServiceImpl popularMenuQueryService;
    @Mock private PopularMenuRedisService            popularMenuRedisService;
    @Mock private PopularMenuRepository              popularMenuRepository;
    @Mock private MenuQueryService                   menuQueryService;

    @Test
    @DisplayName("Redis가 정상이면 ZSET 결과를 반환한다")
    void getPopularMenus_redisAvailable_returnsRedisData() {
        // 객체를 given() 바깥에서 먼저 생성한다.
        // 이유: given(...).willReturn(List.of(helper())) 처럼 helper()가 given() 안에서 예외를 던지면
        //      Mockito 내부 stubbing 상태가 오염되어 이후 given() 호출에서 UnfinishedStubbingException 발생.
        // 항상 stubbing 대상 객체를 먼저 만들고 그 참조를 willReturn()에 넣는다.
        var tuple  = mockTuple("1", 100.0);
        Menu menu  = mockMenu(1L, "아메리카노");  // (수정) given() 바깥에서 미리 생성

        given(popularMenuRedisService.getTop3()).willReturn(List.of(tuple));
        given(menuQueryService.getAllMenusById(any())).willReturn(List.of(menu)); // 미리 만든 객체 사용

        // when
        List<PopularMenuResponse> result = popularMenuQueryService.getPopularMenus();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).menuName()).isEqualTo("아메리카노");
        assertThat(result.get(0).totalQuantity()).isEqualTo(100L);
        verify(popularMenuRepository, never()).findLatestTop3();
    }

    @Test
    @DisplayName("Redis 장애 시 DB Fallback으로 인기 메뉴를 반환한다")
    void getPopularMenus_redisFails_fallbackToDb() {
        given(popularMenuRedisService.getTop3()).willThrow(new RuntimeException("Redis 연결 실패"));

        List<PopularMenu> dbFallback = List.of(
                buildPopularMenu(1L, "아메리카노", 500L, 1),
                buildPopularMenu(2L, "딸기라떼",  300L, 2),
                buildPopularMenu(3L, "치즈케이크", 200L, 3)
        );
        given(popularMenuRepository.findLatestTop3()).willReturn(dbFallback);

        // when
        List<PopularMenuResponse> result = popularMenuQueryService.getPopularMenus();

        // then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).rank()).isEqualTo(1);
        assertThat(result.get(0).menuName()).isEqualTo("아메리카노");
        assertThat(result.get(1).menuName()).isEqualTo("딸기라떼");
        verify(popularMenuRepository).findLatestTop3();
    }

    @Test
    @DisplayName("Redis ZSET이 비어있으면 DB Fallback으로 조회한다")
    void getPopularMenus_redisEmpty_fallbackToDb() {
        given(popularMenuRedisService.getTop3()).willReturn(Collections.emptyList());
        given(popularMenuRepository.findLatestTop3()).willReturn(List.of(
                buildPopularMenu(1L, "아메리카노", 100L, 1)
        ));

        // when
        List<PopularMenuResponse> result = popularMenuQueryService.getPopularMenus();

        // then
        assertThat(result).hasSize(1);
        verify(popularMenuRepository).findLatestTop3();
    }

    // ──────────────────────────────────────────────────────────────
    // 테스트 헬퍼 메서드
    // ──────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private ZSetOperations.TypedTuple<String> mockTuple(String value, double score) {
        var tuple = mock(ZSetOperations.TypedTuple.class);
        given(tuple.getValue()).willReturn(value);
        given(tuple.getScore()).willReturn(score);
        return tuple;
    }

    private Menu mockMenu(Long id, String name) {
        Menu menu = Menu.builder()
                .menuName(name)
                .menuPrice(4_500)
                .menuStock(100)
                .category(Category.HOT_DRINK)
                .build();
        // ReflectionTestUtils.setField 사용
        // 이유: Menu.id는 private이고 @GeneratedValue라 생성자로 주입 불가.
        //      ReflectionTestUtils는 클래스 계층 전체를 탐색하므로 BaseEntity 포함 어디에 있어도 찾는다.
        ReflectionTestUtils.setField(menu, "id", id);
        return menu;
    }

    private PopularMenu buildPopularMenu(Long menuId, String menuName,
                                         Long totalQuantity, int rank) {
        return PopularMenu.builder()
                .menuId(menuId).menuName(menuName)
                .totalQuantity(totalQuantity).rank(rank)
                .snapshotAt(LocalDateTime.now())
                .build();
    }
}