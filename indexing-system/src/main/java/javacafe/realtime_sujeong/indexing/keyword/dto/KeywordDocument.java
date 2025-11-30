package javacafe.realtime_sujeong.indexing.keyword.dto;

import javacafe.realtime_sujeong.indexing.keyword.domain.Keyword;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * KeywordDocument DTO
 *
 * Elasticsearch 문서 조회/색인 시 사용하는 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeywordDocument {

    private String keywordId;
    private String keyword;
    private String entityType;
    private Double score;
    private Integer frequency;
    private LocalDate date;
    private List<String> sourceDataIds;
    private Set<String> sources;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;

    /**
     * Keyword 도메인 모델로 변환
     *
     * @return Keyword 도메인 모델
     */
    public Keyword toEntity() {
        return Keyword.builder()
                .keywordId(keywordId)
                .keyword(keyword)
                .entityType(entityType)
                .score(score)
                .frequency(frequency)
                .date(date)
                .sourceDataIds(sourceDataIds)
                .sources(sources)
                .createdAt(createdAt)
                .lastUpdated(lastUpdated)
                .build();
    }

    /**
     * Keyword 도메인 모델에서 DTO 생성
     *
     * @param keyword Keyword 도메인 모델
     * @return KeywordDocument DTO
     */
    public static KeywordDocument from(Keyword keyword) {
        return KeywordDocument.builder()
                .keywordId(keyword.getKeywordId())
                .keyword(keyword.getKeyword())
                .entityType(keyword.getEntityType())
                .score(keyword.getScore())
                .frequency(keyword.getFrequency())
                .date(keyword.getDate())
                .sourceDataIds(keyword.getSourceDataIds())
                .sources(keyword.getSources())
                .createdAt(keyword.getCreatedAt())
                .lastUpdated(keyword.getLastUpdated())
                .build();
    }
}