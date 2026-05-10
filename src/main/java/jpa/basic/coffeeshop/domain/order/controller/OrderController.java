package jpa.basic.coffeeshop.domain.order.controller;

import jakarta.validation.Valid;
import jpa.basic.coffeeshop.common.ApiResponse;
import jpa.basic.coffeeshop.common.CursorResponse;
import jpa.basic.coffeeshop.common.aop.RateLimit;
import jpa.basic.coffeeshop.common.security.auth.LoginUser;
import jpa.basic.coffeeshop.common.security.auth.LoginUserInfo;
import jpa.basic.coffeeshop.domain.order.dto.request.AdminOrderSearchRequest;
import jpa.basic.coffeeshop.domain.order.dto.request.CreateOrderRequest;
import jpa.basic.coffeeshop.domain.order.dto.response.CreateOrderResponse;
import jpa.basic.coffeeshop.domain.order.dto.response.OrderDetailResponse;
import jpa.basic.coffeeshop.domain.order.dto.response.OrderListItemResponse;
import jpa.basic.coffeeshop.domain.order.facade.OrderCreateFacade;
import jpa.basic.coffeeshop.domain.order.service.OrderCommandService;
import jpa.basic.coffeeshop.domain.order.service.OrderQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 주문 API 컨트롤러
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderCreateFacade  orderCreateFacade;
    private final OrderCommandService orderCommandService;
    private final OrderQueryService orderQueryService;

    /**
     * 주문 생성 + 결제
     *
     * @RateLimit: 동일 사용자 1초 내 3회 초과 시 429 에러
     */
    @RateLimit(key = "'rate:order:create:' + #loginUser.id", limit = 3, seconds = 1)
    @PostMapping
    public ResponseEntity<ApiResponse<CreateOrderResponse>> createOrder(
            @LoginUser LoginUserInfo loginUser,
            @RequestBody @Valid CreateOrderRequest request
    ) {
        CreateOrderResponse response = orderCreateFacade.createOrder(loginUser.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, response));
    }

    /**
     * 주문 목록 조회 (일반 사용자 - 본인 주문, 커서 기반)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<CursorResponse<OrderListItemResponse>>> getMyOrders(
            @LoginUser LoginUserInfo loginUser,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "10") int size
    ) {
        CursorResponse<OrderListItemResponse> response =
                orderQueryService.getMyOrders(loginUser.id(), cursorId, size);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response));
    }

    /**
     * 주문 목록 조회 (관리자 - 전체 주문, 동적 검색 + Offset 페이징)
     */
    @GetMapping("/admin")
    public ResponseEntity<ApiResponse<Page<OrderListItemResponse>>> getAdminOrders(
            @LoginUser LoginUserInfo loginUser,
            @ModelAttribute AdminOrderSearchRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<OrderListItemResponse> response =
                orderQueryService.getAdminOrders(request, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response));
    }

    /**
     * 주문 상세 조회
     */
    @GetMapping("/{orderUid}")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> getOrderDetail(
            @LoginUser LoginUserInfo loginUser,
            @PathVariable String orderUid
    ) {
        boolean isAdmin = loginUser.role().equals("ADMIN");
        OrderDetailResponse response =
                orderQueryService.getOrderDetail(loginUser.id(), orderUid, isAdmin);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response));
    }

    /**
     * 주문 삭제
     */
    @DeleteMapping("/{orderUid}")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(
            @LoginUser LoginUserInfo loginUser,
            @PathVariable String orderUid
    ) {
        boolean isAdmin = loginUser.role().equals("ADMIN");
        orderCommandService.deleteOrder(loginUser.id(), orderUid, isAdmin);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success(HttpStatus.NO_CONTENT));
    }
}
