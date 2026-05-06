/**
 * K6 부하 테스트
 *
 * 부하 수치와 위치를 지정
 */
import http from 'k6/http';

// 부하 수치, 시간 지정
export const options = {
    // 5명이 10초 동안 계속 부하를 준다.
    vus: 5,             // 동시에 요청을 보내는 사용자 수
    duration: '10s',    // 부하를 얼마나 오래 유지할지
};

// 부하 위치
export default function () {
    // K6와 서버가 동일한 컨테이너 안에서 실행 중일 때 (거의 없음)
    // http.get("http://localhost:8080/actuator/health");

    // K6는 컨테이너에서 돌고, 서버는 내 PC(호스트)에서 직접 실행 중일 때
    // 'host.docker.internal'은 컨테이너 밖(내 로컬 PC)으로 나가는 통로이다.
    http.get("http://host.docker.internal:8080/actuator/health");

    // Docker Compose를 사용해 'app'이라는 서비스 이름으로 연결할 때
    // http.get("http://app:8080/actuator/health");
}