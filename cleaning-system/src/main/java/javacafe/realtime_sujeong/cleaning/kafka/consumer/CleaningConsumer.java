package javacafe.realtime_sujeong.cleaning.kafka.consumer;

import javacafe.realtime_sujeong.cleaning.domain.CleanedData;
import javacafe.realtime_sujeong.cleaning.service.CleaningService;
import javacafe.realtime_sujeong.common.kafka.constants.KafkaConstants;
import javacafe.realtime_sujeong.common.kafka.dto.CollectionPayload;
import javacafe.realtime_sujeong.common.kafka.dto.KafkaMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Kafka Consumer - collection-to-cleaning 토픽 구독
 * 수집된 원본 데이터를 배치로 수신하여 정제 처리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CleaningConsumer {

    private final CleaningService cleaningService;

    /**
     * collection-to-cleaning 토픽에서 메시지 배치 수신
     * 최대 100개 메시지를 한 번에 처리
     *
     * @param messages Kafka 메시지 리스트
     * @param acknowledgment 수동 커밋용 Acknowledgment
     */
    @KafkaListener(
            topics = KafkaConstants.Topics.COLLECTION_TO_CLEANING,
            containerFactory = "cleaningKafkaListenerContainerFactory"
    )
    public void consumeBatch(
            List<KafkaMessage<CollectionPayload>> messages,
            Acknowledgment acknowledgment
    ) {
        if (messages == null || messages.isEmpty()) {
            log.warn("Received empty message batch");
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
            return;
        }

        log.info("=== Cleaning Consumer Started ===");
        log.info("Received {} messages from {}", messages.size(), KafkaConstants.Topics.COLLECTION_TO_CLEANING);

        long startTime = System.currentTimeMillis();

        try {
            // 메시지 배치 정제 처리
            List<CleanedData> cleanedDataList = cleaningService.processBatch(messages);

            long elapsedTime = System.currentTimeMillis() - startTime;

            log.info("=== Cleaning Consumer Completed ===");
            log.info("Processed {}/{} messages in {}ms",
                    cleanedDataList.size(), messages.size(), elapsedTime);
            log.info("Average: {}ms per message", elapsedTime / messages.size());

            // 수동 커밋 (처리 완료 후)
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
                log.debug("Kafka offset committed successfully");
            }

        } catch (Exception e) {
            log.error("=== Cleaning Consumer Failed ===");
            log.error("Error processing message batch (size: {})", messages.size(), e);

            // 에러 발생 시 커밋하지 않음 (재처리 가능)
            // TODO: 일정 횟수 이상 실패 시 DLQ로 전송하는 로직 추가 필요
            throw new RuntimeException("Failed to process cleaning batch", e);
        }
    }

    /**
     * 단일 메시지 처리 (테스트용)
     * 실제 운영에서는 배치 처리를 사용
     *
     * @param message Kafka 메시지
     */
    public void consumeSingle(KafkaMessage<CollectionPayload> message) {
        log.info("Processing single message: {}", message.getMessageId());
        consumeBatch(List.of(message), null);
    }
}