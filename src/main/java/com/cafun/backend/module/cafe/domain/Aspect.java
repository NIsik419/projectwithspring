package com.cafun.backend.module.cafe.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// DualHeadBERT ABSA 모델의 12개 aspect — ordinal = float[] 벡터 인덱스
@Getter
@RequiredArgsConstructor
public enum Aspect {
    TASTE("맛/커피"),
    FOOD("음식/디저트"),
    SERVICE("서비스/직원"),
    ATMOSPHERE("분위기/인테리어"),
    PRICE("가격/가성비"),
    LOCATION("위치/접근성"),
    CLEANLINESS("청결/위생"),
    NOISE("소음"),
    WAITING("대기시간"),
    PARKING("주차"),
    WIFI("Wi-Fi/콘센트"),
    SPACE("좌석/공간");

    private final String label;

    public int index() {
        return this.ordinal();
    }
}
