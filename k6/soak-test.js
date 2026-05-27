/**
 * Soak Test — 장시간 안정성 확인
 * 목적: 메모리 누수, 커넥션 풀 고갈 등 장시간 운영 이슈 탐지
 * 실행: k6 run k6/soak-test.js (최소 30분 이상 권장)
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const responseTime = new Trend('response_time_ms');
const errorRate    = new Rate('error_rate');

export const options = {
    stages: [
        { duration: '2m',  target: 30 }, // 워밍업
        { duration: '30m', target: 30 }, // 30분 유지 (실제 운영 시 1시간 이상 권장)
        { duration: '2m',  target: 0  }, // 쿨다운
    ],
    thresholds: {
        'http_req_duration': ['p(95)<500'],
        'error_rate':        ['rate<0.01'],
        // Soak 테스트의 핵심: 시간이 지나도 응답시간이 증가하지 않아야 함
    },
};

const BASE_URL = 'http://coffeeshop-alb-1667448264.ap-northeast-2.elb.amazonaws.com';

export default function () {
    const menuRes = http.get(`${BASE_URL}/api/menus?page=1&size=10`);
    check(menuRes, { '200': (r) => r.status === 200 });
    responseTime.add(menuRes.timings.duration);
    errorRate.add(menuRes.status >= 400);

    sleep(2);
}