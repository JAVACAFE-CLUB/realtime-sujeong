package javacafe.realtime_sujeong.common.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Kafka 메시지 표준 포맷
 * 모든 시스템 간 통신에 사용되는 공통 메시지 래퍼
 *
 * @param <T> 페이로드 타입 (CollectionPayload, CleaningPayload, IndexingPayload 등)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaMessage<T> {

    /**
     * 메시지 포맷 버전
     */
    private String version;

    /**
     * 메시지 고유 ID (UUID)
     */
    private String messageId;

    /**
     * 메시지 생성 타임스탬프
     */
    private LocalDateTime timestamp;

    /**
     * 이벤트 타입 (DATA_COLLECTED, DATA_CLEANED, DATA_INDEXED 등)
     */
    private String eventType;

    /**
     * 실제 데이터 페이로드
     */
    private T payload;

    /**
     * 재시도 횟수
     */
    @Builder.Default
    private int retryCount = 0;
}