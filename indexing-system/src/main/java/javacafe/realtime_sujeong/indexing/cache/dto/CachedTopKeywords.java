package javacafe.realtime_sujeong.indexing.cache.dto;

import javacafe.realtime_sujeong.indexing.keyword.dto.KeywordScoreDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Cached Top Keywords DTO
 *
 * Redis에 캐시되는 일별 Top 키워드 데이터
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CachedTopKeywords {

    /**
     * 날짜
     */
    private LocalDate date;

    /**
     * Top 키워드 리스트 (순위 포함)
     */
    private List<KeywordScoreDto> keywords;

    /**
     * 캐시된 시각
     */
    private LocalDateTime cachedAt;

    /**
     * 전체 키워드 개수
     */
    private int totalCount;
}