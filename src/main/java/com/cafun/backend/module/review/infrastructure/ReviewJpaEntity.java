package com.cafun.backend.module.review.infrastructure;

import com.cafun.backend.module.review.domain.Review;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class ReviewJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long cafeId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private ReviewJpaEntity(Long id, Long cafeId, Long userId, String content, LocalDateTime createdAt) {
        this.id = id;
        this.cafeId = cafeId;
        this.userId = userId;
        this.content = content;
        this.createdAt = createdAt;
    }

    static ReviewJpaEntity from(Review review) {
        return ReviewJpaEntity.builder()
                .id(review.getId())
                .cafeId(review.getCafeId())
                .userId(review.getUserId())
                .content(review.getContent())
                .createdAt(review.getCreatedAt())
                .build();
    }

    Review toDomain() {
        return Review.builder()
                .id(this.id)
                .cafeId(this.cafeId)
                .userId(this.userId)
                .content(this.content)
                .createdAt(this.createdAt)
                .build();
    }
}
