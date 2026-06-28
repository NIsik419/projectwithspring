package com.cafun.backend.module.cafe.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface CafeAspectVectorJpaRepository extends JpaRepository<CafeAspectVectorJpaEntity, Long> {

    // float[] → text cast: "{0.1,0.2,...}" 형식으로 반환 (JPA 타입 매핑 우회)
    @Query(value = "SELECT cafe_id as cafeId, vectors::text as vectors FROM cafe_aspect_vectors", nativeQuery = true)
    List<AspectVectorRow> findAllAsText();

    @Query(value = "SELECT cafe_id as cafeId, vectors::text as vectors FROM cafe_aspect_vectors WHERE cafe_id = :cafeId", nativeQuery = true)
    Optional<AspectVectorRow> findByCafeIdAsText(@Param("cafeId") Long cafeId);

    @Modifying
    @Query(value = """
            INSERT INTO cafe_aspect_vectors (cafe_id, vectors, updated_at)
            VALUES (:cafeId, CAST(:vectors AS float[]), NOW())
            ON CONFLICT (cafe_id) DO UPDATE
            SET vectors = CAST(:vectors AS float[]), updated_at = NOW()
            """, nativeQuery = true)
    void upsert(@Param("cafeId") Long cafeId, @Param("vectors") String vectors);

    interface AspectVectorRow {
        Long getCafeId();
        String getVectors();
    }
}
