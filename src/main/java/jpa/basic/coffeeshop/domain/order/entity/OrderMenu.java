package jpa.basic.coffeeshop.domain.order.entity;

import jakarta.persistence.*;
import jpa.basic.coffeeshop.domain.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 메뉴 스냅샷 엔터티
 *
 * 주문 당시의 메뉴 정보(이름, 가격)를 스냅샷으로 저장
 */
@Getter
@Entity
@Table(name = "order_menus")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderMenu extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "menu_id")
    private Long menuId;

    @Column(nullable = false, name = "order_id")
    private Long orderId;

    @Column(nullable = false, length = 100, name = "menu_name")
    private String menuName;

    @Column(nullable = false, name = "menu_price")
    private int menuPrice;

    @Column(nullable = false)
    private int quantity;

    @Builder
    public OrderMenu(Long menuId, Long orderId, String menuName, int menuPrice, int quantity) {
        this.menuId = menuId;
        this.orderId = orderId;
        this.menuName = menuName;
        this.menuPrice = menuPrice;
        this.quantity = quantity;
    }

    public long calculateAmount() {
        return (long) menuPrice * quantity;
    }
}
