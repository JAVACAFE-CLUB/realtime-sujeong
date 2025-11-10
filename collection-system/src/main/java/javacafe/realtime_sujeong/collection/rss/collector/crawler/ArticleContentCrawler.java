package javacafe.realtime_sujeong.collection.rss.collector.crawler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

/**
 * 기사 본문 크롤러
 * URL에서 HTML을 가져와 전략 패턴으로 본문 추출
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleContentCrawler {

    private static final int CONNECTION_TIMEOUT = 10000; // 10초

    private final ArticleCrawlingStrategyFactory strategyFactory;

    /**
     * 기사 URL에서 본문 크롤링
     *
     * @param articleUrl 기사 URL
     * @param source 데이터 소스 (언론사)
     * @return 추출된 본문 텍스트
     */
    public String crawl(String articleUrl, String source) {
        try {
            log.debug("기사 본문 크롤링 시작: {} (source: {})", articleUrl, source);

            // 1. URL에서 Document 가져오기
            Document doc = Jsoup.connect(articleUrl)
                    .timeout(CONNECTION_TIMEOUT)
                    .userAgent("Mozilla/5.0") // User-Agent 설정
                    .get();

            // 2. 소스별 전략 선택
            ArticleCrawlingStrategy strategy = strategyFactory.getStrategy(source);

            // 3. 전략을 사용하여 본문 추출
            String content = strategy.extractContent(doc);

            if (content.isEmpty()) {
                log.warn("본문이 비어있습니다: {}", articleUrl);
            } else {
                log.debug("기사 본문 크롤링 완료: {} 글자 (source: {})", content.length(), source);
            }

            return content;

        } catch (Exception e) {
            log.error("기사 본문 크롤링 실패: {} - {}", articleUrl, e.getMessage());
            return ""; // 크롤링 실패시 빈 문자열 반환
        }
    }
}