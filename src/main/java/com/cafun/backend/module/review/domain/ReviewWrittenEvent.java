package com.cafun.backend.module.review.domain;

// 리뷰 작성 후 카페 aspect 벡터 캐시 갱신을 위한 도메인 이벤트
public record ReviewWrittenEvent(Long cafeId) {}
