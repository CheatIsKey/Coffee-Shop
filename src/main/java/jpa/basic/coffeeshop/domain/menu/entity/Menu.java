package jpa.basic.coffeeshop.domain.menu.entity;

import jakarta.persistence.*;
import jpa.basic.coffeeshop.common.exception.CustomException;
import jpa.basic.coffeeshop.common.exception.ErrorCode;
import jpa.basic.coffeeshop.domain.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "menus")
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

    @Builder
    public Menu(String menuName, int menuPrice, int menuStock, Category category) {
        this.menuName = menuName;
        this.menuPrice = menuPrice;
        this.menuStock = menuStock;
        this.category = category;
        this.menuStatus = MenuStatus.ON_SALE;
    }

    public void decreaseStock(int quantity) {
        validQuantity(quantity);
        verifyStock(quantity);
        this.menuStock -= quantity;
        closeMenu();
    }

    public void increaseStock(int quantity) {
        validQuantity(quantity);
        this.menuStock += quantity;
        resumeMenu();
    }

    public void checkAvailability(int quantity) {
        validQuantity(quantity);
        verifyStock(quantity);
    }

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
