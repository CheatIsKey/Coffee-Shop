package jpa.basic.coffeeshop.domain.order.dto.request;

import java.time.LocalDate;

/**
 * 관리자 주문 목록 동적 검색 요청 DTO
 * 모든 필드가 Optional이며, 조합 가능
 */
public record AdminOrderSearchRequest(
        String orderUid,
        LocalDate startDate,
        LocalDate endDate,
        String email
) {
}
