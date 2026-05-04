package jpa.basic.coffeeshop.domain.user.service;

public interface UserCommandService {
    void create(String email, String encodedPassword);
}
