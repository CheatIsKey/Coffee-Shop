package jpa.basic.coffeeshop.domain.menu.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/**
 * 메뉴 목록 조회 요청 DTO
 * @ModelAttribute로 Query Parameter를 바인딩
 *
 * record 대신 일반 클래스를 사용한 이유:
 *  - record는 생성자에서만 필드를 초기할 수 있어 기본값 설정이 불편하다.
 *  - @ModelAttribute는 기본 생성자 + setter를 통해 바인딩하므로
 *    일반 클래스로 기본값을 자연스럽게 표현한다.
 */
@Getter
@Setter
public class MenuPageRequest {

    /**
     * 페이지 번호
     */
    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
    private int page = 1;

    /**
     * 페이지당 항목 수
     */
    @Min(value = 1, message = "페이지당 항목 수는 1 이상이어야 합니다.")
    private int size = 10;
}
