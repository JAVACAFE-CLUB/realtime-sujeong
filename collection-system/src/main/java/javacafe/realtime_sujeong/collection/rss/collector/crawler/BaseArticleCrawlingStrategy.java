package javacafe.realtime_sujeong.collection.rss.collector.crawler;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * 기사 본문 크롤링 Base 전략
 * 공통 로직 제공, 하위 클래스에서 CSS 선택자만 정의
 */
@Slf4j
public abstract class BaseArticleCrawlingStrategy implements ArticleCrawlingStrategy {

    /**
     * 본문을 추출할 CSS 선택자
     * 하위 클래스에서 구현 필요
     */
    protected abstract String getContentSelector();

    /**
     * 제거할 요소의 CSS 선택자 (광고, 관련기사 등)
     * 기본값: 없음, 필요시 오버라이드
     */
    protected String getRemoveSelector() {
        return null;
    }

    @Override
    public String extractContent(Document document) {
        try {
            // 1. 제거할 요소가 있다면 먼저 제거
            String removeSelector = getRemoveSelector();
            if (removeSelector != null && !removeSelector.isEmpty()) {
                document.select(removeSelector).remove();
            }

            // 2. 본문 선택
            Elements contentElements = document.select(getContentSelector());

            if (contentElements.isEmpty()) {
                log.warn("본문을 찾을 수 없습니다. Selector: {}", getContentSelector());
                return "";
            }

            // 3. 텍스트 추출 및 정리
            StringBuilder content = new StringBuilder();
            for (Element element : contentElements) {
                String text = element.text();
                if (!text.isEmpty()) {
                    content.append(text).append("\n");
                }
            }

            String result = cleanContent(content.toString());
            log.debug("본문 추출 완료: {} 글자", result.length());

            return result;

        } catch (Exception e) {
            log.error("본문 추출 실패: {}", e.getMessage(), e);
            return "";
        }
    }

    /**
     * 본문 텍스트 정리
     * 공백, 줄바꿈 정규화
     */
    protected String cleanContent(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        // 연속된 공백 제거
        content = content.replaceAll("\\s+", " ");

        // 앞뒤 공백 제거
        content = content.trim();

        return content;
    }
}