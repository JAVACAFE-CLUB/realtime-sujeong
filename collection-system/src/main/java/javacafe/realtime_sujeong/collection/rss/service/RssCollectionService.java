package javacafe.realtime_sujeong.collection.rss.service;

import javacafe.realtime_sujeong.collection.common.util.DataIdGenerator;
import javacafe.realtime_sujeong.collection.kafka.service.KafkaMessageService;
import javacafe.realtime_sujeong.collection.rss.collector.crawler.ArticleContentCrawler;
import javacafe.realtime_sujeong.collection.rss.collector.crawler.ArticleCrawlingStrategy;
import javacafe.realtime_sujeong.collection.rss.collector.crawler.ArticleCrawlingStrategyFactory;
import javacafe.realtime_sujeong.collection.rss.collector.dto.RssItemDto;
import javacafe.realtime_sujeong.collection.rss.collector.parser.RssFeedParser;
import javacafe.realtime_sujeong.collection.rss.domain.RssRawData;
import javacafe.realtime_sujeong.collection.rss.domain.RssRawDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RSS 수집 서비스
 * RSS 피드를 파싱하고 기사 본문을 크롤링하여 MongoDB에 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RssCollectionService {

    private final RssFeedParser rssFeedParser;
    private final ArticleContentCrawler articleContentCrawler;
    private final ArticleCrawlingStrategyFactory strategyFactory;
    private final RssRawDataRepository rssRawDataRepository;
    private final KafkaMessageService kafkaMessageService;

    /**
     * RSS 피드 수집
     *
     * @param source 데이터 소스 (언론사)
     * @return 수집 결과 (저장된 개수, 중복 개수)
     */
    @Transactional
    public CollectionResult collectFeed(String source) {
        // 소스에 맞는 전략 획득 (feedUrl 포함)
        ArticleCrawlingStrategy strategy = strategyFactory.getStrategy(source);
        String feedUrl = strategy.getFeedUrl();

        log.info("RSS 피드 수집 시작 - URL: {}, Source: {}", feedUrl, source);

        // 1. RSS 피드 파싱
        List<RssItemDto> items = rssFeedParser.parse(feedUrl, source);
        log.info("파싱된 아이템 개수: {}", items.size());

        int savedCount = 0;
        int duplicateCount = 0;

        // 2. 각 아이템 처리 (본문 크롤링 + 저장)
        for (RssItemDto item : items) {
            try {
                // DataId 생성 (link + pubDate)
                String dataId = DataIdGenerator.generateRssDataId(
                        item.getLink(),
                        item.getPubDate()
                );

                // 중복 체크
                if (rssRawDataRepository.existsByDataId(dataId)) {
                    log.debug("중복 데이터 스킵 - dataId: {}", dataId);
                    duplicateCount++;
                    continue;
                }

                // 기사 본문 크롤링
                String content = articleContentCrawler.crawl(item.getLink(), source);

                // DTO에 본문 추가
                RssItemDto itemWithContent = RssItemDto.builder()
                        .title(item.getTitle())
                        .link(item.getLink())
                        .pubDate(item.getPubDate())
                        .description(item.getDescription())
                        .content(content)  // 크롤링한 본문
                        .source(item.getSource())
                        .build();

                // MongoDB 저장
                RssRawData rssRawData = itemWithContent.toEntity(dataId);
                rssRawDataRepository.save(rssRawData);

                log.debug("데이터 저장 완료 - dataId: {}, title: {}, content: {} 글자",
                        dataId, item.getTitle(), content.length());

                // Kafka 메시지 전송 (비동기)
                kafkaMessageService.sendRssCollectedMessage(dataId, source)
                        .exceptionally(throwable -> {
                            log.warn("Kafka 메시지 전송 실패했지만 수집은 완료됨 - dataId: {}", dataId, throwable);
                            return false;
                        });

                savedCount++;

            } catch (Exception e) {
                log.error("아이템 저장 실패 - title: {}", item.getTitle(), e);
            }
        }

        CollectionResult result = new CollectionResult(
                items.size(),
                savedCount,
                duplicateCount
        );

        log.info("RSS 피드 수집 완료 - {}", result);
        return result;
    }

    /**
     * 수집 통계 조회
     *
     * @return 통계 정보 (전체 개수, 소스별 개수 등)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getStats() {
        log.debug("통계 조회");

        long totalCount = rssRawDataRepository.count();
        List<String> sources = rssRawDataRepository.findAll()
                .stream()
                .map(RssRawData::getSource)
                .distinct()
                .toList();

        Map<String, Long> countBySource = new HashMap<>();
        for (String source : sources) {
            long count = rssRawDataRepository.countBySource(source);
            countBySource.put(source, count);
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", totalCount);
        stats.put("sources", sources);
        stats.put("countBySource", countBySource);

        return stats;
    }

    /**
     * 수집 결과
     */
    public record CollectionResult(
            int totalCount,      // 전체 파싱된 개수
            int savedCount,      // 저장된 개수
            int duplicateCount   // 중복된 개수
    ) {
        @Override
        public String toString() {
            return String.format("총 %d개 (저장: %d, 중복: %d)",
                    totalCount, savedCount, duplicateCount);
        }
    }
}