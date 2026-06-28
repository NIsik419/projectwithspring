# cafun-backend (Spring Boot)

## 프로젝트 개요
NestJS → Spring Boot 마이그레이션 + 아키텍처 업그레이드
- 기존: NestJS + PostgreSQL + 헥사고날 (304개 카페, 성수동)
- 목표: Spring Boot + 모듈러 모놀리스 + 클린 아키텍처

## 핵심 도메인 지식
- DualHeadBERT ABSA 모델로 12개 aspect 점수 추출 (cafun NLP 핵심)
- 인메모리 벡터 캐시로 추천 (앱 시작 시 304개 로드)
- pg_trgm GIN 인덱스로 카페 이름 유사도 검색
- 리뷰 작성 시 aspect 벡터 재계산 → 캐시 동기화

---

## 아키텍처 규칙

### 패키지 구조
```
com.cafun.backend/
├── global/           # 예외, 공통 응답, 설정
│   ├── config/
│   ├── exception/
│   └── response/
└── module/
    ├── user/
    │   ├── domain/         # Entity, 도메인 로직 (순수 Java, 의존성 없음)
    │   ├── application/    # UseCase, Port 인터페이스
    │   ├── infrastructure/ # JPA Repository, Adapter 구현체
    │   └── presentation/   # Controller, DTO
    ├── cafe/
    ├── review/
    └── auth/
```

### 의존성 방향 (절대 역방향 금지)
```
presentation → application → domain
infrastructure → application (Port 구현)
```
- domain은 Spring, JPA 등 프레임워크 import 금지
- 모듈 간 직접 참조 금지 (application layer의 Port를 통해서만)

---

## 작업 규칙 (Claude Code용)

### 응답 형식
- 코드 변경 시 **diff/핵심 부분만** 출력 (전체 파일 금지)
- 이미 설명한 개념 재설명 금지
- 파트 완료 시 체크리스트 업데이트

### 코드 작성 원칙
- 한 파트 = 한 수직 슬라이스 (domain → application → infrastructure → presentation)
- 각 파트는 "실행 가능한 상태"로 끝내기
- 예외는 GlobalExceptionHandler에서만 처리
- 응답 형식: `ApiResponse<T>` 래퍼 통일

### 금지 사항
- 불필요한 boilerplate 출력
- 개념 설명 없이 코드만 붙여넣기 (왜 이 구조인지 한 줄 주석)
- 모듈 간 직접 의존 (반드시 Port 인터페이스 경유)

---

## 파트별 진행 현황

| 파트 | 내용 | 완료 기준 | 상태 |
|------|------|-----------|------|
| 0 | 프로젝트 세팅 + global + Flyway | 앱 시작 + DB 마이그레이션 성공 | ✅ |
| 1 | user 모듈 | `POST /user/register` 201 반환 | ✅ |
| 2 | auth 모듈 + JWT | `POST /auth/login` JWT 반환 | ✅ |
| 3 | cafe 모듈 기본 조회 | `GET /cafe` 응답 = 기존 NestJS 동일 | ✅ |
| 4 | review 모듈 | 리뷰 작성/조회 동작 | ✅ |
| 5 | 추천 로직 + 인메모리 캐시 | `GET /cafe/search?aspectVector=...` 동작 | ✅ |
| 6 | pg_trgm 검색 최적화 | EXPLAIN ANALYZE 인덱스 확인 | ✅ |

---

## 기술 스택
- Java 17, Spring Boot 4.1.0 (Hibernate 7.4.1)
- Spring Data JPA, Spring Security
- PostgreSQL 15 + pg_trgm
- Flyway (DB 마이그레이션)
- TestContainers (핵심 로직 통합 테스트)
- Docker + docker-compose (로컬 환경)

## 테스트 전략
- 추천 알고리즘, Auth 흐름 → TestContainers 통합 테스트
- 나머지 API → `test.http` 파일로 수동 검증
- 파트마다 `test/파트명.http` 파일 생성

## 환경 변수 (.env)
```
DB_HOST=localhost
DB_PORT=5432
DB_NAME=cafun
DB_USERNAME=cafun
DB_PASSWORD=cafun
JWT_SECRET=your-secret-key
JWT_EXPIRATION=86400000
```

---

## 트러블슈팅 빠른 참조

| 증상 | 원인 | 해결 |
|------|------|------|
| FlywayException: checksum mismatch | 실행된 SQL 파일 수정함 | 파일 되돌리고 새 V번호로 추가 |
| Flyway 마이그레이션 안 실행됨 | Spring Boot 4.x에서 `flyway-core` 단독 사용 시 | `spring-boot-starter-flyway`로 교체 |
| 추천 결과 빈 배열 | 캐시 로딩 실패 or cafe_aspect_vectors 없음 | 시작 로그 확인 |
| JWT signature mismatch | 환경별 JWT_SECRET 다름 | .env 값 확인 |
| JSONB 파싱 오류 | hypersistence-utils 미설치 | build.gradle 의존성 확인 |
| float[] 벡터 매핑 오류 | PostgreSQL float[] ↔ Java 기본 매핑 미지원 | @Column(columnDefinition="float[]") 명시 |

---

## 현재 세션 컨텍스트
- 마지막 작업: 파트 6 완료 — 전체 마이그레이션 완료 ✅
- 다음 작업: 없음 (파트 0~6 모두 완료)
- 특이사항:
  - Spring Boot 4.1.0 사용 (Flyway는 반드시 `spring-boot-starter-flyway` 사용, `flyway-core` 단독 사용 시 자동 실행 안 됨)
  - JWT subject = userId(Long), claim "email" 포함 (jjwt 0.12.6)
  - SecurityConfig: `/user/register`, `/auth/login`, `/actuator/**`, `/cafe/**` → permitAll, 나머지 인증 필요
  - Aspect enum ordinal = cafe_aspect_vectors.vectors float[] 인덱스 (순서 변경 금지)
  - 파트 3 현재 aspectScores는 null 반환 — 파트 5 인메모리 캐시에서 Cafe.withAspects()로 병합 예정
  - LoginService가 user 모듈의 UserRepository(Port) 직접 참조 — Port 경유이므로 허용

파트별 작업 범위 (토큰 절약용)


새 대화 시작 시 반드시 명시: "파트 N 시작, 작업 범위: [아래 경로]만"



파트읽어야 할 파일 범위0build.gradle, application.yml, src/main/resources/db/migration/1src/main/java/com/cafun/backend/module/user/2src/main/java/com/cafun/backend/module/auth/, global/config/SecurityConfig.java3src/main/java/com/cafun/backend/module/cafe/4src/main/java/com/cafun/backend/module/review/5src/main/java/com/cafun/backend/module/cafe/application/ (추천 로직)6src/main/resources/db/migration/ (인덱스 마이그레이션)

새 대화 시작 템플릿

CLAUDE.md 읽고 파트 N 시작해줘.
작업 범위: [위 표의 경로]만 참조
이전 완료: [파트 N-1까지 완료, 특이사항 있으면 기재]

### 금지 사항
- 테스트 코드 수정으로 통과 처리 금지
- 실패한 테스트는 구현 코드를 고쳐서 통과시킬 것
- 테스트 변경 시 반드시 사유 먼저 설명 후 승인 받을 것

## 도메인 지식
→ DOMAIN.md 참조 (DB 스키마, 파싱 주의사항, Spring 매핑 포인트)