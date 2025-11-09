package javacafe.realtime_sujeong.cleaning.kafka.producer;

import javacafe.realtime_sujeong.cleaning.domain.CleanedData;
import javacafe.realtime_sujeong.common.kafka.constants.KafkaConstants;
import javacafe.realtime_sujeong.common.kafka.dto.CleaningPayload;
import javacafe.realtime_sujeong.common.kafka.dto.KafkaMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka Producer - cleaning-to-indexing 토픽으로 발행
 * 정제된 데이터를 Indexing System으로 전송
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CleaningProducer {

    private final KafkaTemplate<String, KafkaMessage<CleaningPayload>> kafkaTemplate;

    /**
     * 정제된 데이터를 배치로 Kafka에 발행
     *
     * @param cleanedDataList 정제된 데이터 리스트
     */
    public void sendBatch(List<CleanedData> cleanedDataList) {
        if (cleanedDataList == null || cleanedDataList.isEmpty()) {
            log.warn("No cleaned data to send");
            return;
        }

        log.info("=== Cleaning Producer Started ===");
        log.info("Sending {} messages to {}", cleanedDataList.size(), KafkaConstants.Topics.CLEANING_TO_INDEXING);

        long startTime = System.currentTimeMillis();
        int successCount = 0;
        int failureCount = 0;

        for (CleanedData cleanedData : cleanedDataList) {
            try {
                sendSingle(cleanedData);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to send message for dataId: {}", cleanedData.getDataId(), e);
                failureCount++;
            }
        }

        long elapsedTime = System.currentTimeMillis() - startTime;

        log.info("=== Cleaning Producer Completed ===");
        log.info("Sent {}/{} messages in {}ms (Failed: {})",
                successCount, cleanedDataList.size(), elapsedTime, failureCount);
    }

    /**
     * 단일 데이터를 Kafka에 발행
     *
     * @param cleanedData 정제된 데이터
     */
    public void sendSingle(CleanedData cleanedData) {
        // CleanedData → CleaningPayload 변환
        CleaningPayload payload = CleaningPayload.builder()
                .dataId(cleanedData.getDataId())
                .source(cleanedData.getSource())
                .title(cleanedData.getTitle())
                .cleanedContent(cleanedData.getCleanedContent())
                .language(cleanedData.getLanguage())
                .metadata(cleanedData.getMetadata())  // url은 metadata에 포함
                .processedAt(cleanedData.getProcessedAt())
                .build();

        // KafkaMessage 래핑
        KafkaMessage<CleaningPayload> kafkaMessage = KafkaMessage.<CleaningPayload>builder()
                .version(KafkaConstants.Versions.DEFAULT)
                .messageId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .eventType(KafkaConstants.EventTypes.DATA_CLEANED)
                .payload(payload)
                .retryCount(0)
                .build();

        // Kafka 발행 (비동기)
        CompletableFuture<SendResult<String, KafkaMessage<CleaningPayload>>> future =
                kafkaTemplate.send(
                        KafkaConstants.Topics.CLEANING_TO_INDEXING,
                        cleanedData.getDataId(),  // Key: dataId
                        kafkaMessage
                );

        // 비동기 결과 처리
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.debug("Message sent successfully: dataId={}, partition={}, offset={}",
                        cleanedData.getDataId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send message: dataId={}", cleanedData.getDataId(), ex);
                throw new RuntimeException("Kafka send failed for dataId: " + cleanedData.getDataId(), ex);
            }
        });
    }

    /**
     * 동기 발행 (테스트용)
     * 실제로는 비동기 발행 권장
     *
     * @param cleanedData 정제된 데이터
     */
    public void sendSync(CleanedData cleanedData) {
        CleaningPayload payload = CleaningPayload.builder()
                .dataId(cleanedData.getDataId())
                .source(cleanedData.getSource())
                .title(cleanedData.getTitle())
                .cleanedContent(cleanedData.getCleanedContent())
                .language(cleanedData.getLanguage())
                .metadata(cleanedData.getMetadata())  // url은 metadata에 포함
                .processedAt(cleanedData.getProcessedAt())
                .build();

        KafkaMessage<CleaningPayload> kafkaMessage = KafkaMessage.<CleaningPayload>builder()
                .version(KafkaConstants.Versions.DEFAULT)
                .messageId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .eventType(KafkaConstants.EventTypes.DATA_CLEANED)
                .payload(payload)
                .retryCount(0)
                .build();

        try {
            SendResult<String, KafkaMessage<CleaningPayload>> result = kafkaTemplate.send(
                    KafkaConstants.Topics.CLEANING_TO_INDEXING,
                    cleanedData.getDataId(),
                    kafkaMessage
            ).get();  // 동기 대기

            log.info("Message sent synchronously: dataId={}, partition={}, offset={}",
                    cleanedData.getDataId(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());

        } catch (Exception e) {
            log.error("Failed to send message synchronously: dataId={}", cleanedData.getDataId(), e);
            throw new RuntimeException("Kafka send failed", e);
        }
    }
}