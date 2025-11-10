package javacafe.realtime_sujeong.collection.wiki.domain;

import javacafe.realtime_sujeong.collection.wiki.dto.WikiPage;
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
     * Wiki 데이터 고유 식별자 (SHA-256: pageId + revisionId)
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