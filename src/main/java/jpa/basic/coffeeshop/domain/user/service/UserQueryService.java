package jpa.basic.coffeeshop.domain.user.service;

import jpa.basic.coffeeshop.domain.user.entity.User;

import java.util.Optional;

public interface UserQueryService {
    Optional<User> getByEmail(String email);

    User getById(Long userId);
}
