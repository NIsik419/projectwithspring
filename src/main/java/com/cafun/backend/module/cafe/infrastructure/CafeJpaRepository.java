package com.cafun.backend.module.cafe.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface CafeJpaRepository extends JpaRepository<CafeJpaEntity, Long> {

    // GIN 인덱스(idx_cafes_name_trgm) 활용 — similarity 함수는 pg_trgm 확장 필요
    @Query(value = """
            SELECT * FROM cafes
            WHERE similarity(name, :keyword) > 0.2
            ORDER BY similarity(name, :keyword) DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<CafeJpaEntity> findByNameSimilarity(@Param("keyword") String keyword, @Param("limit") int limit);
}
