package com.cafun.backend.module.cafe.application;

import com.cafun.backend.module.review.domain.ReviewWrittenEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewEventListener {

    private final AspectVectorCache aspectVectorCache;

    // 리뷰 커밋 후 해당 카페 벡터만 DB에서 재로드 (비동기 — 요청 응답 블로킹 방지)
    @Async
    @EventListener
    public void onReviewWritten(ReviewWrittenEvent event) {
        aspectVectorCache.refresh(event.cafeId());
        log.debug("AspectVectorCache refreshed for cafeId={}", event.cafeId());
    }
}
