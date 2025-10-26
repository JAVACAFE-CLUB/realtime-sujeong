package javacafe.realtime_sujeong.collection.rss.collector.crawler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 매일경제 기사 본문 크롤링 전략
 */
@Slf4j
@Component
public class MaeilArticleStrategy extends BaseArticleCrawlingStrategy {

    private static final String SOURCE = "maeil";
    private static final String FEED_URL = "https://www.mk.co.kr/rss/30000001/";

    @Override
    protected String getContentSelector() {
        // 매일경제 기사 본문 CSS 선택자
        return "#article-view-content-div, #art_txt, .news_cnt_detail_wrap";
    }

    @Override
    protected String getRemoveSelector() {
        // 제거할 요소: 광고, 관련기사, 스크립트 등
        return ".ad, .relate_news, .btn-wrap, script, style, .reporter-info";
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