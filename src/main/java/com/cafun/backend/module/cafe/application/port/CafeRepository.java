package com.cafun.backend.module.cafe.application.port;

import com.cafun.backend.module.cafe.domain.Cafe;

import java.util.List;
import java.util.Optional;

// infrastructure → application 방향으로 구현되는 출력 포트
public interface CafeRepository {
    List<Cafe> findAll();
    Optional<Cafe> findById(Long id);
    List<Cafe> findAllByIds(List<Long> ids);
    List<Cafe> findByNameSimilarity(String keyword, int limit);
}
