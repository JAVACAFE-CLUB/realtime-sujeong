package javacafe.realtime_sujeong.collection.rss.collector.crawler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 동아일보 기사 본문 크롤링 전략
 */
@Slf4j
@Component
public class DongaArticleStrategy extends BaseArticleCrawlingStrategy {

    private static final String SOURCE = "donga";
    private static final String FEED_URL = "https://rss.donga.com/total.xml";

    @Override
    protected String getContentSelector() {
        // 동아일보 기사 본문 CSS 선택자
        return ".article_txt, .news_view, #article_contents";
    }

    @Override
    protected String getRemoveSelector() {
        // 제거할 요소: 광고, 관련기사, 스크립트 등
        return ".ad, .related, .more-news, script, style";
    }

    @Override
    public String getSupportedSource() {
        return SOURCE;
    }

    @Override
    public String getFeedUrl() {
        return FEED_URL;
    }
}