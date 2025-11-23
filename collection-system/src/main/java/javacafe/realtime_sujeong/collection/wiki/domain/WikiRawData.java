package javacafe.realtime_sujeong.collection.wiki.domain;

import javacafe.realtime_sujeong.common.dto.WikiPage;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Wikipedia 수집 원본 데이터
 * MongoDB Collection: wiki_raw_data
 */
@Getter
@Builder
@ToString
@Document(collection = "wiki_raw_data")
public class WikiRawData {

    @Id
    private String id;

    /**
     * Wiki 데이터 고유 식별자 (pageId 그대로 사용)
     * - 같은 페이지는 같은 dataId → Kafka 파티션 순서 보장
     * - 새 리비전(timestamp 변경)은 upsert로 덮어씀 (최신 버전만 유지)
     */
    @Indexed(unique = true)
    private String dataId;

    /**
     * 데이터 소스 (항상 "wiki")
     */
    @Indexed
    private String source;

    /**
     * Wiki 페이지 네임스페이스 (0: 일반 문서, 14: 분류, 등)
     */
    @Indexed
    private String namespace;

    /**
     * Wiki 페이지 제목 (검색/필터링용)
     */
    @Indexed
    private String title;

    /**
     * Wiki 페이지 원본 데이터
     */
    private WikiPage wikiPage;

    /**
     * 수집 시각
     */
    @Indexed
    private LocalDateTime collectedAt;

    /**
     * 더 최신 리비전으로 업데이트 (upsert용)
     * timestamp가 더 최신인 경우에만 호출해야 함
     */
    public void updateFromNewer(WikiPage newWikiPage, String title, String namespace) {
        this.wikiPage = newWikiPage;
        this.title = title;
        this.namespace = namespace;
        this.collectedAt = LocalDateTime.now();
    }

    /**
     * timestamp 비교 (이 데이터가 더 오래되었는지)
     */
    public boolean isOlderThan(LocalDateTime otherTimestamp) {
        if (this.wikiPage == null || this.wikiPage.getRevision() == null) return true;
        LocalDateTime thisTimestamp = this.wikiPage.getRevision().getTimestamp();
        if (thisTimestamp == null) return true;
        if (otherTimestamp == null) return false;
        return thisTimestamp.isBefore(otherTimestamp);
    }

    /**
     * source를 항상 "wiki"로 설정하는 빌더 패턴 확장
     */
    public static class WikiRawDataBuilder {
        private String source = "wiki";

        // source 설정을 방지 (항상 "wiki"로 고정)
        public WikiRawDataBuilder source(String source) {
            // 무시하고 항상 "wiki" 유지
            return this;
        }
    }
}