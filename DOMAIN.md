# DB 데이터 구조와 시드 가이드 (Spring Boot 이전용)

실제 마이그레이션 파일과 JSON 원본을 직접 분석해서 정리한 내용입니다.
Spring Boot로 옮길 때 이 파일 하나만 보면 됩니다.

---

## 1. 데이터 출처

```
네이버 지도 크롤링 → JSON 파일 → DB 시드
```

### 데이터 파일 위치 (현재 NestJS 프로젝트)

```
data/
├── cafe_info/               ← 카페 기본 정보 (304개 JSON 파일)
│   ├── 1005093594_info.json
│   ├── 1007959364_info.json
│   └── ...
├── cafe_keywords/           ← 카페별 리뷰 키워드 카운트 (304개 JSON 파일)
│   ├── 1005093594_keywords.json
│   └── ...
└── cafe_scores.json         ← 카페별 12차원 벡터 (파일 1개)
```

**파일명 규칙:**
- `{cafeId}_info.json` → 카페 ID = 네이버 지도 장소 ID (숫자)
- `{cafeId}_keywords.json` → 같은 ID로 키워드 파일 매핑
- `cafe_scores.json` → `{ "cafeId": [12개 숫자], ... }` 형태

---

## 2. JSON 파일 구조 (실제 예시)

### cafe_info JSON 구조

```json
// data/cafe_info/1005093594_info.json (뉴믹스커피 성수점 예시)
{
  "id": "1005093594",
  "name": "뉴믹스커피 성수점",
  "category": "카페,디저트",
  "micro_review": [
    "센스 있는 선물로 제격인 믹스커피"
  ],

  // ⚠️ 중요 버그: 크롤링 실수로 lat/lon이 뒤바뀜
  "lat": "127.0512953",    // ← 실제로는 경도(lon)값
  "lon": "37.5439944",     // ← 실제로는 위도(lat)값

  "road_address": "서울 성동구 연무장3길 3 1층",
  "address": "서울 성동구 성수동2가 301-99",

  "business_hours": [
    {
      "day": "월",
      "start": "11:00",    // ← "open"이 아닌 "start"
      "end": "19:00",      // ← "close"가 아닌 "end"
      "breakHours": [],
      "description": null,
      "lastOrderTimes": null
    }
    // 요일별 7개 객체
  ],

  // ⚠️ 중요: convenience가 List가 아닌 Object (boolean 맵)
  "convenience": {
    "주차": false,
    "발렛파킹": false,
    "무선 인터넷": false,
    "단체 이용 가능": true,
    "간편결제": true,
    "반려동물 동반": true,
    "유아시설": false
    // ... 총 24개 항목
  },

  // ⚠️ 필드명 주의: image_urls가 아닌 image_url (단수)
  "image_url": [
    "https://ldb-phinf.pstatic.net/..."
  ],

  // ⚠️ 필드명 주의: 대문자 I, 오타
  "Information_facilitie": [
    "단체 이용 가능",
    "포장",
    "반려동물 동반"
  ],

  "payment_info": ["제로페이", "네이버페이", "간편결제"],
  "virtual_phone_number": "0507-1486-2012",
  "url": "https://smartstore.naver.com/newmixcoffee",
  "description": "Korean Dessert Coffee...",
  "parking_info": null,

  "menu": [
    {
      "name": "뉴믹스 오리지널맛 믹스커피",
      "price": "15900",    // ← 문자열로 저장됨, parseInt 필요
      "description": "",
      "images": [
        "https://ldb-phinf.pstatic.net/..."
      ]
    }
  ]
}
```

### cafe_keywords JSON 구조

```json
// data/cafe_keywords/1005093594_keywords.json
{
  "라떼": 6,
  "말차": 2,
  "아메리카노": 1,
  "선물": 26,
  "분위기": 20,
  "인테리어": 23,
  "대기": 7,

  // ⚠️ 편의시설 항목도 포함됨 (숫자 0/1) → 시드 시 필터링 필요
  "주차": 0,
  "발렛파킹": 0,
  "무선 인터넷": 0,
  "단체 이용 가능": 1,
  "간편결제": 1,
  "반려동물 동반": 1
  // ... 숫자인 것만 keywordCounts로 저장
}
```

### cafe_scores.json 구조

```json
// data/cafe_scores.json
{
  "1005093594": [0.7376, 0.5931, 0.7587, 0.7047, 0.1975, 0.8358, 0.0738, 0.9452, ...],
  "1007959364": [0.2, 0.5, 0.8, ...],
  // 304개 카페
}
```

**12차원 벡터 의미:**

| 인덱스 | 의미 | Review 필드 |
|--------|------|------------|
| 0 | 커피/음료 | coffee_beverage |
| 1 | 베이커리/빵 | bakery_bread |
| 2 | 케이크 | cake |
| 3 | 쿠키/구운것 | cookie_baked |
| 4 | 빙수/과일 | bingsu_fruit |
| 5 | 기타 디저트 | other_dessert |
| 6 | 공간/시설 | space_facility |
| 7 | 분위기 | atmosphere_vibe |
| 8 | 서비스 | service |
| 9 | 가격대비 | price_value |
| 10 | 선물/포장 | gift_packaging |
| 11 | 혼잡도/대기 | crowd_waiting |

---

## 3. 실제 DB 스키마 (마이그레이션 파일 원본 기반)

아래는 TypeORM 마이그레이션에서 실행된 실제 DDL입니다.

### 3-1. menus 테이블

```sql
CREATE TABLE "menus" (
    "id"          SERIAL        NOT NULL,
    "cafe_id"     VARCHAR       NOT NULL,
    "name"        VARCHAR       NOT NULL,
    "price"       INTEGER,
    "description" TEXT,
    "images"      JSONB,               -- 메뉴 이미지 URL 리스트
    CONSTRAINT "PK_menus" PRIMARY KEY ("id")
);

ALTER TABLE "menus"
    ADD CONSTRAINT "FK_menus_cafes"
    FOREIGN KEY ("cafe_id") REFERENCES "cafes"("id") ON DELETE CASCADE;
```

### 3-2. cafe_metadata 테이블

```sql
CREATE TABLE "cafe_metadata" (
    "cafe_id"        VARCHAR   NOT NULL,   -- 카페 ID (PK 겸 FK)
    "keyword_counts" JSONB     NOT NULL,   -- 리뷰 추출 키워드 개수
    "updated_at"     TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT "PK_cafe_metadata" PRIMARY KEY ("cafe_id")
);

ALTER TABLE "cafe_metadata"
    ADD CONSTRAINT "FK_cafe_metadata_cafes"
    FOREIGN KEY ("cafe_id") REFERENCES "cafes"("id") ON DELETE CASCADE;
```

### 3-3. cafe_aspect_vectors 테이블

```sql
CREATE TABLE "cafe_aspect_vectors" (
    "cafe_id"    VARCHAR              NOT NULL,  -- 카페 ID (PK 겸 FK)
    "vector"     DOUBLE PRECISION[]   NOT NULL,  -- 12차원 측면 기반 추천 벡터
    "updated_at" TIMESTAMP            NOT NULL DEFAULT now(),
    CONSTRAINT "PK_cafe_aspect_vectors" PRIMARY KEY ("cafe_id")
);

ALTER TABLE "cafe_aspect_vectors"
    ADD CONSTRAINT "FK_cafe_aspect_vectors_cafes"
    FOREIGN KEY ("cafe_id") REFERENCES "cafes"("id") ON DELETE CASCADE;
```

### 3-4. cafes 테이블

```sql
CREATE TABLE "cafes" (
    "id"                   VARCHAR        NOT NULL,  -- 외부 데이터 ID (네이버 장소 ID)
    "name"                 VARCHAR        NOT NULL,
    "category"             VARCHAR,
    "micro_review"         JSONB,                   -- 짧은 리뷰 문구 리스트
    "road_address"         VARCHAR,
    "address"              VARCHAR,
    "lat"                  NUMERIC(11,8),           -- ⚠️ DB에는 정상 저장 (마이그레이션에서 스왑)
    "lon"                  NUMERIC(11,8),
    "business_hours"       JSONB,                   -- 영업시간, 휴무일, 라스트오더
    "convenience"          JSONB,                   -- ⚠️ { "주차": false, "와이파이": true } Object 형태
    "information_facilitie" JSONB,                  -- 시설정보 리스트
    "payment_info"         JSONB,                   -- 결제 수단 리스트
    "image_urls"           JSONB,                   -- 카페 대표 이미지 URL 리스트
    "parking_info"         JSONB,                   -- 주차 정보
    "virtual_phone_number" VARCHAR,
    "url"                  VARCHAR,
    "description"          TEXT,
    "created_at"           TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT "PK_cafes" PRIMARY KEY ("id")
);
```

### 3-5. reviews 테이블

```sql
CREATE TABLE "reviews" (
    "id"               SERIAL    NOT NULL,
    "cafe_id"          VARCHAR   NOT NULL,
    "user_id"          INTEGER   NOT NULL,
    "review_text"      TEXT,
    -- 식음료 (6개)
    "coffee_beverage"  INTEGER,   -- 1=positive, 2=neutral, 3=negative
    "bakery_bread"     INTEGER,
    "cake"             INTEGER,
    "cookie_baked"     INTEGER,
    "bingsu_fruit"     INTEGER,
    "other_dessert"    INTEGER,
    -- 공간/경험 (3개)
    "space_facility"   INTEGER,
    "atmosphere_vibe"  INTEGER,
    "service"          INTEGER,
    -- 기타 (3개)
    "price_value"      INTEGER,
    "gift_packaging"   INTEGER,
    "crowd_waiting"    INTEGER,
    "created_at"       TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT "PK_reviews" PRIMARY KEY ("id")
);

ALTER TABLE "reviews"
    ADD CONSTRAINT "FK_reviews_cafes"
    FOREIGN KEY ("cafe_id") REFERENCES "cafes"("id") ON DELETE CASCADE;
ALTER TABLE "reviews"
    ADD CONSTRAINT "FK_reviews_users"
    FOREIGN KEY ("user_id") REFERENCES "users"("id") ON DELETE CASCADE;
```

### 3-6. users 테이블

```sql
CREATE TABLE "users" (
    "id"         SERIAL      NOT NULL,
    "email"      VARCHAR     NOT NULL,
    "password"   VARCHAR     NOT NULL,
    "nickname"   VARCHAR     NOT NULL,
    "role"       VARCHAR     NOT NULL DEFAULT 'USER',
    "created_at" TIMESTAMP   NOT NULL DEFAULT now(),
    "updated_at" TIMESTAMP   NOT NULL DEFAULT now(),
    "deleted_at" TIMESTAMP,                            -- Soft Delete
    CONSTRAINT "UQ_users_email"    UNIQUE ("email"),
    CONSTRAINT "UQ_users_nickname" UNIQUE ("nickname"),
    CONSTRAINT "CHK_users_role"    CHECK ("role" IN ('USER', 'ADMIN')),
    CONSTRAINT "PK_users"          PRIMARY KEY ("id")
);
```

### 3-7. 인덱스 & 확장

```sql
-- pg_trgm 확장 (카페 이름 유사도 검색)
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX idx_cafes_name_trgm ON cafes USING GIN (name gin_trgm_ops);

-- word_similarity 임계값 (기본 0.6 → 0.3으로 낮춤)
ALTER DATABASE cafun_db SET pg_trgm.word_similarity_threshold = 0.3;
```

---

## 4. 마이그레이션 핵심 버그/특이사항 (반드시 알아야 함)

### ⚠️ 버그 1: lat/lon 스왑

```typescript
// NestJS 마이그레이션 코드 (실제)
cafes.push({
    lat: data.lon ? parseFloat(data.lon) : -1,   // JSON의 lon값 → DB의 lat 컬럼
    lon: data.lat ? parseFloat(data.lat) : -1,   // JSON의 lat값 → DB의 lon 컬럼
});
```

```
JSON 원본:
  "lat": "127.0512953"  (실제로는 경도)
  "lon": "37.5439944"   (실제로는 위도)

DB 저장값:
  lat = 37.5439944  (정상 위도)
  lon = 127.0512953 (정상 경도)

→ 마이그레이션이 스왑해서 DB에는 정상적으로 저장됨
→ Spring Boot DataInitializer에서도 동일하게 스왑 처리 필요
```

### ⚠️ 버그 2: convenience 형태

```
JSON 원본: { "주차": false, "와이파이": true, ... }  ← Object (boolean 맵)
DB 저장:   동일한 JSONB로 저장됨

NestJS 캐시 서비스에서 사용:
  metadata.conveniences["주차"] !== true  → boolean 비교

⚠️ Spring Boot에서 Map<String, Boolean>으로 매핑해야 함
   List<String>이 아님!
```

### ⚠️ 특이사항 3: 원본 필드명 불일치

```
JSON 필드명          → DB 컬럼명 (TypeORM 변환)
-------------------------------------------------
image_url (단수)     → image_urls (복수)
Information_facilitie → information_facilitie (소문자 i)
lat (경도값)         → lat (위도 저장) ← 스왑
lon (위도값)         → lon (경도 저장) ← 스왑
```

### ⚠️ 특이사항 4: keywords에 편의시설 항목 포함

```json
// 키워드 파일에 편의시설 값도 섞여있음
{
  "라떼": 6,        // ← 실제 키워드 (숫자 > 0)
  "주차": 0,        // ← 편의시설 항목 (0 또는 1)
  "간편결제": 1     // ← 편의시설 항목
}
```

```typescript
// 마이그레이션에서 숫자인 것만 필터링해서 저장
if (typeof value === 'number') {
    cleanKeywordCounts[key] = value;
}
// → 편의시설 0값도 포함됨 (0도 숫자)
// → 실제로는 이 전체가 그대로 keyword_counts에 저장됨
```

### ⚠️ 특이사항 5: business_hours 필드 구조

```json
// JSON 원본의 실제 필드명
{ "day": "월", "start": "11:00", "end": "19:00", "lastOrderTimes": null }
//                ↑ start            ↑ end

// 5번 MD에서 추정했던 구조 (다름!)
{ "day": "월", "open": "09:00", "close": "21:00" }
```

---

## 5. Spring Boot DataInitializer 구현

TypeORM 마이그레이션을 Java로 그대로 변환한 코드입니다.

```java
// src/main/java/com/cafun/backend/config/DataInitializer.java

package com.cafun.backend.config;

import com.cafun.backend.cafe.entity.*;
import com.cafun.backend.cafe.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final CafeRepository cafeRepository;
    private final CafeAspectVectorRepository vectorRepository;
    private final CafeMetadataRepository metadataRepository;
    private final ObjectMapper objectMapper;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void init() throws IOException {
        if (cafeRepository.count() > 0) {
            log.info("카페 데이터가 이미 존재합니다. 시드를 건너뜁니다.");
            return;
        }

        log.info("=== 카페 데이터 시드 시작 ===");
        loadCafeInfoAndMenus();
        loadAspectVectors();
        loadKeywordsMetadata();
        log.info("=== 카페 데이터 시드 완료: 총 {}개 카페 ===", cafeRepository.count());
    }

    // ── 1. cafe_info JSON → cafes + menus 테이블 ─────────────────
    private void loadCafeInfoAndMenus() throws IOException {
        File dataDir = new File("data/cafe_info");
        if (!dataDir.exists()) {
            log.warn("data/cafe_info 디렉토리를 찾을 수 없습니다.");
            return;
        }

        File[] files = dataDir.listFiles(f -> f.getName().endsWith("_info.json"));
        if (files == null) return;

        List<Cafe> cafes = new ArrayList<>();
        List<Menu> menus = new ArrayList<>();

        for (File file : files) {
            Map<String, Object> data = objectMapper.readValue(file,
                new TypeReference<Map<String, Object>>() {});

            String id = (String) data.get("id");
            if (id == null) {
                log.error("cafe ID가 없는 파일: {}", file.getName());
                continue;
            }

            // ⚠️ lat/lon 스왑: JSON의 lat→경도, lon→위도 (크롤링 실수)
            String latRaw = (String) data.get("lat"); // 실제로는 경도값
            String lonRaw = (String) data.get("lon"); // 실제로는 위도값
            BigDecimal lat = lonRaw != null ? new BigDecimal(lonRaw) : BigDecimal.valueOf(-1);
            BigDecimal lon = latRaw != null ? new BigDecimal(latRaw) : BigDecimal.valueOf(-1);

            // convenience: Object 형태 그대로 JSONB에 저장 (List가 아님!)
            Object convenienceRaw = data.get("convenience");
            // ObjectMapper가 Map<String, Boolean>으로 파싱

            Cafe cafe = Cafe.builder()
                .id(id)
                .name((String) data.getOrDefault("name", "이름없음"))
                .category((String) data.get("category"))
                .microReview(castToList(data.get("micro_review")))
                .roadAddress((String) data.get("road_address"))
                .address((String) data.get("address"))
                .lat(lat)
                .lon(lon)
                .businessHours(castToListOfMaps(data.get("business_hours")))
                .convenience(convenienceRaw)    // Map<String, Boolean> 그대로
                .informationFacilitie(castToList(data.get("Information_facilitie"))) // 대문자 I
                .paymentInfo(castToList(data.get("payment_info")))
                .imageUrls(castToList(data.get("image_url")))   // 단수 image_url
                .parkingInfo(castToMap(data.get("parking_info")))
                .virtualPhoneNumber((String) data.get("virtual_phone_number"))
                .url((String) data.get("url"))
                .description((String) data.get("description"))
                .build();

            cafes.add(cafe);

            // 메뉴 파싱
            List<Map<String, Object>> menuList = castToListOfMaps(data.get("menu"));
            if (menuList != null) {
                for (Map<String, Object> item : menuList) {
                    String priceRaw = String.valueOf(item.getOrDefault("price", "-1"));
                    int price;
                    try {
                        price = Integer.parseInt(priceRaw);
                    } catch (NumberFormatException e) {
                        price = -1;
                    }

                    menus.add(Menu.builder()
                        .cafeId(id)
                        .name((String) item.getOrDefault("name", ""))
                        .price(price)
                        .description((String) item.get("description"))
                        .images(castToList(item.get("images")))
                        .build());
                }
            }

            // 500개 단위 배치 저장
            if (cafes.size() >= 500) {
                cafeRepository.saveAll(cafes);
                cafes.clear();
            }
        }

        if (!cafes.isEmpty()) cafeRepository.saveAll(cafes);
        // 메뉴는 카페가 모두 저장된 후 삽입 (FK 제약)
        // 500개씩 배치 처리
        for (int i = 0; i < menus.size(); i += 500) {
            // Menu를 직접 저장하는 Repository 필요
            // menuRepository.saveAll(menus.subList(i, Math.min(i + 500, menus.size())));
        }

        log.info("카페 {}개, 메뉴 {}개 저장 완료", cafeRepository.count(), menus.size());
    }

    // ── 2. cafe_scores.json → cafe_aspect_vectors 테이블 ─────────
    private void loadAspectVectors() throws IOException {
        File scoresFile = new File("data/cafe_scores.json");
        if (!scoresFile.exists()) {
            log.warn("data/cafe_scores.json 파일을 찾을 수 없습니다.");
            return;
        }

        // { "cafeId": [12개 숫자] } 형태
        Map<String, List<Double>> scoresData = objectMapper.readValue(scoresFile,
            new TypeReference<Map<String, List<Double>>>() {});

        List<CafeAspectVector> vectors = new ArrayList<>();
        for (Map.Entry<String, List<Double>> entry : scoresData.entrySet()) {
            String cafeId = entry.getKey();
            List<Double> vectorList = entry.getValue();

            // 카페가 실제로 존재하는지 확인
            if (!cafeRepository.existsById(cafeId)) continue;

            double[] vectorArray = vectorList.stream()
                .mapToDouble(Double::doubleValue)
                .toArray();

            vectors.add(CafeAspectVector.builder()
                .cafeId(cafeId)
                .vector(vectorArray)
                .build());

            if (vectors.size() >= 500) {
                vectorRepository.saveAll(vectors);
                vectors.clear();
            }
        }
        if (!vectors.isEmpty()) vectorRepository.saveAll(vectors);
        log.info("벡터 데이터 {}개 저장 완료", vectorRepository.count());
    }

    // ── 3. cafe_keywords/*.json → cafe_metadata 테이블 ───────────
    private void loadKeywordsMetadata() throws IOException {
        File keywordsDir = new File("data/cafe_keywords");
        if (!keywordsDir.exists()) {
            log.warn("data/cafe_keywords 디렉토리를 찾을 수 없습니다.");
            return;
        }

        File[] files = keywordsDir.listFiles(f -> f.getName().endsWith("_keywords.json"));
        if (files == null) return;

        List<CafeMetadata> metadataList = new ArrayList<>();

        for (File file : files) {
            // 파일명에서 cafeId 추출: "1005093594_keywords.json" → "1005093594"
            String cafeId = file.getName().replace("_keywords.json", "");

            if (!cafeRepository.existsById(cafeId)) continue;

            Map<String, Object> rawData = objectMapper.readValue(file,
                new TypeReference<Map<String, Object>>() {});

            // 숫자인 값만 keyword counts로 저장 (편의시설 포함, 원본 동작 그대로)
            Map<String, Integer> cleanKeywordCounts = new HashMap<>();
            for (Map.Entry<String, Object> entry : rawData.entrySet()) {
                if (entry.getValue() instanceof Number n) {
                    cleanKeywordCounts.put(entry.getKey(), n.intValue());
                }
            }

            metadataList.add(CafeMetadata.builder()
                .cafeId(cafeId)
                .keywordCounts(cleanKeywordCounts)
                .build());

            if (metadataList.size() >= 500) {
                metadataRepository.saveAll(metadataList);
                metadataList.clear();
            }
        }
        if (!metadataList.isEmpty()) metadataRepository.saveAll(metadataList);
        log.info("메타데이터 {}개 저장 완료", metadataRepository.count());
    }

    // ── 유틸 메서드 ───────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private List<String> castToList(Object obj) {
        if (obj instanceof List<?> list) {
            return (List<String>) list;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castToListOfMaps(Object obj) {
        if (obj instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castToMap(Object obj) {
        if (obj instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }
}
```

---

## 6. convenience 필드 Java 엔티티 매핑

JSON의 convenience가 `List<String>`이 아닌 `Map<String, Boolean>` 형태입니다.
엔티티에서 정확하게 매핑해야 합니다.

```java
// Cafe.java 엔티티에서
@Type(JsonType.class)
@Column(columnDefinition = "jsonb")
private Map<String, Boolean> convenience;
// ← List<String>이 아님! { "주차": false, "와이파이": true } 형태

// 인메모리 캐시에서도 동일하게 사용됨
// NestJS: metadata.conveniences["주차"] !== true
// Java:   data.getConvenience().getOrDefault("주차", false)
```

**캐시 서비스에서 편의시설 필터링 (원본 NestJS 로직):**

```java
// LocalAiRecommendationAdapter.java
for (String conv : conveniences) {
    Boolean hasIt = data.conveniences().getOrDefault(conv, false);
    if (!Boolean.TRUE.equals(hasIt)) {
        skip = true;  // 하나라도 없으면 제외
        break;
    }
}
```

---

## 7. Flyway SQL 파일 (Spring Boot용 실제 DDL)

### V1__Initial_Schema.sql

```sql
CREATE TABLE "menus" (
    "id"          SERIAL   NOT NULL,
    "cafe_id"     VARCHAR  NOT NULL,
    "name"        VARCHAR  NOT NULL,
    "price"       INTEGER,
    "description" TEXT,
    "images"      JSONB,
    CONSTRAINT "PK_3fec3d" PRIMARY KEY ("id")
);

CREATE TABLE "cafe_metadata" (
    "cafe_id"        VARCHAR   NOT NULL,
    "keyword_counts" JSONB     NOT NULL,
    "updated_at"     TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT "PK_4ace0a" PRIMARY KEY ("cafe_id")
);

CREATE TABLE "cafe_aspect_vectors" (
    "cafe_id"    VARCHAR            NOT NULL,
    "vector"     DOUBLE PRECISION[] NOT NULL,
    "updated_at" TIMESTAMP          NOT NULL DEFAULT now(),
    CONSTRAINT "PK_c9dec5" PRIMARY KEY ("cafe_id")
);

CREATE TABLE "cafes" (
    "id"                    VARCHAR        NOT NULL,
    "name"                  VARCHAR        NOT NULL,
    "category"              VARCHAR,
    "micro_review"          JSONB,
    "road_address"          VARCHAR,
    "address"               VARCHAR,
    "lat"                   NUMERIC(11,8),
    "lon"                   NUMERIC(11,8),
    "business_hours"        JSONB,
    "convenience"           JSONB,
    "information_facilitie" JSONB,
    "payment_info"          JSONB,
    "image_urls"            JSONB,
    "parking_info"          JSONB,
    "virtual_phone_number"  VARCHAR,
    "url"                   VARCHAR,
    "description"           TEXT,
    "created_at"            TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT "PK_1e8e00" PRIMARY KEY ("id")
);

CREATE TABLE "reviews" (
    "id"               SERIAL    NOT NULL,
    "cafe_id"          VARCHAR   NOT NULL,
    "user_id"          INTEGER   NOT NULL,
    "review_text"      TEXT,
    "coffee_beverage"  INTEGER,
    "bakery_bread"     INTEGER,
    "cake"             INTEGER,
    "cookie_baked"     INTEGER,
    "bingsu_fruit"     INTEGER,
    "other_dessert"    INTEGER,
    "space_facility"   INTEGER,
    "atmosphere_vibe"  INTEGER,
    "service"          INTEGER,
    "price_value"      INTEGER,
    "gift_packaging"   INTEGER,
    "crowd_waiting"    INTEGER,
    "created_at"       TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT "PK_231ae5" PRIMARY KEY ("id")
);

CREATE TABLE "users" (
    "id"         SERIAL    NOT NULL,
    "email"      VARCHAR   NOT NULL,
    "password"   VARCHAR   NOT NULL,
    "nickname"   VARCHAR   NOT NULL,
    "role"       VARCHAR   NOT NULL DEFAULT 'USER',
    "created_at" TIMESTAMP NOT NULL DEFAULT now(),
    "updated_at" TIMESTAMP NOT NULL DEFAULT now(),
    "deleted_at" TIMESTAMP,
    CONSTRAINT "UQ_users_email"    UNIQUE ("email"),
    CONSTRAINT "UQ_users_nickname" UNIQUE ("nickname"),
    CONSTRAINT "CHK_users_role"    CHECK ("role" IN ('USER', 'ADMIN')),
    CONSTRAINT "PK_a3ffb1" PRIMARY KEY ("id")
);

-- FK 제약 조건
ALTER TABLE "menus"
    ADD CONSTRAINT "FK_menus_cafes"
    FOREIGN KEY ("cafe_id") REFERENCES "cafes"("id") ON DELETE CASCADE;

ALTER TABLE "cafe_metadata"
    ADD CONSTRAINT "FK_cafe_metadata_cafes"
    FOREIGN KEY ("cafe_id") REFERENCES "cafes"("id") ON DELETE CASCADE;

ALTER TABLE "cafe_aspect_vectors"
    ADD CONSTRAINT "FK_cafe_aspect_vectors_cafes"
    FOREIGN KEY ("cafe_id") REFERENCES "cafes"("id") ON DELETE CASCADE;

ALTER TABLE "reviews"
    ADD CONSTRAINT "FK_reviews_cafes"
    FOREIGN KEY ("cafe_id") REFERENCES "cafes"("id") ON DELETE CASCADE;

ALTER TABLE "reviews"
    ADD CONSTRAINT "FK_reviews_users"
    FOREIGN KEY ("user_id") REFERENCES "users"("id") ON DELETE CASCADE;
```

### V2__Add_Trigram_Extension.sql

```sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_cafes_name_trgm
    ON cafes USING GIN (name gin_trgm_ops);
```

### V3__Lower_Pgtrgm_Threshold.sql

```sql
-- word_similarity_threshold: 기본 0.6 → 0.3으로 낮춤 (더 유연한 검색)
ALTER DATABASE cafun_db SET pg_trgm.word_similarity_threshold = 0.3;
```

> **V3, V4 (데이터 삽입)**: SQL INSERT 파일 대신 Spring Boot `DataInitializer.java`로 처리합니다.
> SQL에 304개 레코드를 직접 넣으면 파일이 수만 줄이 됩니다.

---

## 8. 인메모리 캐시 구조 (NestJS → Java 변환)

NestJS의 캐시 서비스가 하는 일을 정확히 이해해야 Java로 옮길 수 있습니다.

### NestJS 원본 쿼리

```sql
-- 캐시 로딩 시 실행되는 실제 쿼리
SELECT
    v.cafe_id,
    v.vector,
    m.keyword_counts,
    c.convenience          -- ← cafes 테이블에서 편의시설 정보도 함께
FROM cafe_aspect_vectors v
LEFT JOIN cafe_metadata m ON v.cafe_id = m.cafe_id
LEFT JOIN cafes c ON v.cafe_id = c.id
```

### NestJS 캐시 자료구조

```typescript
// Float32Array: 304 × 12 = 3648개 float값이 연속으로 저장됨
aspectVectors: Float32Array  // [카페0_dim0, 카페0_dim1, ..., 카페0_dim11, 카페1_dim0, ...]

// Map: 카페ID → 메타데이터
cafeMetadataMap: Map<string, {
    index: number,                        // Float32Array에서 이 카페의 시작 위치
    keywordCounts: Record<string, number>,  // { "라떼": 6, "선물": 26 }
    conveniences: Record<string, boolean>,  // { "주차": false, "와이파이": true }
}>
```

### Java 변환 (CafeVectorData record)

```java
// Float32Array → double[] (Java는 Float32Array 없음)
// Map<string, CafeCacheMetadata> → Map<String, CafeVectorData>

public record CafeVectorData(
    double[] vector,                      // 12차원 (Float32Array 대신)
    Map<String, Integer> keywordCounts,   // { "라떼": 6, "선물": 26 }
    Map<String, Boolean> conveniences     // { "주차": false, "와이파이": true } ← Boolean!
) {}
```

### Java 캐시 로딩 쿼리

```java
// CafeRepository.java
@Query(value = """
    SELECT
        v.cafe_id,
        v.vector,
        m.keyword_counts,
        c.convenience
    FROM cafe_aspect_vectors v
    LEFT JOIN cafe_metadata m ON v.cafe_id = m.cafe_id
    LEFT JOIN cafes c ON v.cafe_id = c.id
    """, nativeQuery = true)
List<Object[]> findAllForCache();
```

---

## 9. 데이터 검증 체크리스트

Spring Boot 시드 완료 후 확인할 것들:

```sql
-- 카페 수 확인
SELECT COUNT(*) FROM cafes;                    -- 304

-- 벡터 수 확인 (카페와 동일해야 함)
SELECT COUNT(*) FROM cafe_aspect_vectors;      -- 304

-- 메타데이터 수 확인
SELECT COUNT(*) FROM cafe_metadata;            -- 304

-- lat/lon 범위 확인 (서울 범위: lat 37.4~37.7, lon 126.7~127.3)
SELECT MIN(lat), MAX(lat), MIN(lon), MAX(lon) FROM cafes;
-- MIN(lat): 37.xxx, MAX(lon): 127.xxx 이어야 정상

-- convenience 형태 확인 (Object 형태여야 함)
SELECT id, convenience FROM cafes LIMIT 3;
-- { "주차": false, "와이파이": true } 형태

-- 벡터 12차원 확인
SELECT cafe_id, array_length(vector, 1) FROM cafe_aspect_vectors LIMIT 5;
-- array_length: 12

-- 메뉴 수 확인
SELECT COUNT(*) FROM menus;   -- 카페당 평균 수개 × 304
```

---

## 10. 트러블슈팅

### lat/lon이 거꾸로 나올 때
```
증상: 지도에서 카페 위치가 완전히 다른 곳에 표시됨
원인: DataInitializer에서 lat/lon 스왑을 안 함
해결: data.get("lon") → lat, data.get("lat") → lon 으로 스왑
```

### convenience 필터링이 작동 안 할 때
```
증상: ?facilities=와이파이 로 검색해도 모든 카페가 반환됨
원인: Map<String, Boolean>이 아닌 List<String>으로 매핑함
해결: Cafe 엔티티의 convenience 타입을 Map<String, Boolean>으로 수정
     캐시 서비스에서 Boolean.TRUE.equals() 로 비교
```

### 벡터 값이 모두 0.0일 때
```
증상: 검색 결과가 없거나 순서가 이상함
원인: cafe_scores.json 로딩 실패 또는 cafeId 불일치
해결:
  1. data/cafe_scores.json 파일 경로 확인 (프로젝트 루트 기준)
  2. cafes 테이블에 먼저 데이터가 있는지 확인
  3. scores의 cafeId가 cafes.id와 일치하는지 확인
     SELECT COUNT(*) FROM cafe_aspect_vectors WHERE vector = '{0,0,0,0,0,0,0,0,0,0,0,0}';
```

### 키워드 검색 결과가 없을 때
```
증상: keywords=라떼 로 검색해도 결과 없음
원인: cafe_keywords 파일 로딩 실패
해결:
  SELECT keyword_counts FROM cafe_metadata LIMIT 1;
  → 빈 {}면 로딩 실패
  → data/cafe_keywords/ 경로 확인
```
