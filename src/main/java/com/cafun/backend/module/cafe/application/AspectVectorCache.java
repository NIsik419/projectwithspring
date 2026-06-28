package com.cafun.backend.module.cafe.application;

import com.cafun.backend.module.cafe.application.port.AspectVectorPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class AspectVectorCache {

    private final AspectVectorPort aspectVectorPort;
    private final Map<Long, float[]> cache = new ConcurrentHashMap<>();

    @EventListener(ApplicationReadyEvent.class)
    public void load() {
        Map<Long, float[]> all = aspectVectorPort.findAll();
        cache.clear();
        cache.putAll(all);
        log.info("AspectVectorCache loaded: {} cafes", cache.size());
    }

    public Optional<float[]> get(Long cafeId) {
        return Optional.ofNullable(cache.get(cafeId));
    }

    public void refresh(Long cafeId) {
        aspectVectorPort.findByCafeId(cafeId).ifPresent(v -> cache.put(cafeId, v));
    }

    // 코사인 유사도 기준 상위 limit개 cafeId 반환 (내림차순)
    public List<Long> findTopBySimilarity(float[] query, int limit) {
        return cache.entrySet().stream()
                .map(e -> new ScoredId(e.getKey(), cosineSimilarity(query, e.getValue())))
                .sorted((a, b) -> Float.compare(b.score(), a.score()))
                .limit(limit)
                .map(ScoredId::cafeId)
                .toList();
    }

    private float cosineSimilarity(float[] a, float[] b) {
        float dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0f;
        return dot / ((float) Math.sqrt(normA) * (float) Math.sqrt(normB));
    }

    private record ScoredId(Long cafeId, float score) {}
}
