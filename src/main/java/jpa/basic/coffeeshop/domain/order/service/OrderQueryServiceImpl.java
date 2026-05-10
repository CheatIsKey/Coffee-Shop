package jpa.basic.coffeeshop.domain.order.service;

import jpa.basic.coffeeshop.common.CursorResponse;
import jpa.basic.coffeeshop.common.exception.CustomException;
import jpa.basic.coffeeshop.common.exception.ErrorCode;
import jpa.basic.coffeeshop.domain.order.dto.request.AdminOrderSearchRequest;
import jpa.basic.coffeeshop.domain.order.dto.response.OrderDetailResponse;
import jpa.basic.coffeeshop.domain.order.dto.response.OrderListItemResponse;
import jpa.basic.coffeeshop.domain.order.entity.Order;
import jpa.basic.coffeeshop.domain.order.entity.OrderMenu;
import jpa.basic.coffeeshop.domain.order.repository.OrderMenuRepository;
import jpa.basic.coffeeshop.domain.order.repository.OrderQueryRepository;
import jpa.basic.coffeeshop.domain.user.entity.User;
import jpa.basic.coffeeshop.domain.user.repository.UserRepository;
import jpa.basic.coffeeshop.domain.user.service.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderQueryServiceImpl implements OrderQueryService {

    private final OrderQueryRepository orderQueryRepository;
    private final OrderMenuRepository orderMenuRepository;
    private final UserQueryService userQueryService;

    /**
     * 일반 사용자 본인 주문 목록 (커서 기반, 무한 스크롤)
     */
    @Override
    public CursorResponse<OrderListItemResponse> getMyOrders(Long userId, Long cursorId, int size) {
        User user = userQueryService.getById(userId);

        List<Order> orders = orderQueryRepository.findOrdersByUserWithCursor(userId, cursorId, size);

        boolean hasNext    = orders.size() > size;
        List<Order> paged  = hasNext ? orders.subList(0, size) : orders;
        Long nextCursor    = hasNext ? paged.get(paged.size() - 1).getId() : null;

        List<OrderListItemResponse> content = toListResponse(paged, user.getEmail());

        return new CursorResponse<>(content, hasNext, nextCursor);
    }

    /**
     * 관리자 전체 주문 목록 (동적 검색 + Offset 페이징)
     */
    @Override
    public Page<OrderListItemResponse> getAdminOrders(AdminOrderSearchRequest request, Pageable pageable) {
        Page<Order> orderPage = orderQueryRepository.findOrdersForAdmin(request, pageable);

        List<Long> userIds = orderPage.getContent().stream()
                .map(Order::getUserId).distinct().toList();
        Map<Long, String> emailMap = userQueryService.getAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getEmail));

        return orderPage.map(order -> {
            List<OrderMenu> menus = orderMenuRepository.findAllByOrderId(order.getId());
            return OrderListItemResponse.of(order, emailMap.getOrDefault(order.getUserId(), ""), menus);
        });
    }

    /**
     * 주문 상세 조회
     * PENDING 주문, 삭제된 주문 제외
     */
    @Override
    public OrderDetailResponse getOrderDetail(Long userId, String orderUid, boolean isAdmin) {
        Order order = orderQueryRepository.findCompletedOrderByUid(orderUid)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        if (!isAdmin && !order.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        List<OrderMenu> orderMenus = orderMenuRepository.findAllByOrderId(order.getId());
        return OrderDetailResponse.of(order, orderMenus);
    }

    private List<OrderListItemResponse> toListResponse(List<Order> orders, String email) {
        return orders.stream()
                .map(order -> {
                    List<OrderMenu> menus = orderMenuRepository.findAllByOrderId(order.getId());
                    return OrderListItemResponse.of(order, email, menus);
                }).toList();
    }
}
