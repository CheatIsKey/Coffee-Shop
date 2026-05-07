package jpa.basic.coffeeshop.domain.user.repository;

import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;
import jpa.basic.coffeeshop.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsUserByEmail(String email);

    /**
     * 비관적 락을 걸고 사용자를 조회
     *
     * 포인트 충전/차감 시 동시 접근으로 인한 데이터 정합성 문제를 방지하기 위해 사용
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
           SELECT u
           FROM User u
           WHERE u.id = :id
           """)
    Optional<User> findByIdWithLock(@Param("id") Long id);
}
