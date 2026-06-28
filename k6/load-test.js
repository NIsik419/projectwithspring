/**
 * cafun 부하테스트 — 50 VU, 30s
 * 대상: GET /cafe/search, GET /cafe/name-search, POST /auth/login
 * 목표: p95 < 200ms (TEST.md 기준선)
 *
 * 실행:
 *   k6 run k6/load-test.js
 *   k6 run --out json=k6/results.json k6/load-test.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

// ── 엔드포인트별 응답시간 트렌드 ───────────────────────────────────
const searchDuration    = new Trend('cafe_search_p95',      true);
const nameSearchDuration = new Trend('cafe_name_search_p95', true);
const loginDuration     = new Trend('auth_login_p95',       true);
const errorRate         = new Rate('error_rate');

// ── 부하 시나리오 ──────────────────────────────────────────────────
export const options = {
  vus: 50,
  duration: '30s',

  thresholds: {
    // 전체 요청 p95 < 200ms
    http_req_duration:     ['p(95)<200'],
    // 엔드포인트별 p95 < 200ms
    cafe_search_p95:       ['p(95)<200'],
    cafe_name_search_p95:  ['p(95)<200'],
    auth_login_p95:        ['p(95)<200'],
    // 에러율 1% 미만
    error_rate:            ['rate<0.01'],
  },
};

const BASE_URL = 'http://localhost:8080';

// aspect enum 순서: TASTE,FOOD,SERVICE,ATMOSPHERE,PRICE,LOCATION,CLEANLINESS,NOISE,WAITING,PARKING,WIFI,SPACE
const ASPECT_VECTORS = [
  '0.9,0.7,0.8,0.9,0.5,0.6,0.8,0.3,0.4,0.2,0.7,0.8',
  '0.5,0.9,0.6,0.7,0.8,0.4,0.5,0.6,0.3,0.7,0.8,0.9',
  '0.3,0.4,0.9,0.5,0.6,0.8,0.7,0.9,0.5,0.3,0.4,0.6',
  '0.8,0.3,0.5,0.6,0.9,0.7,0.4,0.2,0.8,0.5,0.6,0.7',
  '0.6,0.8,0.4,0.3,0.7,0.9,0.6,0.5,0.7,0.8,0.3,0.4',
];

const CAFE_QUERIES = ['스타벅스', '카페', '이디야', '투썸', '메가커피', '컴포즈', '할리스'];

// ── setup: JWT 한 번 발급 → 전 VU 공유 ───────────────────────────
export function setup() {
  const res = http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify({ email: 'test@cafun.com', password: 'password123' }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  if (res.status !== 200) {
    console.warn(`[setup] login failed: ${res.status} — ${res.body}`);
    return { token: null };
  }

  const body = JSON.parse(res.body);
  // ApiResponse<LoginResponse> → data.accessToken
  const token = body?.data?.accessToken ?? null;
  console.log(`[setup] JWT 발급 완료: ${token ? '성공' : '실패'}`);
  return { token };
}

// ── 메인 가상 사용자 로직 ──────────────────────────────────────────
export default function (data) {
  const authHeader = data.token
    ? { Authorization: `Bearer ${data.token}` }
    : {};

  // 세 API를 ~균등하게 호출 (무작위 분배)
  const scenario = __VU % 3; // VU 번호 기반으로 3개 시나리오 고정 분배

  if (scenario === 0) {
    callAspectSearch(authHeader);
  } else if (scenario === 1) {
    callNameSearch(authHeader);
  } else {
    callLogin();
  }

  // 100ms 생각시간 — 서버 thundering herd 방지
  sleep(0.1);
}

// ── /cafe/search?aspectVector=... ─────────────────────────────────
function callAspectSearch(headers) {
  const vector = ASPECT_VECTORS[Math.floor(Math.random() * ASPECT_VECTORS.length)];
  const res = http.get(`${BASE_URL}/cafe/search?aspectVector=${vector}`, { headers });

  searchDuration.add(res.timings.duration);

  const ok = check(res, {
    '[search] status 200':  (r) => r.status === 200,
    '[search] data array':  (r) => {
      try { return Array.isArray(JSON.parse(r.body).data); } catch { return false; }
    },
  });
  errorRate.add(!ok);
}

// ── /cafe/name-search?q=... ───────────────────────────────────────
function callNameSearch(headers) {
  const q = CAFE_QUERIES[Math.floor(Math.random() * CAFE_QUERIES.length)];
  const res = http.get(
    `${BASE_URL}/cafe/name-search?q=${encodeURIComponent(q)}`,
    { headers }
  );

  nameSearchDuration.add(res.timings.duration);

  const ok = check(res, {
    '[name-search] status 200': (r) => r.status === 200,
    '[name-search] data exists': (r) => {
      try { return JSON.parse(r.body).data !== undefined; } catch { return false; }
    },
  });
  errorRate.add(!ok);
}

// ── POST /auth/login ──────────────────────────────────────────────
function callLogin() {
  const res = http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify({ email: 'test@cafun.com', password: 'password123' }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  loginDuration.add(res.timings.duration);

  const ok = check(res, {
    '[login] status 200':   (r) => r.status === 200,
    '[login] has token':    (r) => {
      try { return !!JSON.parse(r.body).data?.accessToken; } catch { return false; }
    },
  });
  errorRate.add(!ok);
}
