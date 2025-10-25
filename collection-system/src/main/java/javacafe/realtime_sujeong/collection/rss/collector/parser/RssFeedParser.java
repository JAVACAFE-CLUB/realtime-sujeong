package javacafe.realtime_sujeong.collection.rss.collector.parser;

import javacafe.realtime_sujeong.collection.rss.collector.dto.RssItemDto;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * RSS 피드 파서
 * Jsoup을 사용하여 RSS XML을 파싱
 */
@Slf4j
@Component
public class RssFeedParser {

    private static final int CONNECTION_TIMEOUT = 10000; // 10초

    /**
     * RSS 피드 URL에서 아이템 목록 파싱
     *
     * @param feedUrl RSS 피드 URL
     * @param source 데이터 소스 (언론사)
     * @return 파싱된 RSS 아이템 리스트
     */
    public List<RssItemDto> parse(String feedUrl, String source) {
        List<RssItemDto> items = new ArrayList<>();

        try {
            log.info("RSS 피드 파싱 시작: {} (source: {})", feedUrl, source);

            Document doc = Jsoup.connect(feedUrl)
                    .timeout(CONNECTION_TIMEOUT)
                    .ignoreContentType(true)
                    .get();

            Elements itemElements = doc.select("item");
            log.info("파싱된 아이템 개수: {}", itemElements.size());

            for (Element item : itemElements) {
                try {
                    RssItemDto rssItem = parseItem(item, source);
                    items.add(rssItem);
                } catch (Exception e) {
                    log.warn("아이템 파싱 실패: {}", e.getMessage());
                }
            }

            log.info("RSS 피드 파싱 완료: {} 개 아이템", items.size());

        } catch (Exception e) {
            log.error("RSS 피드 파싱 오류: {}", feedUrl, e);
            throw new RssParsingException("RSS 피드 파싱 실패: " + feedUrl, e);
        }

        return items;
    }

    /**
     * 개별 item 엘리먼트 파싱
     */
    private RssItemDto parseItem(Element item, String source) {
        String title = getElementText(item, "title");
        String link = getElementText(item, "link");
        String description = getElementText(item, "description");
        String pubDateStr = getElementText(item, "pubDate");

        LocalDateTime pubDate = parsePubDate(pubDateStr);

        return RssItemDto.builder()
                .title(cleanText(title))
                .link(link)
                .description(cleanText(description))
                .pubDate(pubDate)
                .source(source)
                .build();
    }

    /**
     * Element에서 텍스트 추출
     */
    private String getElementText(Element parent, String tagName) {
        Element element = parent.selectFirst(tagName);
        return element != null ? element.text() : "";
    }

    /**
     * pubDate 파싱 (RFC 822 형식)
     * 예: "Tue, 25 Oct 2025 10:30:00 +0900"
     */
    private LocalDateTime parsePubDate(String pubDateStr) {
        try {
            if (pubDateStr == null || pubDateStr.isEmpty()) {
                return LocalDateTime.now();
            }

            // RFC 822 형식 파싱
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(
                    pubDateStr,
                    DateTimeFormatter.RFC_1123_DATE_TIME
            );

            return zonedDateTime.toLocalDateTime();

        } catch (Exception e) {
            log.warn("pubDate 파싱 실패: {}, 현재 시각 사용", pubDateStr);
            return LocalDateTime.now();
        }
    }

    /**
     * 텍스트 정리 (CDATA, HTML 태그 제거)
     */
    private String cleanText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // CDATA 제거
        text = text.replaceAll("<!\\[CDATA\\[", "").replaceAll("]]>", "");

        // HTML 태그 제거
        text = Jsoup.parse(text).text();

        return text.trim();
    }

    /**
     * RSS 파싱 예외
     */
    public static class RssParsingException extends RuntimeException {
        public RssParsingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}