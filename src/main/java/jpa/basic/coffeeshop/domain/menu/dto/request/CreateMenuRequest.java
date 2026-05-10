package jpa.basic.coffeeshop.domain.menu.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jpa.basic.coffeeshop.domain.menu.entity.Category;

/**
 * 메뉴 생성 요청 DTO
 */
public record CreateMenuRequest(

        @NotBlank(message = "메뉴 이름은 필수입니다.")
        String menuName,

        @NotNull(message = "메뉴 가격은 필수입니다.")
        @Positive(message = "메뉴 가격은 양수여야 합니다.")
        int menuPrice,

        @NotNull(message = "메뉴 재고는 필수입니다.")
        @Positive(message = "메뉴 재고는 양수여야 합니다.")
        int menuStock,

        @NotNull(message = "카테고리는 필수입니다.")
        Category category
) {
}
