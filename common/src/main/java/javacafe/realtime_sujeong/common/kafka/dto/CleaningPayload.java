package javacafe.realtime_sujeong.common.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Cleaning System에서 Indexing System으로 전송하는 페이로드
 * 정제 완료된 데이터 정보
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CleaningPayload {

    /**
     * 데이터 고유 식별자 (SHA-256 해시)
     */
    private String dataId;

    /**
     * 데이터 소스 (rss, wiki, api 등)
     */
    private String source;

    /**
     * 문서 제목
     */
    private String title;

    /**
     * 정제된 본문 텍스트
     */
    private String cleanedContent;

    /**
     * 언어 코드 (ko, en, ja 등)
     */
    private String language;

    /**
     * 메타데이터
     * - originalLength: 원본 길이
     * - cleanedLength: 정제 후 길이
     * - strategyName: RSS 전략명 (RSS만)
     * - url: 원본 URL (RSS만)
     */
    private Map<String, Object> metadata;

    /**
     * 정제 처리 완료 시각
     */
    private LocalDateTime processedAt;
}