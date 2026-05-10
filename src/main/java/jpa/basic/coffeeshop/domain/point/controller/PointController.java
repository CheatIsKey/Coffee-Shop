package jpa.basic.coffeeshop.domain.point.controller;

import jakarta.validation.Valid;
import jpa.basic.coffeeshop.common.ApiResponse;
import jpa.basic.coffeeshop.common.aop.RateLimit;
import jpa.basic.coffeeshop.common.security.auth.LoginUser;
import jpa.basic.coffeeshop.common.security.auth.LoginUserInfo;
import jpa.basic.coffeeshop.domain.point.dto.request.ChargePointRequest;
import jpa.basic.coffeeshop.domain.point.dto.response.ChargePointResponse;
import jpa.basic.coffeeshop.domain.point.dto.response.PointMeResponse;
import jpa.basic.coffeeshop.domain.point.service.PointCommandService;
import jpa.basic.coffeeshop.domain.point.service.PointQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 포인트 API 컨트롤러
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/points")
public class PointController {

    private final PointCommandService pointCommandService;
    private final PointQueryService pointQueryService;

    /**
     * 포인트 충전 API
     *
     * @RateLimit: 동일 사용자가 1초 내 3회 초과 시 429 TOO_MANY_REQUESTS 반환
     * SpEL 표현식 '#loginUser.id'는 @LoginUser로 주입된 LoginUserInfo 파라미터를 참조한다.
     */
    @RateLimit(key = "'rate:point:charge:' + #loginUser.id", limit = 3, seconds = 1)
    @PostMapping("/charge")
    public ResponseEntity<ApiResponse<ChargePointResponse>> chargePoint(
            @LoginUser LoginUserInfo loginUser,
            @RequestBody @Valid ChargePointRequest request
    ) {
        ChargePointResponse response = pointCommandService.chargePoint(loginUser.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, response));
    }

    /**
     * 포인트 조회 API
     *
     * cursorId가 null이면 가장 최신 데이터부터 조회
     * 이전 응답의 nextCursorId를 cursorId로 넘겨 다음 페이지를 요청한다.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PointMeResponse>> getMyPoint(
            @LoginUser LoginUserInfo loginUser,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "10") int size
    ) {
        PointMeResponse response = pointQueryService.getMyPoint(loginUser.id(), cursorId, size);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response));
    }
}
