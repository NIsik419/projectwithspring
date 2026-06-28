package com.cafun.backend.module.review.presentation;

import com.cafun.backend.global.response.ApiResponse;
import com.cafun.backend.module.review.application.GetReviewListUseCase;
import com.cafun.backend.module.review.application.WriteReviewUseCase;
import com.cafun.backend.module.review.presentation.dto.ReviewResponse;
import com.cafun.backend.module.review.presentation.dto.WriteReviewRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/review")
@RequiredArgsConstructor
public class ReviewController {

    private final WriteReviewUseCase writeReviewUseCase;
    private final GetReviewListUseCase getReviewListUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReviewResponse> writeReview(@Valid @RequestBody WriteReviewRequest request,
                                                   Authentication auth) {
        // JWT subject = userId(Long) — JwtAuthenticationFilter에서 principal로 세팅됨
        Long userId = Long.parseLong(auth.getName());
        var review = writeReviewUseCase.writeReview(request.cafeId(), userId, request.content());
        return ApiResponse.created(ReviewResponse.from(review));
    }

    @GetMapping
    public ApiResponse<List<ReviewResponse>> getReviews(@RequestParam Long cafeId) {
        List<ReviewResponse> responses = getReviewListUseCase.getReviewsByCafe(cafeId).stream()
                .map(ReviewResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }
}
