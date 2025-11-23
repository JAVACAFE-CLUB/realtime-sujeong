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
     * 데이터 고유 ID (URL 그대로 사용)
     * - 같은 URL은 같은 dataId → Kafka 파티션 순서 보장
     * - 새 버전(pubDate 변경)은 upsert로 덮어씀 (최신 버전만 유지)
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
     * 더 최신 데이터로 업데이트 (upsert용)
     * pubDate가 더 최신인 경우에만 호출해야 함
     */
    public void updateFromNewer(String title, LocalDateTime pubDate, 
                                 String description, String content) {
        this.title = title;
        this.pubDate = pubDate;
        this.description = description;
        this.content = content;
        this.collectedAt = LocalDateTime.now();
        this.status = DataStatus.COLLECTED;
    }

    /**
     * pubDate 비교 (이 데이터가 더 오래되었는지)
     */
    public boolean isOlderThan(LocalDateTime otherPubDate) {
        if (this.pubDate == null) return true;
        if (otherPubDate == null) return false;
        return this.pubDate.isBefore(otherPubDate);
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