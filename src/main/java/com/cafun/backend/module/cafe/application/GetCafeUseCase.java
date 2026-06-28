package com.cafun.backend.module.cafe.application;

import com.cafun.backend.module.cafe.domain.Cafe;

public interface GetCafeUseCase {
    Cafe getCafe(Long id);
}
