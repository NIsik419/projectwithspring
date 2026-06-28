package com.cafun.backend.module.cafe.presentation;

import com.cafun.backend.global.exception.BusinessException;
import com.cafun.backend.global.exception.ErrorCode;
import com.cafun.backend.global.response.ApiResponse;
import com.cafun.backend.module.cafe.application.CafeNameSearchUseCase;
import com.cafun.backend.module.cafe.application.CafeSearchUseCase;
import com.cafun.backend.module.cafe.application.GetCafeListUseCase;
import com.cafun.backend.module.cafe.application.GetCafeUseCase;
import com.cafun.backend.module.cafe.presentation.dto.CafeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cafe")
@RequiredArgsConstructor
public class CafeController {

    private final GetCafeListUseCase getCafeListUseCase;
    private final GetCafeUseCase getCafeUseCase;
    private final CafeSearchUseCase cafeSearchUseCase;
    private final CafeNameSearchUseCase cafeNameSearchUseCase;

    @GetMapping
    public ApiResponse<List<CafeResponse>> getCafeList() {
        return ApiResponse.success(getCafeListUseCase.getCafeList().stream()
                .map(CafeResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<CafeResponse> getCafe(@PathVariable Long id) {
        return ApiResponse.success(CafeResponse.from(getCafeUseCase.getCafe(id)));
    }

    // aspectVector: "0.8,0.2,..." 형식의 12개 float 값 (Aspect enum ordinal 순서)
    @GetMapping("/search")
    public ApiResponse<List<CafeResponse>> search(@RequestParam String aspectVector,
                                                   @RequestParam(defaultValue = "20") int limit) {
        float[] vector = parseAspectVector(aspectVector);
        return ApiResponse.success(cafeSearchUseCase.searchByAspectVector(vector, limit).stream()
                .map(CafeResponse::from).toList());
    }

    // pg_trgm GIN 인덱스 활용 — similarity > 0.2 카페 이름 유사도 검색
    @GetMapping("/name-search")
    public ApiResponse<List<CafeResponse>> searchByName(@RequestParam String q,
                                                         @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.success(cafeNameSearchUseCase.searchByName(q, limit).stream()
                .map(CafeResponse::from).toList());
    }

    private float[] parseAspectVector(String raw) {
        String[] parts = raw.split(",");
        if (parts.length != 12) throw new BusinessException(ErrorCode.INVALID_INPUT);
        float[] vector = new float[12];
        try {
            for (int i = 0; i < 12; i++) vector[i] = Float.parseFloat(parts[i].trim());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return vector;
    }
}
