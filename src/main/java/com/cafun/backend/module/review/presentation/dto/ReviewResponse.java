package com.cafun.backend.module.review.presentation.dto;

import com.cafun.backend.module.review.domain.Review;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        Long cafeId,
        Long userId,
        String content,
        LocalDateTime createdAt
) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getCafeId(),
                review.getUserId(),
                review.getContent(),
                review.getCreatedAt()
        );
    }
}
