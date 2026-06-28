package com.cafun.backend.module.cafe.infrastructure;

import com.cafun.backend.module.cafe.domain.Cafe;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "cafes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class CafeJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    private String phone;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    private String imageUrl;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private CafeJpaEntity(Long id, String name, String address, String phone,
                          double latitude, double longitude, String imageUrl, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
    }

    Cafe toDomain() {
        return Cafe.builder()
                .id(this.id)
                .name(this.name)
                .address(this.address)
                .phone(this.phone)
                .latitude(this.latitude)
                .longitude(this.longitude)
                .imageUrl(this.imageUrl)
                .createdAt(this.createdAt)
                .aspectScores(null)  // 파트 5에서 인메모리 캐시가 병합
                .build();
    }
}
