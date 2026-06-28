package com.cafun.backend.module.auth;

import com.cafun.backend.global.security.JwtProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
class AuthIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void overrideDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtProvider jwtProvider;
    @Autowired WebApplicationContext wac;

    MockMvc mockMvc;

    static final String EMAIL    = "tester@cafun.com";
    static final String PASSWORD = "TestPass1234!";

    @BeforeEach
    void setUp() {
        // springSecurity() 명시: JwtAuthenticationFilter + EntryPoint 포함한 Security 필터 체인 적용
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(springSecurity())
                .build();
        jdbc.execute("DELETE FROM reviews");
        jdbc.execute("DELETE FROM cafe_aspect_vectors");
        jdbc.execute("DELETE FROM cafes");
        jdbc.execute("DELETE FROM users");
        jdbc.update(
                "INSERT INTO users (email, password, nickname, created_at) VALUES (?, ?, '테스터', NOW())",
                EMAIL, passwordEncoder.encode(PASSWORD));
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────

    private Long testUserId() {
        return jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, EMAIL);
    }

    private String validToken() {
        return jwtProvider.generate(testUserId(), EMAIL);
    }

    // ── TC1 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("[TC1] 정상 로그인 — 200, accessToken 포함")
    void tc1_loginSuccess() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    // ── TC2 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("[TC2] 존재하지 않는 이메일 — 401")
    void tc2_unknownEmail() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ghost@cafun.com","password":"any"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // ── TC3 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("[TC3] 비밀번호 불일치 — 401")
    void tc3_wrongPassword() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"wrongpassword"}
                                """.formatted(EMAIL)))
                .andExpect(status().isUnauthorized());
    }

    // ── TC4 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("[TC4] 발급된 JWT로 인증 필요 API 호출 — 200")
    void tc4_validJwtAccessesProtectedEndpoint() throws Exception {
        // 로그인 플로우 없이 jwtProvider.generate()로 직접 유효 토큰 생성
        mockMvc.perform(get("/review")
                        .param("cafeId", "1")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    // ── TC5-a ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("[TC5-a] 형식 자체가 깨진 토큰 — 401")
    void tc5a_malformedToken() throws Exception {
        // JwtProvider.isValid(): JwtException → false → SecurityContextHolder 미설정 → EntryPoint 401
        mockMvc.perform(get("/review")
                        .param("cafeId", "1")
                        .header("Authorization", "Bearer INVALID.JWT.TOKEN"))
                .andExpect(status().isUnauthorized());
    }

    // ── TC5-b ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("[TC5-b] 구조는 맞지만 서명 키가 다른 토큰 — 401")
    void tc5b_wrongSignatureToken() throws Exception {
        // 올바른 JWT 구조이지만 다른 secret으로 서명 → parse() 시 SignatureException → isValid() false
        SecretKey wrongKey = Keys.hmacShaKeyFor(
                "wrong-secret-key-minimum-32-characters!!".getBytes(StandardCharsets.UTF_8));
        String wrongToken = Jwts.builder()
                .subject("1")
                .claim("email", EMAIL)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000L))
                .signWith(wrongKey)
                .compact();

        mockMvc.perform(get("/review")
                        .param("cafeId", "1")
                        .header("Authorization", "Bearer " + wrongToken))
                .andExpect(status().isUnauthorized());
    }

    // ── TC6 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("[TC6] JWT 없이 인증 필요 API 호출 — 401")
    void tc6_noTokenOnProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/review")
                        .param("cafeId", "1"))
                .andExpect(status().isUnauthorized());
    }

    // ── TC7 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("[TC7] permitAll 경로 미인증 접근 — 200 (Security 차단 없음)")
    void tc7_permitAllWithoutToken() throws Exception {
        // SecurityConfig: /cafe/** → permitAll → 토큰 없어도 200
        mockMvc.perform(get("/cafe"))
                .andExpect(status().isOk());
    }
}
