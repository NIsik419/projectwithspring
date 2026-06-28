package com.cafun.backend.module.review.application.port;

import com.cafun.backend.module.review.domain.Review;

import java.util.List;

public interface ReviewRepository {
    Review save(Review review);
    List<Review> findByCafeId(Long cafeId);
}
