package com.cafun.backend.module.cafe.presentation.dto;

import com.cafun.backend.module.cafe.domain.Aspect;
import com.cafun.backend.module.cafe.domain.Cafe;

import java.time.LocalDateTime;
import java.util.Map;

public record CafeResponse(
        Long id,
        String name,
        String address,
        String phone,
        double latitude,
        double longitude,
        String imageUrl,
        LocalDateTime createdAt,
        Map<String, Float> aspectScores  // Aspect enum key → 레이블 문자열로 직렬화
) {
    public static CafeResponse from(Cafe cafe) {
        Map<String, Float> scores = null;
        if (cafe.getAspectScores() != null && !cafe.getAspectScores().isEmpty()) {
            scores = new java.util.LinkedHashMap<>();
            for (Map.Entry<Aspect, Float> e : cafe.getAspectScores().entrySet()) {
                scores.put(e.getKey().getLabel(), e.getValue());
            }
        }
        return new CafeResponse(
                cafe.getId(),
                cafe.getName(),
                cafe.getAddress(),
                cafe.getPhone(),
                cafe.getLatitude(),
                cafe.getLongitude(),
                cafe.getImageUrl(),
                cafe.getCreatedAt(),
                scores
        );
    }
}
