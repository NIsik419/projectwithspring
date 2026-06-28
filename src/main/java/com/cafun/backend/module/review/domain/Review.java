package com.cafun.backend.module.review.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 순수 Java 도메인 — Spring/JPA import 없음
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review {

    private Long id;
    private Long cafeId;
    private Long userId;
    private String content;
    private LocalDateTime createdAt;

    @Builder
    private Review(Long id, Long cafeId, Long userId, String content, LocalDateTime createdAt) {
        this.id = id;
        this.cafeId = cafeId;
        this.userId = userId;
        this.content = content;
        this.createdAt = createdAt;
    }

    public static Review create(Long cafeId, Long userId, String content) {
        return Review.builder()
                .cafeId(cafeId)
                .userId(userId)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
