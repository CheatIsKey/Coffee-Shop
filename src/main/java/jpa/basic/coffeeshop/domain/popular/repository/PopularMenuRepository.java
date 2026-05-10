package jpa.basic.coffeeshop.domain.popular.repository;

import jpa.basic.coffeeshop.domain.popular.entity.PopularMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PopularMenuRepository extends JpaRepository<PopularMenu, Long> {

    /**
     * 가장 최근 스냅샷의 Top3를 rank 오름차순으로 조회한다.
     * Redis 장애 시 Fallback 및 Warm-up에 사용된다.
     */
    @Query("""
           SELECT p
           FROM PopularMenu p
           WHERE p.snapshotAt = (
                                   SELECT MAX(p2.snapshotAt)
                                   FROM PopularMenu p2
                                 )
           ORDER BY p.rank ASC
           """)
    List<PopularMenu> findLatestTop3();
}
