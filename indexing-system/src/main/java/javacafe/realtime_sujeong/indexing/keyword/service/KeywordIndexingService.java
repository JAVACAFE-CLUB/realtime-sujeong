package javacafe.realtime_sujeong.indexing.keyword.service;

import javacafe.realtime_sujeong.indexing.keyword.domain.Keyword;
import javacafe.realtime_sujeong.indexing.keyword.domain.KeywordRepository;
import javacafe.realtime_sujeong.indexing.keyword.domain.KeywordScore;
import javacafe.realtime_sujeong.indexing.ner.dto.NamedEntity;
import javacafe.realtime_sujeong.indexing.ner.service.NerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Keyword Indexing Service
 *
 * 키워드 색인 비즈니스 로직을 담당하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeywordIndexingService {

    private final NerService nerService;
    private final KeywordScoreCalculator scoreCalculator;
    private final KeywordRepository keywordRepository;

    /**
     * 제목과 본문으로부터 키워드를 추출하고 Elasticsearch에 색인
     *
     * @param title   제목
     * @param content 본문
     * @param source  출처 (rss, wiki)
     * @param dataId  데이터 ID (URL 또는 pageId)
     * @param date    날짜
     */
    @Transactional
    public void indexKeywords(String title, String content, String source, String dataId, LocalDate date) {
        log.debug("Starting keyword indexing for dataId: {}, source: {}", dataId, source);

        // 1. NER로 개체명 추출
        Map<String, List<NamedEntity>> entitiesMap = nerService.extractEntitiesFromTitleAndContent(title, content);
        List<NamedEntity> titleEntities = entitiesMap.get("title");
        List<NamedEntity> contentEntities = entitiesMap.get("content");

        log.debug("Extracted {} title entities and {} content entities",
                titleEntities.size(), contentEntities.size());

        // 2. 키워드 점수 계산
        List<KeywordScore> keywordScores = scoreCalculator.calculateScores(
                titleEntities,
                contentEntities,
                source,
                dataId
        );

        // 3. Elasticsearch에 색인 (upsert)
        int indexed = 0;
        for (KeywordScore keywordScore : keywordScores) {
            upsertKeyword(keywordScore, date);
            indexed++;
        }

        log.info("Successfully indexed {} keywords for dataId: {}", indexed, dataId);
    }

    /**
     * 키워드 Upsert (있으면 업데이트, 없으면 생성)
     *
     * @param keywordScore 키워드 점수
     * @param date         날짜
     */
    private void upsertKeyword(KeywordScore keywordScore, LocalDate date) {
        String keywordId = Keyword.generateKeywordId(date, keywordScore.getKeyword());

        Optional<Keyword> existingKeyword = keywordRepository.findByKeywordId(keywordId);

        if (existingKeyword.isPresent()) {
            // 기존 키워드 업데이트
            Keyword keyword = existingKeyword.get();
            keyword.addScore(keywordScore.getScore());
            keyword.increaseFrequency(keywordScore.getTotalFrequency());
            keyword.addSourceDataId(keywordScore.getDataId());
            keyword.addSource(keywordScore.getSource());
            keyword.updateLastUpdated();

            keywordRepository.save(keyword);

            log.debug("Updated existing keyword: {} (score: {}, frequency: {})",
                    keyword.getKeyword(), keyword.getScore(), keyword.getFrequency());
        } else {
            // 새로운 키워드 생성
            Keyword keyword = Keyword.builder()
                    .keywordId(keywordId)
                    .keyword(keywordScore.getKeyword())
                    .entityType(keywordScore.getEntityType())
                    .score(keywordScore.getScore())
                    .frequency(keywordScore.getTotalFrequency())
                    .date(date)
                    .createdAt(Instant.now())
                    .lastUpdated(Instant.now())
                    .build();

            keyword.addSourceDataId(keywordScore.getDataId());
            keyword.addSource(keywordScore.getSource());

            keywordRepository.save(keyword);

            log.debug("Created new keyword: {} (score: {}, frequency: {})",
                    keyword.getKeyword(), keyword.getScore(), keyword.getFrequency());
        }
    }

    /**
     * 일별 Top N 키워드 조회
     *
     * @param date 날짜
     * @param topN 조회할 개수
     * @return Top N 키워드 리스트
     */
    public List<Keyword> getTopKeywords(LocalDate date, int topN) {
        if (topN == 10) {
            return keywordRepository.findTop10ByDateOrderByScoreDesc(date);
        }
        // Top N이 10이 아닌 경우, 전체 조회 후 제한
        return keywordRepository.findByDateOrderByScoreDesc(date)
                .stream()
                .limit(topN)
                .toList();
    }

    /**
     * 특정 날짜의 모든 키워드 조회
     *
     * @param date 날짜
     * @return 키워드 리스트
     */
    public List<Keyword> getKeywordsByDate(LocalDate date) {
        return keywordRepository.findByDate(date);
    }

    /**
     * 특정 날짜와 개체 유형의 키워드 조회
     *
     * @param date       날짜
     * @param entityType 개체 유형 (PER, ORG, LOC 등)
     * @return 키워드 리스트
     */
    public List<Keyword> getKeywordsByEntityType(LocalDate date, String entityType) {
        return keywordRepository.findByDateAndEntityType(date, entityType);
    }

    /**
     * 키워드 존재 여부 확인
     *
     * @param date    날짜
     * @param keyword 키워드
     * @return 존재 여부
     */
    public boolean existsKeyword(LocalDate date, String keyword) {
        return keywordRepository.existsByDateAndKeyword(date, keyword);
    }

    /**
     * 배치 키워드 색인
     *
     * @param titleContentPairs 제목-본문 쌍 리스트
     * @param source            출처 (rss, wiki)
     * @param dataIds           데이터 ID 리스트
     * @param date              날짜
     */
    @Transactional
    public void indexKeywordsBatch(
            List<Map.Entry<String, String>> titleContentPairs,
            String source,
            List<String> dataIds,
            LocalDate date
    ) {
        log.info("Starting batch keyword indexing for {} documents", titleContentPairs.size());

        for (int i = 0; i < titleContentPairs.size(); i++) {
            Map.Entry<String, String> pair = titleContentPairs.get(i);
            String dataId = dataIds.get(i);

            try {
                indexKeywords(pair.getKey(), pair.getValue(), source, dataId, date);
            } catch (Exception e) {
                log.error("Failed to index keywords for dataId: {}", dataId, e);
                // 배치 처리이므로 하나 실패해도 계속 진행
            }
        }

        log.info("Completed batch keyword indexing");
    }
}