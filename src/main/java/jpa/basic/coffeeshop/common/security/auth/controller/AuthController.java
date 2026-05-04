package jpa.basic.coffeeshop.common.security.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jpa.basic.coffeeshop.common.ApiResponse;
import jpa.basic.coffeeshop.common.security.auth.dto.request.CreateUserRequest;
import jpa.basic.coffeeshop.common.security.auth.dto.request.LoginUserRequest;
import jpa.basic.coffeeshop.common.security.auth.service.AuthService;
import jpa.basic.coffeeshop.domain.user.service.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserQueryService userQueryService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(
            @RequestBody @Valid CreateUserRequest request
    ) {
        authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Void>> login(
            @RequestBody @Valid LoginUserRequest request,
            HttpServletResponse response
    ) {
        authService.login(request, response);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletResponse response
    ) {
        authService.logout(response);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK));
    }

    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<Void>> reissue(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        authService.reissue(request, response);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK));
    }
}
