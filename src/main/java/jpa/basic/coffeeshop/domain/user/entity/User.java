package jpa.basic.coffeeshop.domain.user.entity;

import jakarta.persistence.*;
import jpa.basic.coffeeshop.common.exception.CustomException;
import jpa.basic.coffeeshop.common.exception.ErrorCode;
import jpa.basic.coffeeshop.domain.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private long point;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.USER;

    // 회원가입
    public static User createUser(String email, String encodedPassword) {
        User user = new User();
        user.email = email;
        user.password = encodedPassword;
        user.point = 0L;
        return user;
    }

    // 비밀번호 수정
    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    // 포인트 충전
    public void chargePoint(long point) {
        validPoint(point);
        this.point += point;
    }

    // 포인트 차감
    public void spendPoint(long point) {
        validPoint(point);
        verifyPoint(point);
        this.point -= point;
    }

    private void validPoint(long point) {
        if (point <= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private void verifyPoint(long needPoint) {
        if (this.point < needPoint) {
            throw new CustomException(ErrorCode.INSUFFICIENT_POINT);
        }
    }
}
