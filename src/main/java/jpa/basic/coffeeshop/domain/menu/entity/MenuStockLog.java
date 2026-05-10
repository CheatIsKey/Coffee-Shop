package jpa.basic.coffeeshop.domain.menu.entity;

import jakarta.persistence.*;
import jpa.basic.coffeeshop.domain.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 메뉴 재고 변동 이력 엔터티
 *
 * 재고 변동이 발생할 때마다 기록
 * changeStock: 증가 / 차감
 * remainStock: 변동 후 최종 재고 스냅샷
 */
@Getter
@Entity
@Table(name = "menu_stock_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MenuStockLog extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, name = "menu_id")
    private Long menuId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(nullable = false, name = "change_stock")
    private int changeStock;

    @Column(nullable = false, name = "remain_stock")
    private int remainStock;

    @Builder
    public MenuStockLog(Long menuId, Long orderId, int changeStock, int remainStock) {
        this.menuId = menuId;
        this.orderId = orderId;
        this.changeStock = changeStock;
        this.remainStock = remainStock;
    }
}
