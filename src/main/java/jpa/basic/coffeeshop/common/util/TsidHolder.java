package jpa.basic.coffeeshop.common.util;

import com.github.f4b6a3.tsid.TsidCreator;
import org.springframework.stereotype.Component;

/**
 * TSID(Time-Sorted Unique ID) 생성 유틸리티 컴포넌트
 *
 * TSID를 직접 생성하지 않고 이 클래스를 통해 생성하는 이유:
 * - 테스트 코드에서 @MockBean으로 교체하여 고정값을 주입할 수 있어 결정론적 테스트가 가능하다.
 * - 추후 생성 전략(UUID, Snowflake 등)을 변경하더라도 호출부 코드를 수정할 필요가 없다.
 *
 * 사용처:
 * - PointCharge.pgTid  : "TID_{yyyyMMdd}_{TSID}"
 * - Order.orderUid     : "ORD-{yyyyMMdd}-{TSID}"
 */
@Component
public class TsidHolder {

    /**
     * 고유한 TSID 문자열을 생성
     * TSID는 시간 기반으로 정렬 가능하며, UUID보다 짧고 인덱스 효율이 높다.
     *
     * @return TSID 문자열 (예: "0AWE5HZP3YTS3")
     */
    public String generate() {
        return TsidCreator.getTsid().toString();
    }
}