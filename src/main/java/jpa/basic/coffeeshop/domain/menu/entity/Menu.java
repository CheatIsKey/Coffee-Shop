package jpa.basic.coffeeshop.domain.menu.entity;

import jakarta.persistence.*;
import jpa.basic.coffeeshop.common.exception.CustomException;
import jpa.basic.coffeeshop.common.exception.ErrorCode;
import jpa.basic.coffeeshop.domain.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "menus",
        /**
         * 메뉴 생성/삭제의 멱등성 보장을 위한 복합 유니크 제약
         * 삭제된 메뉴와 새 메뉴가 서로 다른 유니크 레코드로 취급될 수 있도록 deleted_at 추가
        */
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_menus_name_category_deleted",
                        columnNames = {"menu_name", "category", "deleted_at"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Menu extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String menuName;

    @Column(nullable = false)
    private int menuPrice;

    @Column(nullable = false)
    private int menuStock;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Category category;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MenuStatus menuStatus;

    /**
     * 낙관적 락
     *
     * 메뉴 수정 API에서 동시 수정 충돌을 감지하기 위해 @Version 어노테이션을 적용
     * JPA가 UPDATE 시 version 조건을 자동으로 WHERE 절에 추가하여 버전 불일치 감지
     */
    @Version
    @Column(nullable = false)
    private Long version = 0L;

    /**
     * Soft Delete 여부 플래그
     *
     * true -> 삭제된 메뉴로 간주하며, 모든 조회 쿼리에서 제외
     * DB 인덱스를 활용한 빠른 필터링을 위해 별도 컬럼으로 관리
     */
    @Column(nullable = false, name = "is_deleted")
    private boolean isDeleted = false;

    /**
     * Soft Delete 일시
     *  - 삭제 전 기본값  : 1970-01-01 (센티넬 값)
     *  - 삭제 후        : 실제 삭제 요청 일시
     *
     * NULL 대신 센티넬 값을 사용하는 이유:
     *  - MySQL에서 NULL != NULL 이므로, UNIQUE(col, deleted_at) 제약에 NULL을 포함하면
     *    여러 레코드가 동시에 NULL을 가질 수 있어 유니크 제약이 무의미
     *  - 센티넬 값을 사용하면 UNIQUE 제약이 의도대로 동작
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt = LocalDateTime.of(1970, 1, 1, 0, 0, 0);

    @Builder
    public Menu(String menuName, int menuPrice, int menuStock, Category category) {
        this.menuName = menuName;
        this.menuPrice = menuPrice;
        this.menuStock = menuStock;
        this.category = category;
        this.menuStatus = MenuStatus.ON_SALE;
    }

    /**
     * ──────────────────────────────────────────────
     * 재고 관련 도메인 메서드
     * ──────────────────────────────────────────────
     */

    /**
     * 재고를 차감
     * 차감 후 재고가 0이 되면 자동으로 SOLD_OUT 상태로 전환
     */
    public void decreaseStock(int quantity) {
        validQuantity(quantity);
        verifyStock(quantity);
        this.menuStock -= quantity;
        closeMenu();
    }

    /**
     * 재고를 증가
     * SOLD_OUT 상태에서 재고가 1 이상이 되면 자동으로 ON_SALE 상태로 복구
     */
    public void increaseStock(int quantity) {
        validQuantity(quantity);
        this.menuStock += quantity;
        resumeMenu();
    }

    /**
     * 주문 수량에 대한 재고 가용성을 사전 확인
     * 실제 차감 없이 검증만 수행, 주문 유효성 검사 단계에서 사용
     */
    public void checkAvailability(int quantity) {
        validQuantity(quantity);
        verifyStock(quantity);
    }

    /**
     * ──────────────────────────────────────────────
     * 관리자 전용 도메인 메서드
     * ──────────────────────────────────────────────
     */

    /**
     * 메뉴 정보를 수정
     *
     * 낙관적 락으로 version 조건을 자동 검사
     */
    public void update(String menuName, int menuPrice, int menuStock,
                       Category category, MenuStatus menuStatus) {
        this.menuName = menuName;
        this.menuPrice = menuPrice;
        this.menuStock = menuStock;
        this.category = category;
        this.menuStatus = menuStatus;
    }

    /**
     * 메뉴를 Soft Delete 수행
     *
     * is_deleted = true, deleted_at = 현재 시각 설정
     */
    public void softDelete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * ──────────────────────────────────────────────
     * private 내부 검증 메서드
     * ──────────────────────────────────────────────
     */

    private void validQuantity(int quantity) {
        if (quantity <= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private void verifyStock(int quantity) {
        if (this.menuStock < quantity) {
            throw new CustomException(ErrorCode.MENU_OUT_OF_STOCK);
        }
    }

    private void closeMenu() {
        if (this.menuStatus == MenuStatus.SOLD_OUT) {
            return;
        }
        if (this.menuStock == 0) {
            this.menuStatus = MenuStatus.SOLD_OUT;
        }
    }

    private void resumeMenu() {
        if (this.menuStatus == MenuStatus.SOLD_OUT && this.menuStock > 0) {
            this.menuStatus = MenuStatus.ON_SALE;
        }
    }
}
