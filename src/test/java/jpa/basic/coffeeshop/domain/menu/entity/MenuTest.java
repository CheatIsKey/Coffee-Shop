package jpa.basic.coffeeshop.domain.menu.entity;

import jpa.basic.coffeeshop.common.exception.CustomException;
import jpa.basic.coffeeshop.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Menu 엔티티 도메인 로직 단위 테스트
 *
 * DB 없이 순수 도메인 메서드만 검증한다.
 * 스프링 컨텍스트 로드 없이 빠르게 실행된다.
 */
class MenuTest {

    private Menu menu;

    @BeforeEach
    void setUp() {
        menu = Menu.builder()
                .menuName("아메리카노")
                .menuPrice(4_500)
                .menuStock(10)
                .category(Category.HOT_DRINK)
                .build();
    }

    @Test
    @DisplayName("재고를 차감하면 재고가 줄어든다")
    void decreaseStock_success() {
        menu.decreaseStock(3);
        assertThat(menu.getMenuStock()).isEqualTo(7);
        assertThat(menu.getMenuStatus()).isEqualTo(MenuStatus.ON_SALE); // 아직 판매 중
    }

    @Test
    @DisplayName("재고를 전부 차감하면 SOLD_OUT으로 자동 전환된다")
    void decreaseStock_toZero_becomesSoldOut() {
        menu.decreaseStock(10);
        assertThat(menu.getMenuStock()).isZero();
        assertThat(menu.getMenuStatus()).isEqualTo(MenuStatus.SOLD_OUT);
    }

    @Test
    @DisplayName("재고보다 많이 차감하면 MENU_OUT_OF_STOCK 예외가 발생한다")
    void decreaseStock_overStock_throwsException() {
        assertThatThrownBy(() -> menu.decreaseStock(11))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.MENU_OUT_OF_STOCK);

        // 예외 발생 후 재고는 변하지 않아야 함
        assertThat(menu.getMenuStock()).isEqualTo(10);
    }

    @Test
    @DisplayName("수량 0 이하로 차감하면 INVALID_INPUT_VALUE 예외가 발생한다")
    void decreaseStock_zeroQuantity_throwsException() {
        assertThatThrownBy(() -> menu.decreaseStock(0))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("SOLD_OUT 상태에서 재고를 추가하면 ON_SALE로 자동 복구된다")
    void increaseStock_fromSoldOut_resumesToOnSale() {
        menu.decreaseStock(10); // SOLD_OUT 전환
        assertThat(menu.getMenuStatus()).isEqualTo(MenuStatus.SOLD_OUT);

        menu.increaseStock(5);  // 재고 추가
        assertThat(menu.getMenuStock()).isEqualTo(5);
        assertThat(menu.getMenuStatus()).isEqualTo(MenuStatus.ON_SALE); // 자동 복구
    }

    @Test
    @DisplayName("소프트 삭제 후 is_deleted=true, deleted_at이 설정된다")
    void softDelete_setsDeletedFields() {
        menu.softDelete();
        assertThat(menu.isDeleted()).isTrue();
        assertThat(menu.getDeletedAt()).isNotEqualTo(
                java.time.LocalDateTime.of(1970, 1, 1, 0, 0, 0)); // 센티넬 값에서 변경됨
    }
}