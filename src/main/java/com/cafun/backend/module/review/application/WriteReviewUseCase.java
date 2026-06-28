package com.cafun.backend.module.review.application;

import com.cafun.backend.module.review.domain.Review;

public interface WriteReviewUseCase {
    Review writeReview(Long cafeId, Long userId, String content);
}
