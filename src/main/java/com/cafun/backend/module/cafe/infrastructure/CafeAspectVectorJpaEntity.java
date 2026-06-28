package com.cafun.backend.module.cafe.infrastructure;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

// float[] 필드는 native query로만 접근 — JPA 타입 매핑 문제 우회
@Entity
@Table(name = "cafe_aspect_vectors")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class CafeAspectVectorJpaEntity {

    @Id
    @Column(name = "cafe_id")
    private Long cafeId;
}
