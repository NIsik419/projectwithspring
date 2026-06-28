package com.cafun.backend.module.cafe.application;

import com.cafun.backend.module.cafe.application.port.CafeRepository;
import com.cafun.backend.module.cafe.domain.Cafe;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CafeSearchService implements CafeSearchUseCase {

    private final CafeRepository cafeRepository;
    private final AspectVectorCache aspectVectorCache;

    @Override
    public List<Cafe> searchByAspectVector(float[] queryVector, int limit) {
        List<Long> rankedIds = aspectVectorCache.findTopBySimilarity(queryVector, limit);
        if (rankedIds.isEmpty()) return List.of();

        Map<Long, Cafe> cafeMap = cafeRepository.findAllByIds(rankedIds).stream()
                .collect(Collectors.toMap(Cafe::getId, Function.identity()));

        // 유사도 순서 유지하면서 aspect 점수 병합
        return rankedIds.stream()
                .filter(cafeMap::containsKey)
                .map(id -> aspectVectorCache.get(id)
                        .map(v -> Cafe.withAspects(cafeMap.get(id), v))
                        .orElse(cafeMap.get(id)))
                .toList();
    }
}
