package javacafe.realtime_sujeong.collection.rss.collector.dto;

import javacafe.realtime_sujeong.collection.rss.domain.RssRawData;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * RSS 아이템 DTO
 * RSS 피드에서 파싱한 개별 아이템 데이터
 */
@Getter
@Builder
public class RssItemDto {

    /**
     * 기사 제목
     */
    private String title;

    /**
     * 기사 링크 (URL)
     */
    private String link;

    /**
     * 발행 일시
     */
    private LocalDateTime pubDate;

    /**
     * 기사 요약/설명
     */
    private String description;

    /**
     * 기사 본문
     */
    private String content;

    /**
     * 데이터 소스 (언론사)
     */
    private String source;

    /**
     * RssRawData로 변환
     */
    public RssRawData toEntity(String dataId) {
        return RssRawData.builder()
                .dataId(dataId)
                .source(source)
                .title(title)
                .link(link)
                .pubDate(pubDate)
                .description(description)
                .content(content)
                .build();
    }
}