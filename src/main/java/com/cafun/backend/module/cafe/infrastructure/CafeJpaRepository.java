package com.cafun.backend.module.cafe.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface CafeJpaRepository extends JpaRepository<CafeJpaEntity, Long> {

    // % 연산자 사용 — GIN(gin_trgm_ops) 인덱스가 % 연산자만 지원하며 similarity() 함수 형태는 인덱스 미사용
    @Query(value = """
            SELECT * FROM cafes
            WHERE name % :keyword
            ORDER BY name <-> :keyword
            LIMIT :limit
            """, nativeQuery = true)
    List<CafeJpaEntity> findByNameSimilarity(@Param("keyword") String keyword, @Param("limit") int limit);
}
