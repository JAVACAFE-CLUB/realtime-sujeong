package javacafe.realtime_sujeong.cleaning.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javacafe.realtime_sujeong.common.kafka.dto.CleaningPayload;
import javacafe.realtime_sujeong.common.kafka.dto.CollectionPayload;
import javacafe.realtime_sujeong.common.kafka.dto.KafkaMessage;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka 설정
 * Consumer: collection-to-cleaning 토픽에서 배치 수신
 * Producer: cleaning-to-indexing 토픽으로 발행
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroupId;

    @Value("${spring.kafka.consumer.max-poll-records:100}")
    private int maxPollRecords;

    /**
     * Consumer 설정
     * CollectionPayload를 담은 KafkaMessage 수신
     */
    @Bean
    public ConsumerFactory<String, KafkaMessage<CollectionPayload>> consumerFactory() {
        Map<String, Object> config = new HashMap<>();

        // 기본 설정
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);

        // 배치 처리 설정
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        config.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024);  // 최소 1KB
        config.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);  // 최대 500ms 대기

        // 오프셋 관리
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);  // 수동 커밋
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // 역직렬화 설정
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        // JSON 역직렬화 상세 설정
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        // ObjectMapper에 JavaTimeModule 추가 (LocalDateTime 지원)
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // TypeReference를 사용하여 중첩된 Generic 타입 정보 보존
        return new DefaultKafkaConsumerFactory<>(
                config,
                new StringDeserializer(),
                new JsonDeserializer<>(new TypeReference<KafkaMessage<CollectionPayload>>() {}, objectMapper, false)
        );
    }

    /**
     * Kafka Listener Container Factory
     * 배치 리스너 모드 설정
     */
    @Bean("cleaningKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, KafkaMessage<CollectionPayload>>
            kafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, KafkaMessage<CollectionPayload>> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());

        // 배치 리스너 활성화
        factory.setBatchListener(true);

        // 동시성 설정 (파티션 개수와 맞춤)
        factory.setConcurrency(3);

        // 수동 커밋 모드 - MANUAL로 설정하여 Acknowledgment 사용 가능
        factory.getContainerProperties().setAckMode(
                org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL
        );

        return factory;
    }

    /**
     * Producer 설정
     * CleaningPayload를 담은 KafkaMessage 발행
     */
    @Bean
    public ProducerFactory<String, KafkaMessage<CleaningPayload>> producerFactory() {
        Map<String, Object> config = new HashMap<>();

        // 기본 설정
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // 안정성 설정
        config.put(ProducerConfig.ACKS_CONFIG, "all");  // 모든 replica 확인
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);  // 순서 보장

        // 배치 성능 설정
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);  // 16KB
        config.put(ProducerConfig.LINGER_MS_CONFIG, 10);  // 10ms 대기
        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");

        // 직렬화 설정
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        return new DefaultKafkaProducerFactory<>(config);
    }

    /**
     * Kafka Template
     * Producer를 통해 메시지 발행
     */
    @Bean
    public KafkaTemplate<String, KafkaMessage<CleaningPayload>> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}