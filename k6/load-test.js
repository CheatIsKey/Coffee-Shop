/**
 * Load Test — 점진적 부하 증가
 * 목적: 정상 트래픽 범위에서 응답시간, 에러율 측정
 * 실행: k6 run k6/load-test.js
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

const responseTime = new Trend('response_time_ms');
const errorRate    = new Rate('error_rate');
const orderCount   = new Counter('order_count');

export const options = {
    stages: [
        { duration: '1m',  target: 10  }, // 워밍업: 1분간 10 VU로 증가
        { duration: '3m',  target: 50  }, // 증가: 3분간 50 VU로 증가
        { duration: '5m',  target: 50  }, // 유지: 5분간 50 VU 유지
        { duration: '1m',  target: 0   }, // 쿨다운
    ],
    thresholds: {
        'http_req_duration': ['p(95)<500', 'p(99)<1000'], // P95 < 500ms, P99 < 1s
        'error_rate':        ['rate<0.01'],                // 에러율 1% 미만
    },
};

const BASE_URL = 'http://localhost:8080';

// 공통 헤더 (로그인 토큰이 필요한 경우 아래에 추가)
const headers = {
    'Content-Type': 'application/json',
    // 'Authorization': 'Bearer {TOKEN}' // 실제 토큰으로 교체
};

export default function () {
    // 1. 메뉴 목록 조회 (비회원 허용)
    const menuRes = http.get(`${BASE_URL}/api/menus?page=1&size=10`, { headers });
    check(menuRes, { '메뉴 목록 200': (r) => r.status === 200 });
    responseTime.add(menuRes.timings.duration);
    errorRate.add(menuRes.status >= 400);

    sleep(0.5);

    // 2. 인기 메뉴 조회
    const popularRes = http.get(`${BASE_URL}/api/menus/popular`, { headers });
    check(popularRes, { '인기 메뉴 200': (r) => r.status === 200 });
    responseTime.add(popularRes.timings.duration);
    errorRate.add(popularRes.status >= 400);

    sleep(1);
}