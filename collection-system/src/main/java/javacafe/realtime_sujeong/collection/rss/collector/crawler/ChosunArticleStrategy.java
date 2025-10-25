package javacafe.realtime_sujeong.collection.rss.collector.crawler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 조선일보 기사 본문 크롤링 전략
 */
@Slf4j
@Component
public class ChosunArticleStrategy extends BaseArticleCrawlingStrategy {

    private static final String SOURCE = "chosun";

    @Override
    protected String getContentSelector() {
        // 조선일보 기사 본문 CSS 선택자
        return "article .article-body, .article__body, #articleBody";
    }

    @Override
    protected String getRemoveSelector() {
        // 제거할 요소: 광고, 관련기사, 버튼 등
        return ".ad, .related-article, .button-wrap, script, style";
    }

    @Override
    public String getSupportedSource() {
        return SOURCE;
    }
}