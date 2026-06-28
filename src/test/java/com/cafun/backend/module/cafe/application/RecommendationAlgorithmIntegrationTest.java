package com.cafun.backend.module.cafe.application;

import com.cafun.backend.module.cafe.application.port.AspectVectorPort;
import com.cafun.backend.module.cafe.domain.Aspect;
import com.cafun.backend.module.cafe.domain.Cafe;
import com.cafun.backend.module.review.application.WriteReviewUseCase;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
class RecommendationAlgorithmIntegrationTest {

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine");

    @DynamicPropertySource
    static void overrideDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired AspectVectorCache cache;
    @Autowired CafeSearchUseCase searchUseCase;
    @Autowired AspectVectorPort vectorPort;
    @Autowired WriteReviewUseCase writeReviewUseCase;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext wac;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        // FK 순서 준수: reviews → cafe_aspect_vectors → cafes → users
        jdbc.execute("DELETE FROM reviews");
        jdbc.execute("DELETE FROM cafe_aspect_vectors");
        jdbc.execute("DELETE FROM cafes");
        jdbc.execute("DELETE FROM users");
        cache.load(); // 이전 테스트 데이터 제거 후 빈 캐시로 초기화
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────

    private Long insertCafe(String name) {
        return jdbc.queryForObject(
                "INSERT INTO cafes (name, address, latitude, longitude, created_at) "
                        + "VALUES (?, '성수동', 37.5, 127.0, NOW()) RETURNING id",
                Long.class, name);
    }

    private Long insertUser() {
        return jdbc.queryForObject(
                "INSERT INTO users (email, password, nickname, created_at) "
                        + "VALUES ('tester@cafun.com', 'hashed_pw', '테스터', NOW()) RETURNING id",
                Long.class);
    }

    // aspectIndex 위치에만 val을 넣고 나머지는 0인 12차원 벡터
    private float[] vectorOf(int aspectIndex, float val) {
        float[] v = new float[12];
        v[aspectIndex] = val;
        return v;
    }

    // ── TC1 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("[TC1] 앱 시작 벡터 캐시 로딩 — DB 카페 전부 로드, 각 벡터 길이 == 12")
    void tc1_cacheLoadsAllCafesWithVectorLength12() {
        Long id1 = insertCafe("카페A");
        Long id2 = insertCafe("카페B");
        Long id3 = insertCafe("카페C");
        float[] vec = vectorOf(0, 0.8f);
        vectorPort.upsert(id1, vec);
        vectorPort.upsert(id2, vec);
        vectorPort.upsert(id3, vec);

        cache.load();

        // 삽입한 3개 카페 모두 캐시에 존재
        assertThat(cache.get(id1)).isPresent();
        assertThat(cache.get(id2)).isPresent();
        assertThat(cache.get(id3)).isPresent();
        // DualHeadBERT ABSA 12개 aspect 전부 로드
        assertThat(cache.get(id1).get()).hasSize(12);
        assertThat(cache.get(id2).get()).hasSize(12);
        assertThat(cache.get(id3).get()).hasSize(12);
    }

    // ── TC2 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("[TC2] aspect 필터 추천 — 선택 aspect 점수 높은 순 코사인 유사도 정렬")
    void tc2_searchRanksByTargetAspectScore() {
        Long highTaste = insertCafe("스페셜티 커피");
        Long medTaste  = insertCafe("보통 커피");
        Long lowTaste  = insertCafe("그냥 커피");
        vectorPort.upsert(highTaste, vectorOf(Aspect.TASTE.index(), 0.9f));
        vectorPort.upsert(medTaste,  vectorOf(Aspect.TASTE.index(), 0.5f));
        vectorPort.upsert(lowTaste,  vectorOf(Aspect.TASTE.index(), 0.1f));
        cache.load();

        // TASTE=1.0 쿼리 → 코사인 유사도 기준 TASTE 높은 순
        List<Cafe> results = searchUseCase.searchByAspectVector(
                vectorOf(Aspect.TASTE.index(), 1.0f), 10);

        assertThat(results).hasSize(3);
        assertThat(results.get(0).getId()).isEqualTo(highTaste);
        assertThat(results.get(1).getId()).isEqualTo(medTaste);
        assertThat(results.get(2).getId()).isEqualTo(lowTaste);
    }

    // ── TC3 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("[TC3] 키워드 점수 반영 — Math.log1p(count) × 0.5 가중치 높은 카페 우선 랭킹")
    void tc3_logWeightedReviewCountAffectsRanking() {
        // ABSA 모델이 리뷰 수에 log1p(count)*0.5 가중치 적용 후 벡터를 저장
        // 가중치 적용 결과값을 직접 벡터에 주입하여 코사인 유사도 정렬 검증
        float highWeight = (float) (Math.log1p(50) * 0.5); // 리뷰 50개 ≈ 1.98
        float lowWeight  = (float) (Math.log1p(2)  * 0.5); // 리뷰 2개  ≈ 0.55

        Long popular = insertCafe("인기 카페");
        Long sparse  = insertCafe("한산 카페");
        vectorPort.upsert(popular, vectorOf(Aspect.ATMOSPHERE.index(), highWeight));
        vectorPort.upsert(sparse,  vectorOf(Aspect.ATMOSPHERE.index(), lowWeight));
        cache.load();

        List<Cafe> results = searchUseCase.searchByAspectVector(
                vectorOf(Aspect.ATMOSPHERE.index(), 1.0f), 10);

        // 리뷰 많은(= log 가중치 높은) 카페가 우선 노출
        assertThat(results.get(0).getId()).isEqualTo(popular);
        assertThat(results.get(1).getId()).isEqualTo(sparse);
    }

    // ── TC4 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("[TC4] 리뷰 작성 후 캐시 동기화 — 새 리뷰 → DB 벡터 갱신 → 캐시 반영")
    void tc4_cacheRefreshedAfterReviewWritten() {
        Long cafeId = insertCafe("동기화 테스트 카페");
        Long userId  = insertUser();

        // 초기 벡터(TASTE=0.3) 캐시 로드
        vectorPort.upsert(cafeId, vectorOf(Aspect.TASTE.index(), 0.3f));
        cache.load();
        assertThat(cache.get(cafeId).get()[Aspect.TASTE.index()]).isEqualTo(0.3f);

        // ABSA 재계산 결과(0.9)를 DB에 반영 후 리뷰 작성으로 이벤트 발행
        vectorPort.upsert(cafeId, vectorOf(Aspect.TASTE.index(), 0.9f));
        writeReviewUseCase.writeReview(cafeId, userId, "맛이 훨씬 좋아졌어요!");

        // @Async ReviewEventListener → cache.refresh(cafeId) 처리 대기 (최대 5초)
        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() ->
                        assertThat(cache.get(cafeId).get()[Aspect.TASTE.index()])
                                .isEqualTo(0.9f));
    }

    // ── TC5 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("[TC5-a] 빈 aspectVector(length=0) → 서비스 레벨 예외 없이 처리")
    void tc5a_emptyVectorHandledGracefully() {
        // cosineSimilarity: normA==0 → 0.0f 반환, 예외 없이 빈 리스트 반환
        assertThatCode(() -> searchUseCase.searchByAspectVector(new float[]{}, 10))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("[TC5-b] aspectVector 길이 != 12 → 컨트롤러 INVALID_INPUT 400 반환")
    void tc5b_wrongLengthVectorReturns400() throws Exception {
        // parseAspectVector: length != 12 → BusinessException(INVALID_INPUT)
        // → GlobalExceptionHandler → HTTP 400
        mockMvc.perform(get("/cafe/search")
                        .param("aspectVector", "0.1,0.2")
                        .param("limit", "10"))
                .andExpect(status().isBadRequest());
    }
}
