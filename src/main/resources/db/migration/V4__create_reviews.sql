CREATE TABLE reviews (
    id         BIGSERIAL PRIMARY KEY,
    cafe_id    BIGINT    NOT NULL REFERENCES cafes(id) ON DELETE CASCADE,
    user_id    BIGINT    NOT NULL REFERENCES users(id),
    content    TEXT      NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 카페별 리뷰 목록 조회 최적화
CREATE INDEX idx_reviews_cafe_id ON reviews(cafe_id);
