package jpa.basic.coffeeshop.domain.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import jpa.basic.coffeeshop.common.exception.CustomException;
import jpa.basic.coffeeshop.common.exception.ErrorCode;
import jpa.basic.coffeeshop.common.util.TsidHolder;
import jpa.basic.coffeeshop.domain.menu.entity.Menu;
import jpa.basic.coffeeshop.domain.menu.service.MenuCommandService;
import jpa.basic.coffeeshop.domain.order.dto.request.CreateOrderRequest;
import jpa.basic.coffeeshop.domain.order.dto.request.OrderItemRequest;
import jpa.basic.coffeeshop.domain.order.dto.response.CreateOrderResponse;
import jpa.basic.coffeeshop.domain.order.entity.Order;
import jpa.basic.coffeeshop.domain.order.entity.OrderMenu;
import jpa.basic.coffeeshop.domain.order.entity.OrderStatus;
import jpa.basic.coffeeshop.domain.order.entity.OutboxEvent;
import jpa.basic.coffeeshop.domain.order.kafka.OrderCreatedEvent;
import jpa.basic.coffeeshop.domain.order.kafka.OrderEventProducer;
import jpa.basic.coffeeshop.domain.order.repository.OrderMenuRepository;
import jpa.basic.coffeeshop.domain.order.repository.OrderRepository;
import jpa.basic.coffeeshop.domain.order.repository.OutboxEventRepository;
import jpa.basic.coffeeshop.domain.point.entity.PointLog;
import jpa.basic.coffeeshop.domain.point.entity.PointLogType;
import jpa.basic.coffeeshop.domain.point.repository.PointLogRepository;
import jpa.basic.coffeeshop.domain.point.service.PointCommandService;
import jpa.basic.coffeeshop.domain.user.entity.User;
import jpa.basic.coffeeshop.domain.user.service.UserQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OrderCommandServiceImpl implements OrderCommandService {

    private final UserQueryService userQueryService;
    private final OrderRepository orderRepository;
    private final OrderMenuRepository orderMenuRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final PointCommandService pointCommandService;
    private final OrderEventProducer orderEventProducer;
    private final TsidHolder tsidHolder;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FORMATTER
            = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final MenuCommandService menuCommandService;
    private final PointLogRepository pointLogRepository;

    private final MeterRegistry meterRegistry;

    /**
     * 주문 생성 + 결제 처리
     *
     * 처리 순서:
     *  1. User 비관적 락 조회
     *  2. orderFingerprint 생성 -> Order 생성 (PENDING)
     *  3. 메뉴별 재고 차감 (비관적 락) + MenuStockLog 기록
     *  4. 포인트 차감 + Order COMPLETED 전환
     *  5. OrderMenu 저장 + PointLog 저장
     *  6. OutboxEvent 저장 (Kafka 발행 보장용)
     *  7. After Commit Hook 등록 -> 커밋 후 즉시 Kafka 발행 시도
     */
    @Override
    public CreateOrderResponse createOrder(Long userId, CreateOrderRequest request) {
        User user = userQueryService.getByIdWithLock(userId);

        String orderUid = generateOrderUid();
        String orderFingerprint = generateFingerprint(request.items(), userId);

        Order order = Order.builder()
                .orderUid(orderUid)
                .totalAmount(0L)        // 재고 차감 후 계산
                .orderStatus(OrderStatus.PENDING)
                .userId(userId)
                .orderFingerprint(orderFingerprint)
                .build();

        Order savedOrder;
        try {
            savedOrder = orderRepository.save(order);
            orderRepository.flush();
        } catch (DataIntegrityViolationException e) {
            log.warn("[OrderCreate] 중복 주문 감지 - userId: {}, fingerprint: {}", userId, orderFingerprint);
            throw new CustomException(ErrorCode.ORDER_ALREADY_EXISTS);
        }

        List<OrderMenu> orderMenus = new ArrayList<>();
        long totalAmount = 0L;

        for (OrderItemRequest item : request.items()) {
            // menuCommandService가 비관적 락 + 재고 차감 + MenuStockLog 기록을 수행
            Menu menu = menuCommandService.decreaseStockWithLog(
                    item.menuId(), item.quantity(), savedOrder.getId());

            totalAmount += (long) menu.getMenuPrice() * item.quantity();

            orderMenus.add(OrderMenu.builder()
                    .menuId(menu.getId())
                    .orderId(savedOrder.getId())
                    .menuName(menu.getMenuName())
                    .menuPrice(menu.getMenuPrice())
                    .quantity(item.quantity())
                    .build()
            );
        }

        orderMenuRepository.saveAll(orderMenus);
        user.spendPoint(totalAmount);   // 잔액 부족 시 예외 발생
        savedOrder.complete(totalAmount);

        pointCommandService.recordOrderUsage(
                userId,
                savedOrder.getId(),
                totalAmount,
                user.getPoint()
        );

        // OutboxEvent 저장
        OrderCreatedEvent kafkaEvent = buildKafkaEvent(savedOrder, user, orderMenus);
        OutboxEvent outboxEvent = buildOutboxEvent(savedOrder, kafkaEvent);
        OutboxEvent savedOutbox = outboxEventRepository.save(outboxEvent);

        // After Commit Hook 등록
        Long outboxId = savedOutbox.getId();
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            orderEventProducer.tryPublishAfterCommit(outboxId, kafkaEvent);
                        }
                }
        );

        meterRegistry.counter("coffeeshop.order.created.total").increment();

        log.info("[OrderCreate] 완료 - orderUid: {}, userId: {}, totalAmount: {}",
                orderUid, userId, totalAmount);

        return CreateOrderResponse.of(savedOrder, user, orderMenus);
    }

    /**
     * 주문 Soft Delete 수행
     * 낙관적 락으로 동시 삭제 충돌을 감지
     */
    @Override
    public void deleteOrder(Long userId, String orderUid, boolean isAdmin) {
        Order order = orderRepository.findByOrderUid(orderUid)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        if (!isAdmin && !order.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        try {
            order.softDelete();
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("[OrderDelete] 낙관적 락 충돌 - orderUid: {}", orderUid);
            throw new CustomException(ErrorCode.TOO_MANY_REQUESTS);
        }

        log.info("[OrderDelete] 완료 - orderUid: {}, userId: {}", orderUid, userId);
    }

    /**
     * 주문 UID 생성
     * 형식: ORD-{yyyyMMdd}-{TSID}
     */
    private String generateOrderUid() {
        return "ORD-" + LocalDate.now().format(DATE_FORMATTER) + "-" + tsidHolder.generate();
    }

    /**
     * 주문 멱등성 키(fingerprint) 생성
     *
     * 구성: userId + 30초 단위 시간 윈도우 + 정렬된 "menuId:quantity" 조합
     *
     * 30초 윈도우를 포함하면 30초 내 동일 요청만 차단하고
     * 30초 이후 의도적 재주문은 다른 fingerprint로 인식하여 정상 처리한다.
     *
     * Redis 분산 락(5초)이 1차로 동시 요청을 차단하므로, fingerprint는 2차 방어다.
     */
    private String generateFingerprint(List<OrderItemRequest> items, Long userId) {
        long timeWindow = System.currentTimeMillis() / 30_000;  // 30초마다 값이 바뀜

        String raw = userId + ":" + timeWindow + ":" +
                items.stream()
                .sorted(Comparator.comparingLong(OrderItemRequest::menuId))
                .map(i -> i.menuId() + ":" + i.quantity())
                .collect(Collectors.joining(","));

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }

    private OrderCreatedEvent buildKafkaEvent(Order order, User user, List<OrderMenu> menus) {
        List<OrderCreatedEvent.OrderItem> items = menus.stream()
                .map(m -> new OrderCreatedEvent.OrderItem(
                        m.getMenuId(), m.getMenuName(), m.getQuantity(), m.calculateAmount()
                )).toList();

        return new OrderCreatedEvent(
                user.getId(), order.getOrderUid(), order.getTotalAmount(), items, order.getCreatedAt()
        );
    }

    private OutboxEvent buildOutboxEvent(Order order, OrderCreatedEvent event) {
        try {
            return OutboxEvent.builder()
                    .aggregateType("ORDER")
                    .aggregateId(order.getId())
                    .eventType("ORDER_CREATED")
                    .payload(objectMapper.writeValueAsString(event))
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("OutboxEvent 직렬화 실패", e);
        }
    }
}
