package javacafe.realtime_sujeong.cleaning.domain.view;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * RSS 원본 데이터 조회용 View (read-only)
 * cleaning-system에서 rss_raw_data 컬렉션을 읽기 전용으로 접근
 *
 * collection-system의 RssRawData와 동일한 컬렉션을 참조하지만,
 * cleaning에 필요한 필드만 포함하여 독립성을 확보
 */
@Document(collection = "rss_raw_data")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RssRawDataView {

    /**
     * MongoDB Document ID
     */
    @Id
    private String id;

    /**
     * 데이터 고유 ID (SHA-256 hash of link + pubDate)
     */
    @Indexed(unique = true)
    private String dataId;

    /**
     * 데이터 소스 (언론사)
     */
    @Indexed
    private String source;

    /**
     * 기사 제목
     */
    private String title;

    /**
     * 기사 링크 (URL)
     */
    @Indexed
    private String link;

    /**
     * 발행 일시
     */
    @Indexed
    private LocalDateTime pubDate;

    /**
     * 기사 요약/설명
     */
    private String description;

    /**
     * 기사 본문 (크롤링된 내용)
     */
    private String content;

    // collectedAt, status 필드 제외 (cleaning에서 불필요)
    // setter 없음 (read-only)
}