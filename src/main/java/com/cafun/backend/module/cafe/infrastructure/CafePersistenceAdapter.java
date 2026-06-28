package com.cafun.backend.module.cafe.infrastructure;

import com.cafun.backend.module.cafe.application.port.CafeRepository;
import com.cafun.backend.module.cafe.domain.Cafe;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
class CafePersistenceAdapter implements CafeRepository {

    private final CafeJpaRepository jpaRepository;

    @Override
    public List<Cafe> findAll() {
        return jpaRepository.findAll().stream()
                .map(CafeJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<Cafe> findById(Long id) {
        return jpaRepository.findById(id)
                .map(CafeJpaEntity::toDomain);
    }

    @Override
    public List<Cafe> findAllByIds(List<Long> ids) {
        return jpaRepository.findAllById(ids).stream()
                .map(CafeJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<Cafe> findByNameSimilarity(String keyword, int limit) {
        return jpaRepository.findByNameSimilarity(keyword, limit).stream()
                .map(CafeJpaEntity::toDomain)
                .toList();
    }
}
