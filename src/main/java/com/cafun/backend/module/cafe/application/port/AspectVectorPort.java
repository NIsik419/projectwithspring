package com.cafun.backend.module.cafe.application.port;

import java.util.Map;
import java.util.Optional;

public interface AspectVectorPort {
    Map<Long, float[]> findAll();           // 앱 시작 시 전체 로드
    Optional<float[]> findByCafeId(Long cafeId);
    void upsert(Long cafeId, float[] vectors);  // 리뷰 재계산 후 동기화
}
