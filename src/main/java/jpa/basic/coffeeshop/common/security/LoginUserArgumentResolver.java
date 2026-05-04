package jpa.basic.coffeeshop.common.security;

import groovyjarjarantlr4.v4.runtime.misc.Nullable;
import jpa.basic.coffeeshop.common.exception.CustomException;
import jpa.basic.coffeeshop.common.exception.ErrorCode;
import jpa.basic.coffeeshop.common.security.auth.LoginUser;
import jpa.basic.coffeeshop.common.security.auth.LoginUserInfo;
import lombok.NonNull;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(@NonNull MethodParameter parameter) {
        boolean hasAnnotation   = parameter.hasParameterAnnotation(LoginUser.class);
        // 수정 포인트: LoginUserInfo를 프로젝트 실제 인증 DTO 타입으로 교체
        boolean hasLoginUserType = LoginUserInfo.class.isAssignableFrom(parameter.getParameterType());
        return hasAnnotation && hasLoginUserType;
    }

    @Override
    public Object resolveArgument(@NonNull MethodParameter parameter,
                                  @Nullable ModelAndViewContainer mavContainer,
                                  @NonNull NativeWebRequest webRequest,
                                  @Nullable WebDataBinderFactory binderFactory) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 인증 정보가 없거나 비로그인(anonymous) 상태이면 즉시 예외 발생
        if (authentication == null
                || authentication.getPrincipal().equals("anonymousUser")
                || !(authentication.getPrincipal() instanceof LoginUserInfo)) {
            throw new CustomException(ErrorCode.AUTH_FORBIDDEN_ACCESS);
        }

        // JwtAuthenticationFilter에서 Principal에 저장한 LoginUserInfo 객체를 그대로 반환
        return authentication.getPrincipal();
    }
}