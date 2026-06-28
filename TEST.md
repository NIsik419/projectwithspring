# TEST.md — cafun 테스트 보강 & 성능 측정 가이드

---

## 테스트 보강 우선순위

| 순위 | 대상 | 방식 | 이유 |
|------|------|------|------|
| 1 | 추천 알고리즘 (파트 5) | TestContainers | 핵심 비즈니스 로직 |
| 2 | Auth 흐름 (파트 2) | TestContainers | 보안 핵심 |
| 3 | Cafe/Review CRUD | test.http | 단순 CRUD, 통합테스트 불필요 |

---

## 1. 추천 알고리즘 테스트 케이스

> 작업 범위: `src/main/java/com/cafun/backend/module/cafe/application/`

| # | 케이스 | 검증 포인트 |
|---|--------|-----------|
| 1 | 앱 시작 시 벡터 캐시 로딩 | 304개 카페 전부 로드, vector 길이 == 12 |
| 2 | aspect 필터 적용 추천 | 선택한 aspect 점수 높은 순 정렬 |
| 3 | 키워드 점수 반영 | Math.log1p(count) * 0.5 가중치 정상 적용 |
| 4 | 리뷰 작성 후 캐시 동기화 | 새 리뷰 → vector 재계산 → 캐시 갱신 확인 |
| 5 | 빈 aspectVector 입력 | 빈 배열 or null → 기본 정렬 또는 400 반환 |

---

## 2. Auth 테스트 케이스

> 작업 범위: `src/main/java/com/cafun/backend/module/auth/`, `global/config/SecurityConfig.java`

| # | 케이스 | 기대 결과 |
|---|--------|---------|
| 1 | 정상 로그인 | 200, accessToken 포함 |
| 2 | 존재하지 않는 이메일 | 401 |
| 3 | 비밀번호 불일치 | 401 |
| 4 | 발급된 JWT로 인증 필요 API 호출 | 200 |
| 5 | 잘못된 JWT 헤더 | 401 |
| 6 | JWT 없이 인증 필요 API 호출 | 401 |
| 7 | permitAll 경로 미인증 접근 | 200 (토큰 없어도 통과) |

---

## 3. 성능 측정

### k6 부하테스트 대상
```
GET  /cafe/search?aspectVector=...   ← 추천 API (인메모리, 핵심)
GET  /cafe?name=...                  ← pg_trgm 검색
POST /auth/login                     ← Auth
```

### 측정 기준선 (로컬 Docker 기준)
- 목표: p95 응답시간 < 200ms
- 동시 사용자: 50~100 VU
- 측정 후 README에 그래프/수치 기록

### EXPLAIN ANALYZE 확인 대상
```sql
-- pg_trgm 인덱스 사용 여부 확인
EXPLAIN ANALYZE
SELECT * FROM cafes
WHERE name %> '스타벅스'
ORDER BY name <->> '스타벅스'
LIMIT 10;
```

---

## 4. 트러블슈팅 기록 (측정하면서 추가)

### k6 부하테스트 결과 기록 템플릿
> 측정 후 아래 표에 수치 채워넣기

| API | avg (ms) | p95 (ms) | p99 (ms) | 목표 p95 | 통과 |
|-----|----------|----------|----------|----------|------|
| GET /cafe/search | — | — | — | < 200ms | ⬜ |
| GET /cafe/name-search | — | — | — | < 200ms | ⬜ |
| POST /auth/login | — | — | — | < 200ms | ⬜ |

> 조건: 50 VU, 30s / 측정일: <!-- YYYY-MM-DD -->

<!-- 성능 측정 중 발견한 이슈 여기에 누적 -->

---

## 완료 기준

| 항목 | 기준 | 상태 |
|------|------|------|
| 추천 알고리즘 TestContainers | 5개 케이스 통과 | ✅ |
| Auth TestContainers | 8개 케이스 통과 (TC5 분리) | ✅ |
| k6 기준선 측정 | 3개 API 수치 기록 | ⬜ |
| EXPLAIN ANALYZE | 인덱스 사용 확인 | ⬜ |
| README 성능 수치 정리 | 최소 1개 개선 before/after | ⬜ |
