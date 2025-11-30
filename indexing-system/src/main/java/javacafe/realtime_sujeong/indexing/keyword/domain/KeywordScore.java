package javacafe.realtime_sujeong.indexing.keyword.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * KeywordScore Value Object
 *
 * 키워드 점수 계산 결과를 담는 불변 객체
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeywordScore {

    /**
     * 키워드
     */
    private String keyword;

    /**
     * 개체 유형 (PER, ORG, LOC 등)
     */
    private String entityType;

    /**
     * 제목 빈도수
     */
    private int titleFrequency;

    /**
     * 본문 빈도수
     */
    private int contentFrequency;

    /**
     * 전체 빈도수
     */
    private int totalFrequency;

    /**
     * 계산된 점수
     */
    private double score;

    /**
     * 출처 (rss, wiki)
     */
    private String source;

    /**
     * 데이터 ID (URL 또는 pageId)
     */
    private String dataId;

    /**
     * 전체 빈도수 계산
     *
     * @return 제목 빈도수 + 본문 빈도수
     */
    public int calculateTotalFrequency() {
        return titleFrequency + contentFrequency;
    }
}