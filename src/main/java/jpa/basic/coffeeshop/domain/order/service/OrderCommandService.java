package jpa.basic.coffeeshop.domain.order.service;

import jpa.basic.coffeeshop.domain.order.dto.request.CreateOrderRequest;
import jpa.basic.coffeeshop.domain.order.dto.response.CreateOrderResponse;

public interface OrderCommandService {

    /**
     * 주문을 생성하고 결제를 처리한다.
     * 분산 락은 OrderCreateFacade에서 처리
     */
    CreateOrderResponse createOrder(Long userId, CreateOrderRequest createOrderRequest);

    /**
     * 주문을 Soft Delete 처리한다.
     * 일반 사용자는 본인 주문만, 관리자는 모든 주문 삭제 가능
     */
    void deleteOrder (Long userId, String orderUid, boolean isAdmin);
}
