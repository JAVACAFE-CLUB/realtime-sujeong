package javacafe.realtime_sujeong.common.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * RSS 소스의 상세 정보
 * collection-to-cleaning 메시지의 sourceDetails에 포함
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RssSourceDetails implements SourceDetails {

    /**
     * 수집 전략 이름 (예: 조선일보, 매일경제)
     */
    private String strategyName;

    /**
     * 뉴스 제목
     */
    private String title;

    /**
     * 뉴스 URL
     */
    private String url;

    /**
     * 기자명
     */
    private String author;

    /**
     * 발행 시각
     */
    private LocalDateTime publishedAt;
}
