package com.cafun.backend.module.review.application;

import com.cafun.backend.module.review.domain.Review;

import java.util.List;

public interface GetReviewListUseCase {
    List<Review> getReviewsByCafe(Long cafeId);
}
