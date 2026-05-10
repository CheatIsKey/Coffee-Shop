package jpa.basic.coffeeshop.domain.popular.controller;

import jpa.basic.coffeeshop.common.ApiResponse;
import jpa.basic.coffeeshop.domain.popular.dto.response.PopularMenuResponse;
import jpa.basic.coffeeshop.domain.popular.service.PopularMenuQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 인기 메뉴 API 컨트롤러
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/menus/popular")
public class PopularMenuController {

    private final PopularMenuQueryService popularMenuQueryService;

    /**
     * 인기 메뉴 Top3 조회
     *
     * Redis -> DB Fallback 순으로 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<PopularMenuResponse>>> getPopularMenus() {
        List<PopularMenuResponse> response = popularMenuQueryService.getPopularMenus();
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response));
    }
}
