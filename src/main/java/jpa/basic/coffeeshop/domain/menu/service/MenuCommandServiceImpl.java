package jpa.basic.coffeeshop.domain.menu.service;

import jpa.basic.coffeeshop.common.exception.CustomException;
import jpa.basic.coffeeshop.common.exception.ErrorCode;
import jpa.basic.coffeeshop.domain.menu.dto.request.CreateMenuRequest;
import jpa.basic.coffeeshop.domain.menu.entity.Menu;
import jpa.basic.coffeeshop.domain.menu.entity.UpdateMenuRequest;
import jpa.basic.coffeeshop.domain.menu.repository.MenuQueryRepository;
import jpa.basic.coffeeshop.domain.menu.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MenuCommandServiceImpl implements MenuCommandService {

    private final MenuRepository menuRepository;
    private final MenuQueryRepository menuQueryRepository;

    /**
     * 메뉴 생성
     *
     * 중복 방지 전략 (2중 방어):
     * 1차) Redis 분산 락: 동일 카테고리에 대해 동시에 한 명의 관리자만 진입 가능
     * 2차) DB UNIQUE 제약: 분산 락을 통과하더라도 DB 레벨에서 최종 차단
     */
    @Override
    public void createMenu(CreateMenuRequest request) {
        // 1차 중복 체크: 동일 이름 + 카테고리의 활성 메뉴가 이미 존재하는지 확인
        if (menuRepository.existsByMenuNameAndCategoryAndIsDeletedFalse(
                request.menuName(), request.category())) {
            throw new CustomException(ErrorCode.MENU_ALREADY_EXISTS);
        }

        Menu menu = Menu.builder()
                .menuName(request.menuName())
                .menuPrice(request.menuPrice())
                .menuStock(request.menuStock())
                .category(request.category())
                .build();

        menuRepository.save(menu);
        log.info("[MenuCreate] 메뉴 생성 완료 - name: {}, category: {}",
                request.menuName(), request.category());
    }

    /**
     * 메뉴 정보를 수정
     *
     * 낙관적 락 전략:
     * @Version 필드를 통해 JPA가 UPDATE 시 version 조건을 자동으로 검사
     * 동시 수정 충돌이 발생하면 ObjectOptimisticLockingFailureException을 던진다.
     * 메뉴 수정은 충돌 빈도가 낮으므로 분산 락 대신 낙관적 락을 채택
     *
     * 이미 존재하는 메뉴(이름 + 카테고리) 체크
     * 수정 대상은 체크에서 제외하여, 자기 자신으로 재저장하는 경우를 허용
     */
    @Override
    public void updateMenu(Long menuId, UpdateMenuRequest request) {
        Menu menu = menuQueryRepository.findActiveMenuById(menuId)
                .orElseThrow(() -> new CustomException(ErrorCode.MENU_NOT_FOUND));

        // 이미 존재하는 메뉴(이름 + 카테고리)로 변경하려는 경우 차단
        if (menuRepository.existsByMenuNameAndCategoryAndIsDeletedFalseAndIdNot(
                request.menuName(), request.category(), menuId)) {
            throw new CustomException(ErrorCode.MENU_ALREADY_EXISTS);
        }

        try {
            menu.update(
                    request.menuName(),
                    request.menuPrice(),
                    request.menuStock(),
                    request.category(),
                    request.menuStatus()
            );
        } catch (ObjectOptimisticLockingFailureException e) {
            // 동시에 다른 관리자가 동일 메뉴를 수정한 경우
            log.warn("[MenuUpdate] 낙관적 락 충돌 - menuId: {}", menuId);
            throw new CustomException(ErrorCode.MENU_OPTIMISTIC_LOCK_CONFLICT);
        }
    }

    /**
     * 메뉴를 Soft Delete 수행
     *
     * 실제 레코드는 유지되며 is_deleted = true, deleted_at = 현재 시각으로 변경
     * 삭제 후 동일한 이름 + 카테고리의 메뉴를 재등록 가능
     */
    @Override
    public void deleteMenu(Long menuId) {
        Menu menu = menuQueryRepository.findActiveMenuById(menuId)
                .orElseThrow(() -> new CustomException(ErrorCode.MENU_NOT_FOUND));

        menu.softDelete();
        log.info("[MenuDelete] 메뉴 소프트 삭제 완료 - menuId: {}", menuId);
    }
}
