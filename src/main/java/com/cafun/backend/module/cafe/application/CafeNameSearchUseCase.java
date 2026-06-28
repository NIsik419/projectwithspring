package com.cafun.backend.module.cafe.application;

import com.cafun.backend.module.cafe.domain.Cafe;

import java.util.List;

public interface CafeNameSearchUseCase {
    List<Cafe> searchByName(String keyword, int limit);
}
