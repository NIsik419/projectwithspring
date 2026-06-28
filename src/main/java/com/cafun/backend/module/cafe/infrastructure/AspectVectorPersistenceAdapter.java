package com.cafun.backend.module.cafe.infrastructure;

import com.cafun.backend.module.cafe.application.port.AspectVectorPort;
import com.cafun.backend.module.cafe.infrastructure.CafeAspectVectorJpaRepository.AspectVectorRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
class AspectVectorPersistenceAdapter implements AspectVectorPort {

    private final CafeAspectVectorJpaRepository jpaRepository;

    @Override
    public Map<Long, float[]> findAll() {
        return jpaRepository.findAllAsText().stream()
                .collect(Collectors.toMap(AspectVectorRow::getCafeId,
                        row -> parseVector(row.getVectors())));
    }

    @Override
    public Optional<float[]> findByCafeId(Long cafeId) {
        return jpaRepository.findByCafeIdAsText(cafeId)
                .map(row -> parseVector(row.getVectors()));
    }

    @Override
    @Transactional
    public void upsert(Long cafeId, float[] vectors) {
        jpaRepository.upsert(cafeId, toVectorString(vectors));
    }

    // PostgreSQL text 표현 "{0.1,0.2,...}" → float[]
    private float[] parseVector(String text) {
        String inner = text.substring(1, text.length() - 1);
        String[] parts = inner.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }

    // float[] → "{0.1,0.2,...}" PostgreSQL array literal
    private String toVectorString(float[] vectors) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < vectors.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vectors[i]);
        }
        return sb.append("}").toString();
    }
}
