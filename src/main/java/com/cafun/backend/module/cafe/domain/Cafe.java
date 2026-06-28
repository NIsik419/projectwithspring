package com.cafun.backend.module.cafe.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

// 순수 Java 도메인 — Spring/JPA import 없음
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cafe {

    private Long id;
    private String name;
    private String address;
    private String phone;
    private double latitude;
    private double longitude;
    private String imageUrl;
    private LocalDateTime createdAt;

    // DualHeadBERT ABSA 12개 aspect 점수 — 파트 5 벡터 캐시 로드 시 채워짐
    private Map<Aspect, Float> aspectScores;

    @Builder
    private Cafe(Long id, String name, String address, String phone,
                 double latitude, double longitude, String imageUrl,
                 LocalDateTime createdAt, Map<Aspect, Float> aspectScores) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
        this.aspectScores = aspectScores != null ? aspectScores : new EnumMap<>(Aspect.class);
    }

    // float[] 벡터 배열을 Aspect enum 키로 변환 — 파트 5에서 캐시 병합 시 사용
    public static Cafe withAspects(Cafe cafe, float[] vectors) {
        Map<Aspect, Float> scores = new EnumMap<>(Aspect.class);
        Aspect[] aspects = Aspect.values();
        for (int i = 0; i < aspects.length && i < vectors.length; i++) {
            scores.put(aspects[i], vectors[i]);
        }
        return Cafe.builder()
                .id(cafe.id).name(cafe.name).address(cafe.address).phone(cafe.phone)
                .latitude(cafe.latitude).longitude(cafe.longitude)
                .imageUrl(cafe.imageUrl).createdAt(cafe.createdAt)
                .aspectScores(scores)
                .build();
    }
}
