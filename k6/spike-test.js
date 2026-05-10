/**
 * Spike Test — 순간 급증 시나리오
 * 목적: 갑작스러운 트래픽 급증 시 시스템 복원력 확인
 * 실행: k6 run k6/spike-test.js
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const errorRate = new Rate('error_rate');

export const options = {
    stages: [
        { duration: '30s', target: 10  }, // 기본 트래픽
        { duration: '10s', target: 200 }, // 스파이크: 10초 만에 200 VU로 급증
        { duration: '1m',  target: 200 }, // 스파이크 유지
        { duration: '10s', target: 10  }, // 급감
        { duration: '2m',  target: 10  }, // 회복 확인
        { duration: '30s', target: 0   },
    ],
    thresholds: {
        'http_req_duration': ['p(99)<3000'], // 스파이크 중에도 P99 3초 이내
        'error_rate':        ['rate<0.05'],  // 에러율 5% 미만
    },
};

const BASE_URL = 'http://localhost:8080';

export default function () {
    const res = http.get(`${BASE_URL}/api/menus/popular`);
    check(res, { '인기 메뉴 200': (r) => r.status === 200 });
    errorRate.add(res.status >= 400);
    sleep(1);
}