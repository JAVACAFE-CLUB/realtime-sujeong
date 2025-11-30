package javacafe.realtime_sujeong.indexing.common.config;

import javacafe.realtime_sujeong.common.kafka.dto.CleaningPayload;
import javacafe.realtime_sujeong.common.kafka.dto.KafkaMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Consumer Configuration
 *
 * Kafka Consumer 설정 및 배치 처리 설정
 */
@Slf4j
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Value("${spring.kafka.consumer.auto-offset-reset:earliest}")
    private String autoOffsetReset;

    @Value("${spring.kafka.consumer.max-poll-records:100}")
    private int maxPollRecords;

    @Value("${indexing.batch.size:100}")
    private int batchSize;

    /**
     * Consumer Factory
     */
    @Bean
    public ConsumerFactory<String, KafkaMessage<CleaningPayload>> consumerFactory() {
        Map<String, Object> config = new HashMap<>();

        // Bootstrap servers
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Group ID
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        // Auto offset reset
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);

        // Max poll records (배치 크기)
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);

        // Manual commit
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        // Deserializers
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        // JSON Deserializer 설정
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, KafkaMessage.class.getName());
        config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        log.info("Kafka Consumer configured: bootstrapServers={}, groupId={}, maxPollRecords={}",
                bootstrapServers, groupId, maxPollRecords);

        return new DefaultKafkaConsumerFactory<>(config);
    }

    /**
     * Kafka Listener Container Factory (배치 처리)
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, KafkaMessage<CleaningPayload>> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, KafkaMessage<CleaningPayload>> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());

        // 배치 리스너 활성화
        factory.setBatchListener(true);

        // 수동 커밋 설정
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        // 동시성 설정 (파티션 수와 동일하게 설정 권장)
        factory.setConcurrency(3);

        log.info("Kafka Listener Container Factory configured: batchListener=true, ackMode=MANUAL, concurrency=3");

        return factory;
    }
}