package com.cafun.backend.module.review.application;

import com.cafun.backend.global.exception.BusinessException;
import com.cafun.backend.global.exception.ErrorCode;
import com.cafun.backend.module.cafe.application.port.CafeRepository;
import com.cafun.backend.module.cafe.domain.Cafe;
import com.cafun.backend.module.review.application.port.ReviewRepository;
import com.cafun.backend.module.review.domain.Review;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock ReviewRepository reviewRepository;
    @Mock CafeRepository cafeRepository;
    @InjectMocks ReviewService reviewService;

    @Test
    @DisplayName("리뷰 작성 성공")
    void writeReview_success() {
        given(cafeRepository.findById(1L)).willReturn(Optional.of(stubCafe()));
        Review saved = Review.builder()
                .id(1L).cafeId(1L).userId(2L)
                .content("맛있어요").createdAt(LocalDateTime.now())
                .build();
        given(reviewRepository.save(any())).willReturn(saved);

        Review result = reviewService.writeReview(1L, 2L, "맛있어요");

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getCafeId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(2L);
        verify(reviewRepository).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 카페에 리뷰 작성 시 CAFE_NOT_FOUND 예외")
    void writeReview_cafeNotFound() {
        given(cafeRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.writeReview(999L, 2L, "내용"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.CAFE_NOT_FOUND));
    }

    @Test
    @DisplayName("카페별 리뷰 목록 조회")
    void getReviewsByCafe_returnsList() {
        List<Review> reviews = List.of(
                Review.builder().id(1L).cafeId(1L).userId(2L).content("좋아요").createdAt(LocalDateTime.now()).build(),
                Review.builder().id(2L).cafeId(1L).userId(3L).content("또 올게요").createdAt(LocalDateTime.now()).build()
        );
        given(reviewRepository.findByCafeId(1L)).willReturn(reviews);

        List<Review> result = reviewService.getReviewsByCafe(1L);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(r -> r.getCafeId().equals(1L));
    }

    private Cafe stubCafe() {
        return Cafe.builder()
                .id(1L).name("테스트 카페").address("성수동").latitude(37.5).longitude(127.0)
                .createdAt(LocalDateTime.now()).aspectScores(null)
                .build();
    }
}
