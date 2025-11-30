package javacafe.realtime_sujeong.indexing.cache.service;

import javacafe.realtime_sujeong.indexing.cache.dto.CachedTopKeywords;
import javacafe.realtime_sujeong.indexing.keyword.domain.Keyword;
import javacafe.realtime_sujeong.indexing.keyword.dto.KeywordScoreDto;
import javacafe.realtime_sujeong.indexing.keyword.service.KeywordIndexingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Keyword Cache Service
 *
 * Redis를 사용한 키워드 캐시 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeywordCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final KeywordIndexingService keywordIndexingService;

    @Value("${redis.cache.ttl.daily-keywords:90000}")
    private long cacheTtl; // 25시간 (초 단위)

    @Value("${redis.cache.prefix.daily-top-keywords}")
    private String cacheKeyPrefix;

    private static final int TOP_N = 10;

    /**
     * 일별 Top 10 키워드 조회 (캐시 우선)
     *
     * @param date 날짜
     * @return 캐시된 Top 키워드
     */
    public Optional<CachedTopKeywords> getTopKeywords(LocalDate date) {
        String cacheKey = generateCacheKey(date);

        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);

            if (cached != null) {
                log.debug("Cache hit for top keywords: date={}", date);
                return Optional.of((CachedTopKeywords) cached);
            }

            log.debug("Cache miss for top keywords: date={}", date);

            // 캐시 미스 시 Elasticsearch에서 조회 후 캐시
            return refreshTopKeywords(date);

        } catch (Exception e) {
            log.error("Failed to get top keywords from cache: date={}", date, e);
            return Optional.empty();
        }
    }

    /**
     * Top 10 키워드 캐시 갱신
     *
     * @param date 날짜
     * @return 갱신된 캐시 데이터
     */
    public Optional<CachedTopKeywords> refreshTopKeywords(LocalDate date) {
        try {
            // Elasticsearch에서 Top 10 조회
            List<Keyword> topKeywords = keywordIndexingService.getTopKeywords(date, TOP_N);

            if (topKeywords.isEmpty()) {
                log.warn("No keywords found for date: {}", date);
                return Optional.empty();
            }

            // DTO 변환 (순위 포함)
            List<KeywordScoreDto> keywordScores = IntStream.range(0, topKeywords.size())
                    .mapToObj(i -> KeywordScoreDto.from(topKeywords.get(i), i + 1))
                    .toList();

            // 캐시 데이터 생성
            CachedTopKeywords cachedData = CachedTopKeywords.builder()
                    .date(date)
                    .keywords(keywordScores)
                    .cachedAt(LocalDateTime.now())
                    .totalCount(keywordScores.size())
                    .build();

            // Redis에 캐시 (TTL 설정)
            String cacheKey = generateCacheKey(date);
            redisTemplate.opsForValue().set(cacheKey, cachedData, cacheTtl, TimeUnit.SECONDS);

            log.info("Refreshed top keywords cache: date={}, count={}", date, keywordScores.size());

            return Optional.of(cachedData);

        } catch (Exception e) {
            log.error("Failed to refresh top keywords cache: date={}", date, e);
            return Optional.empty();
        }
    }

    /**
     * 캐시 무효화
     *
     * @param date 날짜
     */
    public void invalidateCache(LocalDate date) {
        String cacheKey = generateCacheKey(date);

        try {
            Boolean deleted = redisTemplate.delete(cacheKey);
            if (Boolean.TRUE.equals(deleted)) {
                log.info("Invalidated top keywords cache: date={}", date);
            }
        } catch (Exception e) {
            log.error("Failed to invalidate cache: date={}", date, e);
        }
    }

    /**
     * 개별 키워드 점수 캐시 저장
     *
     * @param date    날짜
     * @param keyword 키워드
     * @param score   점수
     */
    public void cacheKeywordScore(LocalDate date, String keyword, Double score) {
        String cacheKey = generateKeywordScoreCacheKey(date, keyword);

        try {
            redisTemplate.opsForValue().set(cacheKey, score, cacheTtl, TimeUnit.SECONDS);
            log.debug("Cached keyword score: date={}, keyword={}, score={}", date, keyword, score);
        } catch (Exception e) {
            log.error("Failed to cache keyword score: date={}, keyword={}", date, keyword, e);
        }
    }

    /**
     * 개별 키워드 점수 조회
     *
     * @param date    날짜
     * @param keyword 키워드
     * @return 점수 (Optional)
     */
    public Optional<Double> getKeywordScore(LocalDate date, String keyword) {
        String cacheKey = generateKeywordScoreCacheKey(date, keyword);

        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return Optional.of((Double) cached);
            }
        } catch (Exception e) {
            log.error("Failed to get keyword score from cache: date={}, keyword={}", date, keyword, e);
        }

        return Optional.empty();
    }

    /**
     * 매일 자정에 오늘 날짜의 Top 10 캐시 갱신 (스케줄링)
     */
    @Scheduled(cron = "0 0 0 * * *") // 매일 00:00:00
    public void scheduledRefreshTodayTopKeywords() {
        LocalDate today = LocalDate.now();
        log.info("Scheduled refresh of today's top keywords: date={}", today);

        refreshTopKeywords(today);
    }

    /**
     * 매 시간 정각에 오늘 날짜의 Top 10 캐시 갱신
     */
    @Scheduled(cron = "0 0 * * * *") // 매 시간 00분
    public void scheduledRefreshTopKeywordsHourly() {
        LocalDate today = LocalDate.now();
        log.info("Hourly refresh of today's top keywords: date={}", today);

        refreshTopKeywords(today);
    }

    /**
     * 캐시 키 생성
     *
     * @param date 날짜
     * @return 캐시 키 (예: "daily_top_keywords:2025-01-01")
     */
    private String generateCacheKey(LocalDate date) {
        return cacheKeyPrefix + ":" + date.toString();
    }

    /**
     * 개별 키워드 점수 캐시 키 생성
     *
     * @param date    날짜
     * @param keyword 키워드
     * @return 캐시 키 (예: "keyword_score:2025-01-01:삼성전자")
     */
    private String generateKeywordScoreCacheKey(LocalDate date, String keyword) {
        return "keyword_score:" + date.toString() + ":" + keyword;
    }
}