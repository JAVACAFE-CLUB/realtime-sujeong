package javacafe.realtime_sujeong.indexing.keyword.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Keyword Domain Model
 *
 * Elasticsearch에 저장되는 키워드 도메인 모델
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "keyword_index")
public class Keyword {

    /**
     * 키워드 고유 ID (날짜_키워드)
     * 예: "2025-01-01_삼성전자"
     */
    @Id
    private String keywordId;

    /**
     * 키워드 (예: "삼성전자")
     */
    @Field(type = FieldType.Text, analyzer = "korean_noun_analyzer")
    private String keyword;

    /**
     * 개체 유형 (PER, ORG, LOC, AF 등)
     */
    @Field(type = FieldType.Keyword)
    private String entityType;

    /**
     * 가중치 점수
     */
    @Field(type = FieldType.Double)
    private Double score;

    /**
     * 빈도수
     */
    @Field(type = FieldType.Integer)
    private Integer frequency;

    /**
     * 날짜 (yyyy-MM-dd)
     */
    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate date;

    /**
     * 출처 데이터 ID 리스트 (URL 또는 pageId)
     */
    @Field(type = FieldType.Keyword)
    @Builder.Default
    private List<String> sourceDataIds = new ArrayList<>();

    /**
     * 출처 리스트 (rss, wiki)
     */
    @Field(type = FieldType.Keyword)
    @Builder.Default
    private Set<String> sources = new HashSet<>();

    /**
     * 생성 시각
     */
    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private LocalDateTime createdAt;

    /**
     * 마지막 업데이트 시각
     */
    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private LocalDateTime lastUpdated;

    /**
     * keywordId 생성
     *
     * @param date    날짜
     * @param keyword 키워드
     * @return keywordId (날짜_키워드)
     */
    public static String generateKeywordId(LocalDate date, String keyword) {
        return date.toString() + "_" + keyword;
    }

    /**
     * 점수 추가 (기존 점수에 누적)
     *
     * @param additionalScore 추가 점수
     */
    public void addScore(double additionalScore) {
        if (this.score == null) {
            this.score = 0.0;
        }
        this.score += additionalScore;
    }

    /**
     * 빈도수 증가
     *
     * @param count 증가 횟수
     */
    public void increaseFrequency(int count) {
        if (this.frequency == null) {
            this.frequency = 0;
        }
        this.frequency += count;
    }

    /**
     * 출처 데이터 ID 추가
     *
     * @param dataId 데이터 ID
     */
    public void addSourceDataId(String dataId) {
        if (this.sourceDataIds == null) {
            this.sourceDataIds = new ArrayList<>();
        }
        if (!this.sourceDataIds.contains(dataId)) {
            this.sourceDataIds.add(dataId);
        }
    }

    /**
     * 출처 추가
     *
     * @param source 출처 (rss, wiki)
     */
    public void addSource(String source) {
        if (this.sources == null) {
            this.sources = new HashSet<>();
        }
        this.sources.add(source);
    }

    /**
     * 마지막 업데이트 시각 갱신
     */
    public void updateLastUpdated() {
        this.lastUpdated = LocalDateTime.now();
    }
}