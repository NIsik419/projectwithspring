package com.cafun.backend.module.user.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

import java.time.LocalDateTime;

// 순수 Java 도메인 객체 — Spring/JPA import 없음
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    private Long id;
    private String email;
    private String password;
    private String nickname;
    private LocalDateTime createdAt;

    @Builder
    private User(Long id, String email, String password, String nickname, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.createdAt = createdAt;
    }

    public static User create(String email, String encodedPassword, String nickname) {
        return User.builder()
                .email(email)
                .password(encodedPassword)
                .nickname(nickname)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
