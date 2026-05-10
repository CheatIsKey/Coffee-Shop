package jpa.basic.coffeeshop.domain.point.service;

import jpa.basic.coffeeshop.common.exception.CustomException;
import jpa.basic.coffeeshop.common.exception.ErrorCode;
import jpa.basic.coffeeshop.common.util.TsidHolder;
import jpa.basic.coffeeshop.domain.point.dto.request.ChargePointRequest;
import jpa.basic.coffeeshop.domain.point.dto.response.ChargePointResponse;
import jpa.basic.coffeeshop.domain.point.entity.PointCharge;
import jpa.basic.coffeeshop.domain.point.entity.PointLog;
import jpa.basic.coffeeshop.domain.point.entity.PointLogType;
import jpa.basic.coffeeshop.domain.point.repository.PointChargeRepository;
import jpa.basic.coffeeshop.domain.point.repository.PointLogRepository;
import jpa.basic.coffeeshop.domain.user.entity.User;
import jpa.basic.coffeeshop.domain.user.repository.UserRepository;
import jpa.basic.coffeeshop.domain.user.service.UserQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PointCommandServiceImpl implements PointCommandService {

    private final UserQueryService userQueryService;
    private final PointChargeRepository pointChargeRepository;
    private final PointLogRepository pointLogRepository;
    private final TsidHolder tsidHolder;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 포인트 충전
     *
     * 처리 순서:
     *  1. 비관적 락으로 사용자 조회 — 동시 충전/차감 충돌 방지
     *  2. PointCharge 생성 (PENDING)
     *  3. User.point 증가
     *  4. PointCharge → SUCCESS 전환, paid_at 기록
     *  5. PointLog 생성 (잔액 스냅샷 포함)
     */
    @Override
    public ChargePointResponse chargePoint(Long userId, ChargePointRequest request) {
        if (request.amount() <= 0) {
            throw new CustomException(ErrorCode.INVALID_AMOUNT);
        }

        User user = userQueryService.getByIdWithLock(userId);

        String pgTid = generatePgTid();

        PointCharge charge = PointCharge.builder()
                .amount(request.amount())
                .paymentType(request.paymentType())
                .pgTid(pgTid)
                .userId(userId)
                .build();

        PointCharge savedCharge = pointChargeRepository.save(charge);

        user.chargePoint(request.amount());

        savedCharge.completePayment();

        PointLog pointLog = PointLog.builder()
                .type(PointLogType.CHARGE)
                .amount(request.amount())
                .remainPoint(user.getPoint())
                .userId(userId)
                .orderId(null)
                .chargeId(savedCharge.getId())
                .build();

        pointLogRepository.save(pointLog);

        log.info("[PointCharge] 포인트 충전 완료 - userId: {}, amount: {}, remainPoint: {}, pgTid: {}",
                userId, request.amount(), user.getPoint(), pgTid);

        return ChargePointResponse.of(savedCharge, user.getPoint());
    }

    @Override
    public void recordOrderUsage(Long userId, Long orderId, Long amount, Long remainPoint) {
        pointLogRepository.save(PointLog.builder()
                .type(PointLogType.USE)
                .amount(amount)
                .remainPoint(remainPoint)
                .userId(userId)
                .orderId(orderId)
                .chargeId(null)
                .build());
    }

    private String generatePgTid() {
        return "TID_" + LocalDate.now().format(DATE_FORMATTER) + "_" + tsidHolder.generate();
    }
}
