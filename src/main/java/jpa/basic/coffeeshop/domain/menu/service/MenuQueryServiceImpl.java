package jpa.basic.coffeeshop.domain.menu.service;

import jpa.basic.coffeeshop.common.exception.CustomException;
import jpa.basic.coffeeshop.common.exception.ErrorCode;
import jpa.basic.coffeeshop.domain.menu.dto.response.MenuDetailResponse;
import jpa.basic.coffeeshop.domain.menu.dto.response.MenuPageResponse;
import jpa.basic.coffeeshop.domain.menu.entity.Menu;
import jpa.basic.coffeeshop.domain.menu.repository.MenuQueryRepository;
import jpa.basic.coffeeshop.domain.menu.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MenuQueryServiceImpl implements MenuQueryService {

    private final MenuQueryRepository menuQueryRepository;
    private final MenuRepository menuRepository;

    /**
     * 삭제되지 않은 메뉴 목록을 Offset 페이징으로 조회
     *
     * 클라이언트가 보내는 1-indexed 페이지 번호에서 1을 빼서 반환
     * (예: 클라이언트 page=1 -> PageRequest.of(0, size))
     */
    @Override
    public MenuPageResponse getMenus(int page, int size) {
        PageRequest pageable = PageRequest.of(page - 1, size);
        Page<Menu> menuPage = menuQueryRepository.findMenusWithPaging(pageable);

        return MenuPageResponse.from(menuPage, page);
    }

    /**
     * 메뉴 ID로 단건 조회
     * is_deleted = true 이거나 존재하지 않는 메뉴는 예외 발생
     */
    @Override
    public MenuDetailResponse getMenuById(Long menuId) {
        Menu menu = menuQueryRepository.findActiveMenuById(menuId)
                .orElseThrow(() -> new CustomException(ErrorCode.MENU_NOT_FOUND));

        return MenuDetailResponse.from(menu);
    }

    @Override
    public List<Menu> getAllMenusById(List<Long> menuIds) {
        return menuRepository.findAllById(menuIds);
    }
}
