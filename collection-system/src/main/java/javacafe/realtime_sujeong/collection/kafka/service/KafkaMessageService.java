package javacafe.realtime_sujeong.collection.kafka.service;

import javacafe.realtime_sujeong.common.kafka.constants.KafkaConstants;
import javacafe.realtime_sujeong.common.kafka.dto.CollectionPayload;
import javacafe.realtime_sujeong.common.kafka.dto.KafkaMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka 메시지 전송 서비스
 * Collection System에서 Cleaning System으로 메시지를 전송
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaMessageService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 데이터 수집 완료 메시지 전송
     *
     * @param dataId 데이터 고유 식별자
     * @param source 데이터 소스 (rss, wiki, api)
     * @param mongoCollectionName MongoDB 컬렉션명
     * @param sourceDetails 소스별 상세 정보
     * @return CompletableFuture<Boolean> 전송 성공/실패
     */
    public CompletableFuture<Boolean> sendDataCollectedMessage(
            String dataId,
            String source,
            String mongoCollectionName,
            Object sourceDetails) {

        try {
            // CollectionPayload 생성
            CollectionPayload payload = CollectionPayload.builder()
                    .dataId(dataId)
                    .source(source)
                    .mongoCollectionName(mongoCollectionName)
                    .priority(KafkaConstants.Priorities.NORMAL)
                    .sourceDetails(sourceDetails)
                    .build();

            // KafkaMessage 래퍼 생성
            KafkaMessage<CollectionPayload> message = KafkaMessage.<CollectionPayload>builder()
                    .version(KafkaConstants.Versions.DEFAULT)
                    .messageId(UUID.randomUUID().toString())
                    .timestamp(LocalDateTime.now())
                    .eventType(KafkaConstants.EventTypes.DATA_COLLECTED)
                    .payload(payload)
                    .retryCount(0)
                    .build();

            // Kafka 메시지 전송 (dataId를 파티셔닝 키로 사용)
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                    KafkaConstants.Topics.COLLECTION_TO_CLEANING,
                    dataId, // 파티셔닝을 위한 키
                    message
            );

            // 전송 결과 처리
            return future.handle((result, throwable) -> {
                if (throwable != null) {
                    log.error("Kafka 메시지 전송 실패: dataId={}, source={}", dataId, source, throwable);
                    return false;
                } else {
                    log.debug("Kafka 메시지 전송 성공: dataId={}, source={}, topic={}, partition={}, offset={}",
                            dataId, source,
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                    return true;
                }
            });

        } catch (Exception e) {
            log.error("Kafka 메시지 생성 실패: dataId={}, source={}", dataId, source, e);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * RSS 데이터 수집 완료 메시지 전송 (편의 메서드)
     *
     * @param dataId 데이터 ID
     * @param source 언론사 소스
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> sendRssCollectedMessage(String dataId, String source) {
        return sendDataCollectedMessage(
                dataId,
                source,
                KafkaConstants.Collections.RSS_RAW_DATA,
                null
        );
    }

    /**
     * Wiki 데이터 수집 완료 메시지 전송 (편의 메서드)
     *
     * @param dataId 데이터 ID
     * @param namespace Wiki 네임스페이스
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> sendWikiCollectedMessage(String dataId, String namespace) {
        return sendDataCollectedMessage(
                dataId,
                "wiki",
                KafkaConstants.Collections.WIKI_RAW_DATA,
                namespace
        );
    }
}
