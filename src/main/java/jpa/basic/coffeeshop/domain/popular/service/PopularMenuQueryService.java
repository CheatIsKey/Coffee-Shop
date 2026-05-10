package jpa.basic.coffeeshop.domain.popular.service;

import jpa.basic.coffeeshop.domain.popular.dto.response.PopularMenuResponse;

import java.util.List;

public interface PopularMenuQueryService {

    /**
     * 최근 7일 인기 메뉴 Top3를 조회한다.
     * Redis ZSET에서 우선 조회하고 실패 시 PopularMenu 테이블로 Fallback 한다.
     */
    List<PopularMenuResponse> getPopularMenus();
}
