package jpa.basic.coffeeshop.domain.menu.controller;

import jakarta.validation.Valid;
import jpa.basic.coffeeshop.common.ApiResponse;
import jpa.basic.coffeeshop.common.security.auth.LoginUser;
import jpa.basic.coffeeshop.common.security.auth.LoginUserInfo;
import jpa.basic.coffeeshop.domain.menu.dto.request.CreateMenuRequest;
import jpa.basic.coffeeshop.domain.menu.dto.request.MenuPageRequest;
import jpa.basic.coffeeshop.domain.menu.dto.response.MenuDetailResponse;
import jpa.basic.coffeeshop.domain.menu.dto.response.MenuPageResponse;
import jpa.basic.coffeeshop.domain.menu.entity.UpdateMenuRequest;
import jpa.basic.coffeeshop.domain.menu.facade.MenuCreateFacade;
import jpa.basic.coffeeshop.domain.menu.service.MenuCommandService;
import jpa.basic.coffeeshop.domain.menu.service.MenuQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/menus")
public class MenuController {

    private final MenuQueryService menuQueryService;
    private final MenuCommandService menuCommandService;
    private final MenuCreateFacade menuCreateFacade;

    /**
     * 메뉴 생성 (ADMIN 전용)
     *
     * @param loginUser     : JWT에서 추출한 로그인 사용자 정보
     * @param request       : 생성할 메뉴 정보
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createMenu(
            @LoginUser LoginUserInfo loginUser,
            @RequestBody @Valid CreateMenuRequest request
    ) {
        menuCreateFacade.createMenu(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED));
    }

    /**
     * 메뉴 수정 (ADMIN 전용)
     */
    @PutMapping("/{menuId}")
    public ResponseEntity<ApiResponse<Void>> updateMenu(
            @LoginUser LoginUserInfo loginUser,
            @PathVariable Long menuId,
            @RequestBody @Valid UpdateMenuRequest request
    ) {
        menuCommandService.updateMenu(menuId, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK));
    }

    /**
     * 메뉴 삭제 (ADMIN 전용)
     */
    @DeleteMapping("/{menuId}")
    public ResponseEntity<ApiResponse<Void>> deleteMenu(
            @LoginUser LoginUserInfo loginUser,
            @PathVariable Long menuId
    ) {
        menuCommandService.deleteMenu(menuId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success(HttpStatus.OK));
    }

    /**
     * 메뉴 목록 조회 API
     *
     * 미래 메뉴 확장을 고려하여 Offset 기반 페이징을 사용
     */
    @GetMapping
    public ResponseEntity<ApiResponse<MenuPageResponse>> getMenus(
            @ModelAttribute @Valid MenuPageRequest request
    ) {
        MenuPageResponse response = menuQueryService.getMenus(request.getPage(), request.getSize());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response));
    }

    /**
     * 메뉴 상세 조회 API
     *
     * Soft Delete로 삭제된 메뉴는 조회 결과에서 제외
     */
    @GetMapping("/{menuId}")
    public ResponseEntity<ApiResponse<MenuDetailResponse>> getMenuById(
            @PathVariable Long menuId
    ) {
        MenuDetailResponse response = menuQueryService.getMenuById(menuId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response));
    }
}
