package javacafe.realtime_sujeong.common.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Wikipedia 소스의 상세 정보
 * collection-to-cleaning 메시지의 sourceDetails에 포함
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WikiSourceDetails implements SourceDetails {

    /**
     * 네임스페이스 (예: 0 = 일반 문서)
     */
    private String namespace;

    /**
     * 문서 제목
     */
    private String title;

    /**
     * 페이지 ID
     */
    private String pageId;

    /**
     * 리비전 ID
     */
    private String revisionId;

    /**
     * 타임스탬프
     */
    private LocalDateTime timestamp;
}