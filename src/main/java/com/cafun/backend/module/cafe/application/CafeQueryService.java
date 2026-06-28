package com.cafun.backend.module.cafe.application;

import com.cafun.backend.global.exception.BusinessException;
import com.cafun.backend.global.exception.ErrorCode;
import com.cafun.backend.module.cafe.application.port.CafeRepository;
import com.cafun.backend.module.cafe.domain.Cafe;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CafeQueryService implements GetCafeListUseCase, GetCafeUseCase, CafeNameSearchUseCase {

    private final CafeRepository cafeRepository;
    private final AspectVectorCache aspectVectorCache;

    @Override
    public List<Cafe> getCafeList() {
        return cafeRepository.findAll().stream()
                .map(cafe -> aspectVectorCache.get(cafe.getId())
                        .map(v -> Cafe.withAspects(cafe, v))
                        .orElse(cafe))
                .toList();
    }

    @Override
    public Cafe getCafe(Long id) {
        Cafe cafe = cafeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CAFE_NOT_FOUND));
        return aspectVectorCache.get(id)
                .map(v -> Cafe.withAspects(cafe, v))
                .orElse(cafe);
    }

    @Override
    public List<Cafe> searchByName(String keyword, int limit) {
        return cafeRepository.findByNameSimilarity(keyword, limit).stream()
                .map(cafe -> aspectVectorCache.get(cafe.getId())
                        .map(v -> Cafe.withAspects(cafe, v))
                        .orElse(cafe))
                .toList();
    }
}
