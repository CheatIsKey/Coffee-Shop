package jpa.basic.coffeeshop.domain.order.kafka;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Producer 설정
 *
 * acks = all: 리더 + 모든 ISR(In-Sync Replicas)이 메시지를 저장했을 때 발행
 *                  -> 로컬 단일 브로커 환경에서는 replication.factor = 1이므로 리더만 응답하면 됨
 *                     replication.factor = 1은 리더와 팔로워 전부 포함해서 1개라는 뜻으로, 즉 리더만 존재
 *
 * enable.idempotence = true: 멱등성 Producer 활성화
 *  - Broker가 PID + Sequence Number로 중복 메시지를 감지하고 거부
 *  - Producer 재시도(retries)로 인한 중복 발행을 방지
 *  - acks = all, retries >= 1 조건이 만족되어야 활성화 가능
 */
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // Key, Value 모두 String 직렬화
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    @Primary // 자동 설정 KafkaTemplate보다 이 Bean을 우선 주입
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
