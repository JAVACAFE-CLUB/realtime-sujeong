package javacafe.realtime_sujeong.collection.rss.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * RSS 원본 데이터
 * MongoDB에 저장되는 수집된 RSS 피드 아이템
 */
@Document(collection = "rss_raw_data")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RssRawData {

    /**
     * MongoDB Document ID
     */
    @Id
    private String id;

    /**
     * 데이터 고유 ID (SHA-256 hash of link + pubDate)
     * 중복 수집 방지용
     */
    @Indexed(unique = true)
    private String dataId;

    /**
     * 데이터 소스 (언론사)
     * 예: chosun, maeil, donga
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
     * 기사 본문 (크롤링)
     */
    private String content;

    /**
     * 수집 일시
     */
    @CreatedDate
    @Indexed
    private LocalDateTime collectedAt;

    /**
     * 처리 상태
     * COLLECTED: 수집 완료
     * PROCESSING: 정제 중
     * PROCESSED: 정제 완료
     * INDEXED: 인덱싱 완료
     */
    @Indexed
    private DataStatus status;

    @Builder
    public RssRawData(String dataId, String source, String title,
                      String link, LocalDateTime pubDate, String description, String content) {
        this.dataId = dataId;
        this.source = source;
        this.title = title;
        this.link = link;
        this.pubDate = pubDate;
        this.description = description;
        this.content = content;
        this.status = DataStatus.COLLECTED;
        this.collectedAt = LocalDateTime.now();
    }

    /**
     * 상태 변경
     */
    public void updateStatus(DataStatus status) {
        this.status = status;
    }

    /**
     * 데이터 처리 상태
     */
    public enum DataStatus {
        COLLECTED,   // 수집 완료
        PROCESSING,  // 정제 중
        PROCESSED,   // 정제 완료
        INDEXED      // 인덱싱 완료
    }
}