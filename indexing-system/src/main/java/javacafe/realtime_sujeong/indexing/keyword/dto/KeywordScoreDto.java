package javacafe.realtime_sujeong.indexing.keyword.dto;

import javacafe.realtime_sujeong.indexing.keyword.domain.Keyword;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * KeywordScore DTO
 *
 * 키워드 점수 조회 시 사용하는 DTO (외부 API 응답용)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeywordScoreDto {

    /**
     * 순위
     */
    private Integer rank;

    /**
     * 키워드
     */
    private String keyword;

    /**
     * 개체 유형
     */
    private String entityType;

    /**
     * 점수
     */
    private Double score;

    /**
     * 빈도수
     */
    private Integer frequency;

    /**
     * 날짜
     */
    private LocalDate date;

    /**
     * Keyword 도메인 모델에서 DTO 생성
     *
     * @param keyword Keyword 도메인 모델
     * @param rank    순위
     * @return KeywordScoreDto
     */
    public static KeywordScoreDto from(Keyword keyword, int rank) {
        return KeywordScoreDto.builder()
                .rank(rank)
                .keyword(keyword.getKeyword())
                .entityType(keyword.getEntityType())
                .score(keyword.getScore())
                .frequency(keyword.getFrequency())
                .date(keyword.getDate())
                .build();
    }

    /**
     * Keyword 도메인 모델에서 DTO 생성 (순위 없이)
     *
     * @param keyword Keyword 도메인 모델
     * @return KeywordScoreDto
     */
    public static KeywordScoreDto from(Keyword keyword) {
        return from(keyword, 0);
    }
}