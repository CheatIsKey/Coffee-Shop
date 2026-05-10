package jpa.basic.coffeeshop.domain.point.service;

import jpa.basic.coffeeshop.common.exception.CustomException;
import jpa.basic.coffeeshop.common.exception.ErrorCode;
import jpa.basic.coffeeshop.common.util.TsidHolder;
import jpa.basic.coffeeshop.domain.point.dto.request.ChargePointRequest;
import jpa.basic.coffeeshop.domain.point.dto.response.ChargePointResponse;
import jpa.basic.coffeeshop.domain.point.entity.PaymentStatus;
import jpa.basic.coffeeshop.domain.point.entity.PaymentType;
import jpa.basic.coffeeshop.domain.point.entity.PointCharge;
import jpa.basic.coffeeshop.domain.point.entity.PointLogType;
import jpa.basic.coffeeshop.domain.point.repository.PointChargeRepository;
import jpa.basic.coffeeshop.domain.point.repository.PointLogRepository;
import jpa.basic.coffeeshop.domain.user.entity.User;
import jpa.basic.coffeeshop.domain.user.service.UserQueryService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

/**
 * 포인트 충전 서비스 단위 테스트
 *
 * Mock을 사용하여 DB/Redis 없이 비즈니스 로직만 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class PointCommandServiceTest {

    @InjectMocks private PointCommandServiceImpl pointCommandService;

    @Mock private UserQueryService       userQueryService;
    @Mock private PointChargeRepository  pointChargeRepository;
    @Mock private PointLogRepository     pointLogRepository;
    @Mock private TsidHolder             tsidHolder;
    @Spy  private MeterRegistry          meterRegistry = new SimpleMeterRegistry();

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.createUser("test@test.com", "password");
    }

    @Test
    @DisplayName("정상 충전 시 포인트가 증가하고 PointCharge가 SUCCESS 상태가 된다")
    void chargePoint_success() {
        // given
        long chargeAmount = 10_000L;
        ChargePointRequest request = new ChargePointRequest(chargeAmount, PaymentType.CARD);

        given(tsidHolder.generate()).willReturn("FIXED_TSID");

        PointCharge savedCharge = PointCharge.builder()
                .userId(1L).amount(chargeAmount)
                .paymentType(PaymentType.CARD).pgTid("TID_TEST")
                .build();

        given(userQueryService.getByIdWithLock(any())).willReturn(testUser);
        given(pointChargeRepository.save(any())).willReturn(savedCharge);
        given(pointLogRepository.save(any())).willReturn(null);

        // when
        ChargePointResponse response = pointCommandService.chargePoint(1L, request);

        // then
        assertThat(testUser.getPoint()).isEqualTo(chargeAmount);          // 포인트 증가 확인
        assertThat(savedCharge.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS); // SUCCESS 전환
        assertThat(savedCharge.getPaidAt()).isNotNull();                   // 결제 시각 기록
        verify(pointLogRepository).save(
                argThat(log -> log.getType() == PointLogType.CHARGE       // 로그 타입 확인
                        && log.getAmount().equals(chargeAmount)));          // 로그 금액 확인
    }

    @Test
    @DisplayName("충전 금액이 0 이하면 INVALID_AMOUNT 예외가 발생한다")
    void chargePoint_invalidAmount_throwsException() {
        // given
        ChargePointRequest request = new ChargePointRequest(0L, PaymentType.CARD);

        // when & then
        assertThatThrownBy(() -> pointCommandService.chargePoint(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_AMOUNT);
    }

    @Test
    @DisplayName("잔액보다 많이 차감하면 INSUFFICIENT_POINT 예외가 발생한다")
    void spendPoint_insufficientBalance_throwsException() {
        // given: 잔액 1000원인 사용자
        testUser.chargePoint(1_000L);

        // when & then: 2000원 차감 시도
        assertThatThrownBy(() -> testUser.spendPoint(2_000L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_POINT);

        // 잔액은 그대로여야 함
        assertThat(testUser.getPoint()).isEqualTo(1_000L);
    }
}