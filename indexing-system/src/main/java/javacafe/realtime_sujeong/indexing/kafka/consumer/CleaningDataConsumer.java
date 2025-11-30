package javacafe.realtime_sujeong.indexing.kafka.consumer;

import javacafe.realtime_sujeong.common.kafka.dto.CleaningPayload;
import javacafe.realtime_sujeong.common.kafka.dto.KafkaMessage;
import javacafe.realtime_sujeong.indexing.cleaning.domain.CleanedData;
import javacafe.realtime_sujeong.indexing.cleaning.domain.CleanedDataRepository;
import javacafe.realtime_sujeong.indexing.keyword.service.KeywordIndexingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Cleaning Data Consumer
 *
 * cleaning-to-indexing 토픽에서 메시지를 소비하고 키워드 색인을 수행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CleaningDataConsumer {

    private final CleanedDataRepository cleanedDataRepository;
    private final KeywordIndexingService keywordIndexingService;

    /**
     * cleaning-to-indexing 토픽 메시지 소비
     *
     * @param messages      KafkaMessage 리스트 (배치)
     * @param acknowledgment Kafka Acknowledgment
     */
    @KafkaListener(
            topics = "${kafka.topics.cleaning-to-indexing}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeCleaningData(
            List<KafkaMessage<CleaningPayload>> messages,
            Acknowledgment acknowledgment
    ) {
        log.info("Received {} messages from cleaning-to-indexing topic", messages.size());

        int successCount = 0;
        int failureCount = 0;

        for (KafkaMessage<CleaningPayload> message : messages) {
            try {
                processMessage(message);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to process message: messageId={}, error={}",
                        message.getMessageId(), e.getMessage(), e);
                failureCount++;
                // 배치 처리이므로 하나 실패해도 계속 진행
            }
        }

        // 수동 커밋
        acknowledgment.acknowledge();

        log.info("Batch processing completed: success={}, failure={}", successCount, failureCount);
    }

    /**
     * 단일 메시지 처리
     *
     * @param message KafkaMessage
     */
    private void processMessage(KafkaMessage<CleaningPayload> message) {
        CleaningPayload payload = message.getPayload();

        log.debug("Processing message: messageId={}, dataId={}, source={}",
                message.getMessageId(), payload.getDataId(), payload.getSource());

        // 1. MongoDB에서 cleaned_data 조회
        Optional<CleanedData> cleanedDataOpt = cleanedDataRepository.findByDataId(payload.getDataId());

        if (cleanedDataOpt.isEmpty()) {
            log.warn("CleanedData not found in MongoDB: dataId={}", payload.getDataId());
            return;
        }

        CleanedData cleanedData = cleanedDataOpt.get();

        // 2. 키워드 색인
        indexKeywords(cleanedData);

        log.debug("Successfully processed message: messageId={}, dataId={}",
                message.getMessageId(), payload.getDataId());
    }

    /**
     * 키워드 색인 수행
     *
     * @param cleanedData 정제된 데이터
     */
    private void indexKeywords(CleanedData cleanedData) {
        String title = cleanedData.getCleanedTitle();
        String content = cleanedData.getCleanedContent();
        String source = cleanedData.getSource();
        String dataId = cleanedData.getDataId();
        LocalDate date = LocalDate.now(); // 또는 cleanedData.getCleanedAt().toLocalDate()

        // 한국어 데이터만 처리
        if (!"ko".equalsIgnoreCase(cleanedData.getLanguage())) {
            log.debug("Skipping non-Korean data: dataId={}, language={}",
                    dataId, cleanedData.getLanguage());
            return;
        }

        // 제목 또는 본문이 비어있으면 처리하지 않음
        if ((title == null || title.trim().isEmpty()) &&
                (content == null || content.trim().isEmpty())) {
            log.warn("Both title and content are empty: dataId={}", dataId);
            return;
        }

        try {
            keywordIndexingService.indexKeywords(title, content, source, dataId, date);
            log.info("Successfully indexed keywords: dataId={}, source={}", dataId, source);
        } catch (Exception e) {
            log.error("Failed to index keywords: dataId={}", dataId, e);
            throw new RuntimeException("Keyword indexing failed", e);
        }
    }
}