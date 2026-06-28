package com.cafun.backend.module.cafe.application;

import com.cafun.backend.module.cafe.domain.Cafe;

import java.util.List;

public interface GetCafeListUseCase {
    List<Cafe> getCafeList();
}
