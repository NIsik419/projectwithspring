package com.cafun.backend.module.review.infrastructure;

import com.cafun.backend.module.review.application.port.ReviewRepository;
import com.cafun.backend.module.review.domain.Review;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
class ReviewPersistenceAdapter implements ReviewRepository {

    private final ReviewJpaRepository jpaRepository;

    @Override
    public Review save(Review review) {
        return jpaRepository.save(ReviewJpaEntity.from(review)).toDomain();
    }

    @Override
    public List<Review> findByCafeId(Long cafeId) {
        return jpaRepository.findByCafeIdOrderByCreatedAtDesc(cafeId).stream()
                .map(ReviewJpaEntity::toDomain)
                .toList();
    }
}
