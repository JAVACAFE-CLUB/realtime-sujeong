package javacafe.realtime_sujeong.collection.rss.collector.crawler;

import org.jsoup.nodes.Document;

/**
 * 기사 본문 크롤링 전략 인터페이스
 * 언론사별로 다른 HTML 구조에 맞춰 본문을 추출
 */
public interface ArticleCrawlingStrategy {

    /**
     * 기사 페이지에서 본문 추출
     *
     * @param document 기사 페이지 Jsoup Document
     * @return 추출된 본문 텍스트
     */
    String extractContent(Document document);

    /**
     * 이 전략이 지원하는 소스 이름
     *
     * @return 소스 이름 (예: "chosun", "maeil", "donga")
     */
    String getSupportedSource();
}
