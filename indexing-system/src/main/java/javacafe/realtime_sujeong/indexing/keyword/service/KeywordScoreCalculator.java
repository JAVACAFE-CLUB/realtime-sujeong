package javacafe.realtime_sujeong.indexing.keyword.service;

import javacafe.realtime_sujeong.indexing.keyword.domain.KeywordScore;
import javacafe.realtime_sujeong.indexing.ner.dto.NamedEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * KeywordScore Calculator
 *
 * NER 결과로부터 키워드 점수를 계산하는 컴포넌트
 */
@Slf4j
@Component
public class KeywordScoreCalculator {

    @Value("${keyword.scoring.title-weight:2.0}")
    private double titleWeight;

    @Value("${keyword.scoring.content-weight:1.0}")
    private double contentWeight;

    @Value("${keyword.scoring.entity-weight.org:1.5}")
    private double orgWeight;

    @Value("${keyword.scoring.entity-weight.per:1.3}")
    private double perWeight;

    @Value("${keyword.scoring.entity-weight.loc:1.0}")
    private double locWeight;

    @Value("${keyword.scoring.entity-weight.af:1.2}")
    private double afWeight;

    @Value("${keyword.scoring.entity-weight.default:1.0}")
    private double defaultWeight;

    /**
     * 제목과 본문의 개체명으로부터 키워드 점수 계산
     *
     * @param titleEntities   제목 개체명 리스트
     * @param contentEntities 본문 개체명 리스트
     * @param source          출처 (rss, wiki)
     * @param dataId          데이터 ID
     * @return 키워드 점수 리스트
     */
    public List<KeywordScore> calculateScores(
            List<NamedEntity> titleEntities,
            List<NamedEntity> contentEntities,
            String source,
            String dataId
    ) {
        // 키워드별 집계 맵 (키워드 → ScoreAccumulator)
        Map<String, ScoreAccumulator> accumulatorMap = new HashMap<>();

        // 제목 개체명 처리
        for (NamedEntity entity : titleEntities) {
            String keyword = entity.getWord();
            String entityType = entity.getEntityType();

            ScoreAccumulator accumulator = accumulatorMap.computeIfAbsent(
                    keyword,
                    k -> new ScoreAccumulator(keyword, entityType)
            );

            accumulator.addTitleFrequency(1);
        }

        // 본문 개체명 처리
        for (NamedEntity entity : contentEntities) {
            String keyword = entity.getWord();
            String entityType = entity.getEntityType();

            ScoreAccumulator accumulator = accumulatorMap.computeIfAbsent(
                    keyword,
                    k -> new ScoreAccumulator(keyword, entityType)
            );

            accumulator.addContentFrequency(1);
        }

        // 최종 점수 계산
        List<KeywordScore> keywordScores = new ArrayList<>();
        for (ScoreAccumulator accumulator : accumulatorMap.values()) {
            double score = calculateScore(
                    accumulator.titleFrequency,
                    accumulator.contentFrequency,
                    accumulator.entityType
            );

            KeywordScore keywordScore = KeywordScore.builder()
                    .keyword(accumulator.keyword)
                    .entityType(accumulator.entityType)
                    .titleFrequency(accumulator.titleFrequency)
                    .contentFrequency(accumulator.contentFrequency)
                    .totalFrequency(accumulator.titleFrequency + accumulator.contentFrequency)
                    .score(score)
                    .source(source)
                    .dataId(dataId)
                    .build();

            keywordScores.add(keywordScore);
        }

        log.debug("Calculated {} keyword scores from {} title entities and {} content entities",
                keywordScores.size(), titleEntities.size(), contentEntities.size());

        return keywordScores;
    }

    /**
     * 점수 계산
     *
     * Score = (titleFreq × titleWeight × entityWeight) + (contentFreq × contentWeight × entityWeight)
     *
     * @param titleFrequency   제목 빈도수
     * @param contentFrequency 본문 빈도수
     * @param entityType       개체 유형
     * @return 계산된 점수
     */
    private double calculateScore(int titleFrequency, int contentFrequency, String entityType) {
        double entityWeight = getEntityWeight(entityType);

        double titleScore = titleFrequency * titleWeight * entityWeight;
        double contentScore = contentFrequency * contentWeight * entityWeight;

        return titleScore + contentScore;
    }

    /**
     * 개체 유형별 가중치 조회
     *
     * @param entityType 개체 유형 (PER, ORG, LOC, AF 등)
     * @return 가중치
     */
    private double getEntityWeight(String entityType) {
        if (entityType == null) {
            return defaultWeight;
        }

        return switch (entityType.toUpperCase()) {
            case "ORG" -> orgWeight;      // 기관/기업: 1.5
            case "PER" -> perWeight;      // 인물: 1.3
            case "AF" -> afWeight;        // 인공물/제품: 1.2
            case "LOC" -> locWeight;      // 지역: 1.0
            default -> defaultWeight;     // 기타: 1.0
        };
    }

    /**
     * 점수 집계용 내부 클래스
     */
    private static class ScoreAccumulator {
        private final String keyword;
        private final String entityType;
        private int titleFrequency = 0;
        private int contentFrequency = 0;

        public ScoreAccumulator(String keyword, String entityType) {
            this.keyword = keyword;
            this.entityType = entityType;
        }

        public void addTitleFrequency(int count) {
            this.titleFrequency += count;
        }

        public void addContentFrequency(int count) {
            this.contentFrequency += count;
        }
    }
}