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
import java.util.Locale;

/**
 * RSS 피드 파서
 * Jsoup을 사용하여 표준 RSS XML 형식을 파싱
 */
@Slf4j
@Component
public class RssFeedParser {

    private static final int CONNECTION_TIMEOUT = 10000; // 10초
    private static final DateTimeFormatter RSS_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

    /**
     * RSS 피드 URL에서 아이템 목록 파싱
     *
     * @param feedUrl RSS 피드 URL
     * @param source 데이터 소스 (언론사)
     * @return 파싱된 RSS 아이템 리스트
     */
    public List<RssItemDto> parse(String feedUrl, String source) {
        try {
            log.info("RSS 피드 파싱 시작: {} (source: {})", feedUrl, source);

            // 1. URL에서 Document 가져오기
            Document doc = Jsoup.connect(feedUrl)
                    .timeout(CONNECTION_TIMEOUT)
                    .ignoreContentType(true)
                    .get();

            // 2. 표준 RSS 형식 파싱
            List<RssItemDto> items = parseRssItems(doc, source);

            log.info("RSS 피드 파싱 완료: {} 개 아이템 (source: {})", items.size(), source);

            return items;

        } catch (Exception e) {
            log.error("RSS 피드 파싱 오류: {}", feedUrl, e);
            throw new RssParsingException("RSS 피드 파싱 실패: " + feedUrl, e);
        }
    }

    /**
     * RSS Document에서 item 목록 파싱
     */
    private List<RssItemDto> parseRssItems(Document doc, String source) {
        List<RssItemDto> items = new ArrayList<>();
        Elements itemElements = doc.select("item");

        for (Element itemElement : itemElements) {
            try {
                RssItemDto item = parseRssItem(itemElement, source);
                items.add(item);
            } catch (Exception e) {
                log.warn("RSS 아이템 파싱 실패 (건너뜀): {}", e.getMessage());
            }
        }

        return items;
    }

    /**
     * 개별 RSS item 엘리먼트 파싱
     */
    private RssItemDto parseRssItem(Element itemElement, String source) {
        String title = getElementText(itemElement, "title");
        String link = getElementText(itemElement, "link");
        String description = getElementText(itemElement, "description");
        String pubDateStr = getElementText(itemElement, "pubDate");

        LocalDateTime pubDate = parseDate(pubDateStr);

        return RssItemDto.builder()
                .title(title)
                .link(link)
                .description(description)
                .pubDate(pubDate)
                .source(source)
                .build();
    }

    /**
     * Element에서 특정 태그의 텍스트 추출
     */
    private String getElementText(Element element, String tagName) {
        Element child = element.selectFirst(tagName);
        return child != null ? child.text() : "";
    }

    /**
     * RSS 날짜 문자열을 LocalDateTime으로 변환
     * RFC 822 형식: "Mon, 30 Sep 2025 14:30:00 +0900"
     */
    private LocalDateTime parseDate(String dateStr) {
        try {
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateStr, RSS_DATE_FORMATTER);
            return zonedDateTime.toLocalDateTime();
        } catch (Exception e) {
            log.warn("날짜 파싱 실패, 현재 시간 사용: {}", dateStr);
            return LocalDateTime.now();
        }
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