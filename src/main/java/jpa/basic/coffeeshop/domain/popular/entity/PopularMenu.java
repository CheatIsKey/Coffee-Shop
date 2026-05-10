package jpa.basic.coffeeshop.domain.popular.entity;

import jakarta.persistence.*;
import jpa.basic.coffeeshop.domain.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 인기 메뉴 스냅샷 엔터티
 *
 * 역할:
 * - PopularMenuScheduler가 1시간마다 Redis ZSET Top3를 DB에 기록한 스냅샷이다.
 * - Redis 장애 시 이 테이블을 Fallback으로 사용한다.
 * - Redis 재시작 시 가장 최근 snapshot_at의 레코드로 Warm-up한다.
 *
 * 스냅샷 방식:
 * - 매 시간 새 레코드 3개(1-3등)를 INSERT한다.
 * - 과거 이력이 모두 남으므로 시간대별 인기 메뉴 추이 분석이 가능하다.
 */
@Getter
@Entity
@Table(
        name = "popular_menus",
        indexes = {
                // Fallback 조회 시 최신 snapshot_at을 빠르게 찾기 위한 인덱스
                @Index(name = "idx_popular_menus_snapshot_at", columnList = "snapshot_at DESC")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PopularMenu extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "menu_id")
    private Long menuId;

    @Column(nullable = false, length = 100, name = "menu_name")
    private String menuName;

    @Column(nullable = false, name = "total_quantity")
    private Long totalQuantity;

    @Column(nullable = false)
    private int rank;

    /**
     * 스케쥴러가 스냅샷을 찍은 시각
     */
    @Column(nullable = false, name = "snapshot_at")
    private LocalDateTime snapshotAt;

    @Builder
    public PopularMenu(Long menuId, String menuName, Long totalQuantity, int rank, LocalDateTime snapshotAt) {
        this.menuId = menuId;
        this.menuName = menuName;
        this.totalQuantity = totalQuantity;
        this.rank = rank;
        this.snapshotAt = snapshotAt;
    }
}
