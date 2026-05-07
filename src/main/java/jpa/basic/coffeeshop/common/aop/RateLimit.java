package jpa.basic.coffeeshop.common.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Redis 기반 요청 속도 제한 어노테이션
 *
 * 적용 대상:
 * - POST /api/points/charge : 동일 사용자 1초 내 3회 초과 시 429
 * - POST /api/orders        : 동일 사용자 1초 내 3회 초과 시 429
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * Redis 키를 결정하는 SpEL 표현식
     * 예) "'rate:point:charge:' + #loginUser.id"
     */
    String key();

    /**
     * 제한 시간(seconds) 내 허용 최대 요청 횟수
     * 기본값: 3
     */
    int limit() default 3;

    /**
     * 요청 횟수를 측정하는 시간 윈도우 (초)
     * 기본값: 1
     */
    int seconds() default 1;
}