package jpa.basic.coffeeshop.domain.user.service;

import jpa.basic.coffeeshop.domain.user.entity.User;

import java.util.Optional;

public interface UserQueryService {
    Optional<User> getByEmail(String email);

    User getById(Long userId);

    // 포인트 충전에서 사용하기 위한 비관적 락
    User getByIdWithLock(Long userId);
}
