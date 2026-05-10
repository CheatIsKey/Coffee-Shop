/**
 * Stress Test — 한계점 탐색
 * 목적: 시스템이 언제 무너지는지, 최대 처리 가능 VU 수 측정
 * 실행: k6 run k6/stress-test.js
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const errorRate = new Rate('error_rate');

export const options = {
    stages: [
        { duration: '2m', target: 50  },
        { duration: '2m', target: 100 },
        { duration: '2m', target: 150 },
        { duration: '2m', target: 200 },
        { duration: '2m', target: 250 }, // 여기서 한계 도달 예상
        { duration: '2m', target: 0   },
    ],
    thresholds: {
        'http_req_duration': ['p(99)<5000'],
        'error_rate':        ['rate<0.10'], // 스트레스 테스트는 10%까지 허용
    },
};

const BASE_URL = 'http://localhost:8080';

export default function () {
    const res = http.get(`${BASE_URL}/api/menus?page=1&size=10`);
    check(res, { '200': (r) => r.status === 200 });
    errorRate.add(res.status >= 400);
    sleep(1);
}