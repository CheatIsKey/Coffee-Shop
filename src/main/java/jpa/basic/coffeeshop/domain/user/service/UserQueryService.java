package jpa.basic.coffeeshop.domain.user.service;

import jpa.basic.coffeeshop.domain.user.entity.User;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public interface UserQueryService {
    Optional<User> getByEmail(String email);

    User getById(Long userId);

    // 다른 도메인의 비즈니스 로직에서 사용하기 위한 비관적 락
    User getByIdWithLock(Long userId);

    // Order 도메인에서 userId로 User 이메일 일괄 조회를 위해 사용
    List<User> getAllById(List<Long> userIds);
}
