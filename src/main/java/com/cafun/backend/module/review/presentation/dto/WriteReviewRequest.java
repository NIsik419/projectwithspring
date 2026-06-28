package com.cafun.backend.module.review.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WriteReviewRequest(
        @NotNull Long cafeId,
        @NotBlank String content
) {}
