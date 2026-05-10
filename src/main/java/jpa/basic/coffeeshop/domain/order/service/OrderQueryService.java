package jpa.basic.coffeeshop.domain.order.service;

import jpa.basic.coffeeshop.common.CursorResponse;
import jpa.basic.coffeeshop.domain.order.dto.request.AdminOrderSearchRequest;
import jpa.basic.coffeeshop.domain.order.dto.response.OrderDetailResponse;
import jpa.basic.coffeeshop.domain.order.dto.response.OrderListItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderQueryService {

    CursorResponse<OrderListItemResponse> getMyOrders(Long userId, Long cursorId, int size);

    Page<OrderListItemResponse> getAdminOrders(AdminOrderSearchRequest request, Pageable pageable);

    OrderDetailResponse getOrderDetail(Long userId, String orderUid, boolean isAdmin);
}
