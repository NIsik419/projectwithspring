package com.cafun.backend.module.review.application;

import com.cafun.backend.global.exception.BusinessException;
import com.cafun.backend.global.exception.ErrorCode;
import com.cafun.backend.module.cafe.application.port.CafeRepository;
import com.cafun.backend.module.review.application.port.ReviewRepository;
import com.cafun.backend.module.review.domain.Review;
import com.cafun.backend.module.review.domain.ReviewWrittenEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService implements WriteReviewUseCase, GetReviewListUseCase {

    private final ReviewRepository reviewRepository;
    private final CafeRepository cafeRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public Review writeReview(Long cafeId, Long userId, String content) {
        cafeRepository.findById(cafeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CAFE_NOT_FOUND));
        Review saved = reviewRepository.save(Review.create(cafeId, userId, content));
        // 트랜잭션 커밋 후 캐시 갱신 (cafe 모듈 의존 없이 이벤트로 분리)
        eventPublisher.publishEvent(new ReviewWrittenEvent(cafeId));
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> getReviewsByCafe(Long cafeId) {
        return reviewRepository.findByCafeId(cafeId);
    }
}
