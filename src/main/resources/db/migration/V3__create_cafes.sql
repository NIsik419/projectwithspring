CREATE TABLE cafes (
    id          BIGSERIAL        PRIMARY KEY,
    name        VARCHAR(255)     NOT NULL,
    address     VARCHAR(500)     NOT NULL,
    phone       VARCHAR(50),
    latitude    DOUBLE PRECISION NOT NULL DEFAULT 0,
    longitude   DOUBLE PRECISION NOT NULL DEFAULT 0,
    image_url   VARCHAR(1000),
    created_at  TIMESTAMP        NOT NULL DEFAULT NOW()
);

-- DualHeadBERT ABSA 모델이 생성하는 12개 aspect 점수 벡터
-- float[] 타입 사용 (hypersistence-utils 없이 @Column(columnDefinition) 로 매핑, Part 5에서 구현)
CREATE TABLE cafe_aspect_vectors (
    cafe_id    BIGINT    NOT NULL REFERENCES cafes(id) ON DELETE CASCADE PRIMARY KEY,
    vectors    float[]   NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
