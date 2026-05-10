package jpa.basic.coffeeshop.common.aop;

import jpa.basic.coffeeshop.common.exception.CustomException;
import jpa.basic.coffeeshop.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * @RateLimit 어노테이션을 처리하는 AOP Aspect
 *
 * 동작 원리:
 * 1. 메서드 파라미터에서 SpEL 표현식을 평가하여 Redis 키를 생성한다.
 * 2. Redis INCR 명령으로 요청 횟수를 증가시킨다.
 * 3. 최초 요청(count == 1)일 때 TTL을 설정한다.
 * 4. 요청 횟수가 limit를 초과하면 TOO_MANY_REQUESTS 예외를 발생시킨다.
 *
 * Redis 키 구조: SpEL 표현식 평가 결과 (예: "rate:point:charge:1")
 * TTL: @RateLimit.seconds() 값 (기본 1초)
 *
 * 원자성 보장:
 * increment()와 expire()가 별개 명령이므로 엄밀히 말하면 원자적이지 않다.
 * count == 1일 때만 expire를 설정하므로, 극히 드물게 TTL이 누락될 수 있다.
 * 이 경우 2차로 DB UNIQUE 제약과 비관적 락이 방어선을 담당하므로 허용 가능한 수준이다.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final StringRedisTemplate redisTemplate;

    private static final ExpressionParser PARSER = new SpelExpressionParser();

    /**
     * @RateLimit이 붙은 메서드를 가로채어 요청 속도를 제한한다.
     */
    @Around("@annotation(rateLimit)")
    public Object checkRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String redisKey = resolveKey(joinPoint, rateLimit.key());

        // 요청 횟수 증가
        Long count = redisTemplate.opsForValue().increment(redisKey);

        // 최초 요청 시 TTL 설정
        // increment()가 null을 반환하는 경우(Redis 연결 문제)는 NPE 방어 처리
        if (count == null) {
            log.error("[RateLimitAspect] Redis increment 결과가 null — 요청을 통과시킵니다. key: {}", redisKey);
            return joinPoint.proceed();
        }

        if (count == 1L) {
            // 최초 요청: TTL 설정 (이후 요청들은 이 TTL 내에서 카운팅됨)
            redisTemplate.expire(redisKey, Duration.ofSeconds(rateLimit.seconds()));
        }

        if (count > rateLimit.limit()) {
            log.warn("[RateLimitAspect] 요청 제한 초과 - key: {}, count: {}, limit: {}",
                    redisKey, count, rateLimit.limit());
            throw new CustomException(ErrorCode.TOO_MANY_REQUESTS);
        }

        return joinPoint.proceed();
    }

    /**
     * SpEL 표현식을 메서드 파라미터 컨텍스트로 평가하여 Redis 키 문자열을 반환한다.
     *
     * 파라미터 이름 추출:
     * MethodSignature.getParameterNames()는 컴파일 시 -parameters 옵션이 있어야 동작한다.
     * Spring Boot는 기본적으로 이 옵션을 활성화하므로 별도 설정이 필요하지 않다.
     *
     * @param joinPoint     AOP 조인 포인트
     * @param keyExpression @RateLimit.key()의 SpEL 표현식
     * @return 평가된 Redis 키 문자열
     */
    private String resolveKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        MethodSignature signature  = (MethodSignature) joinPoint.getSignature();
        String[]        paramNames = signature.getParameterNames();
        Object[]        args       = joinPoint.getArgs();

        // SpEL 컨텍스트에 파라미터 이름-값 쌍을 등록
        EvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < paramNames.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }

        String resolvedKey = PARSER.parseExpression(keyExpression).getValue(context, String.class);
        log.debug("[RateLimitAspect] 해석된 Rate Limit 키: {}", resolvedKey);
        return resolvedKey;
    }
}