package com.cafun.backend.module.review.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface ReviewJpaRepository extends JpaRepository<ReviewJpaEntity, Long> {
    List<ReviewJpaEntity> findByCafeIdOrderByCreatedAtDesc(Long cafeId);
}
