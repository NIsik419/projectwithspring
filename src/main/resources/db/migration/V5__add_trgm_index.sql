-- pg_trgm GIN 인덱스: cafes.name 유사도 검색
-- 검증: EXPLAIN ANALYZE SELECT * FROM cafes WHERE similarity(name, '스타벅스') > 0.3;
--       → "Bitmap Index Scan on idx_cafes_name_trgm" 확인
CREATE INDEX idx_cafes_name_trgm ON cafes USING GIN (name gin_trgm_ops);
