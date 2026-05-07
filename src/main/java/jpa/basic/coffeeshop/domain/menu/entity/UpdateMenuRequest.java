package jpa.basic.coffeeshop.domain.menu.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 메뉴 수정 요청 DTO
 *
 * menuStatus:
 *  수정 API에서는 관리자가 메뉴 상태(판매중/품절/단종)를 직접 지정 가능
 *  재고 변동에 따른 자동 상태 전화(closeMenu/resumeMenu)과는 독립적으로 동작
 */
public record UpdateMenuRequest(

        @NotBlank(message = "메뉴 이름은 필수적입니다.")
        String menuName,

        @Positive(message = "메뉴 가격은 양수여야 합니다.")
        int menuPrice,

        @Positive(message = "메뉴 재고는 양수여야 합니다.")
        int menuStock,

        @NotNull(message = "카테고리는 필수입니다.")
        Category category,

        @NotNull(message = "메뉴 상태는 필수입니다.")
        MenuStatus menuStatus
) {
}
