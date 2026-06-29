# cafun-backend

성수동 카페 304곳의 리뷰를 DualHeadBERT ABSA 모델로 분석해 aspect 기반 추천을 제공하는 Spring Boot 백엔드.

NestJS + 헥사고날 → Spring Boot + 모듈러 모놀리스 + 클린 아키텍처로 마이그레이션.

---

## 기술 스택

- Java 17, Spring Boot 4.1.0
- Spring Data JPA + Spring Security
- PostgreSQL 15 + pg_trgm
- Flyway, TestContainers, Docker

---

## 실행

```bash
docker compose up -d
./gradlew bootRun
```

---

## 성능 측정

### 추천 API — 인메모리 코사인 유사도

앱 시작 시 304개 카페의 aspect 벡터(12차원)를 메모리에 로드하고, 요청마다 전체 코사인 유사도를 정렬한다.

| 지표 | 수치 |
|------|------|
| 처리량 | 337 req/s (50 VU, 30s) |
| p95 응답시간 | 70ms |
| 에러율 | 0% |

> **참고** — 측정 당시 `cafe_aspect_vectors` 미적재 상태(빈 캐시)에서 p95 20ms였고,
> 실 벡터 304개 로드 후 p95 70ms로 올라갔다. 50ms 차이가 코사인 유사도 계산 비용이다.

---

### pg_trgm 검색 최적화

카페 이름 유사도 검색(`GET /cafe?name=...`)에 GIN 인덱스(`idx_cafes_name_trgm`)를 사용한다.

#### 발견한 문제

`similarity()` **함수** 형태는 GIN 인덱스를 구조적으로 사용할 수 없다.
`enable_seqscan = off`로 강제해도 Seq Scan — 플래너 선택의 문제가 아니라 인덱스가 해당 연산을 지원하지 않는다.

```sql
-- 수정 전: 인덱스 사용 불가 (규모 무관, 항상 풀스캔)
WHERE similarity(name, :keyword) > 0.2

-- 수정 후: % 연산자 → Bitmap Index Scan on idx_cafes_name_trgm
WHERE name % :keyword
ORDER BY name <-> :keyword
```

#### 10,000행 벤치마크 (자연 실행, 인덱스 효과 측정용)

실 데이터 304행에서는 플래너가 Seq Scan을 올바르게 선택한다(풀스캔이 더 빠른 규모).
인덱스 우위를 확인하기 위해 10,000행으로 측정했다.

| 키워드 | 수정 전 (Seq Scan) | 수정 후 (Bitmap Index Scan) | 속도 향상 |
|--------|--------------------|------------------------------|-----------|
| 커피   | 20.2 ms            | 5.8 ms                       | 3.5×      |
| 이디야 | 19.3 ms            | 8.6 ms                       | 2.2×      |

> 데이터가 클수록 격차가 벌어진다. 실 서비스 규모(수만 행 이상)에서 인덱스 효과가 두드러진다.

#### 임계값 조정 (0.3 → 0.2)

`%` 연산자 기본 임계값은 0.3이지만 실제 검색에서 결과 누락이 발생했다.

| 키워드 | similarity > 0.2 | % (기본 0.3) | 누락 |
|--------|:----------------:|:------------:|------|
| 스타벅스 | 3 | 3 | 없음 |
| 커피     | 4 | 3 | 커피빈 성수점 |
| 이디야   | 1 | 0 | 이디야커피 성수역점 (유일한 정답) |

`ALTER DATABASE cafun SET pg_trgm.similarity_threshold = 0.2`를 V6 마이그레이션에 추가해 DB 기본값으로 설정했다.

---

### 부하테스트 (k6)

**조건**: 50 VU, 30초, 로컬 Docker 환경, 실 카페 데이터 304개 적재 상태

| API | avg | p95 | p99 | 에러율 |
|-----|-----|-----|-----|--------|
| GET /cafe/search (추천) | 21 ms | 70 ms | 143 ms | 0% |
| GET /cafe/name-search (pg_trgm) | 21 ms | 70 ms | 135 ms | 0% |
| POST /auth/login | 103 ms | 160 ms | 224 ms | 0% |

전체 처리량: **337 req/s**, 전체 p95: **125 ms**, 에러율: **0%**
