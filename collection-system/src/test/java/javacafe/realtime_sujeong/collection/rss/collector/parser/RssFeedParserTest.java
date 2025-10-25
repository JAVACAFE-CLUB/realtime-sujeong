package javacafe.realtime_sujeong.collection.rss.collector.parser;

import javacafe.realtime_sujeong.collection.rss.collector.dto.RssItemDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RssFeedParser 테스트")
class RssFeedParserTest {

    private RssFeedParser parser;

    @BeforeEach
    void setUp() {
        parser = new RssFeedParser();
    }

    @Test
    @DisplayName("RSS 피드 파싱 - 실제 조선일보 RSS (통합 테스트)")
    void parse_RealChosunRssFeed() {
        // given
        String feedUrl = "https://www.chosun.com/arc/outboundfeeds/rss/?outputType=xml";
        String source = "chosun";

        // when
        List<RssItemDto> items = parser.parse(feedUrl, source);

        // then
        assertThat(items).isNotEmpty();

        RssItemDto firstItem = items.get(0);
        assertThat(firstItem.getTitle()).isNotBlank();
        assertThat(firstItem.getLink()).startsWith("http");
        assertThat(firstItem.getPubDate()).isNotNull();
        assertThat(firstItem.getSource()).isEqualTo(source);
    }

    @Test
    @DisplayName("RSS 피드 파싱 - 잘못된 URL이면 예외 발생")
    void parse_InvalidUrl_ThrowsException() {
        // given
        String invalidUrl = "http://localhost:9999/nonexistent";
        String source = "test";

        // when & then
        assertThatThrownBy(() -> parser.parse(invalidUrl, source))
                .isInstanceOf(RssFeedParser.RssParsingException.class)
                .hasMessageContaining("RSS 피드 파싱 실패");
    }

    @Test
    @DisplayName("RSS 피드 파싱 - 실제 매일경제 RSS (통합 테스트)")
    void parse_RealMaeilRssFeed() {
        // given
        String feedUrl = "https://www.mk.co.kr/rss/30000001/";
        String source = "maeil";

        // when
        List<RssItemDto> items = parser.parse(feedUrl, source);

        // then
        assertThat(items).isNotEmpty();

        items.forEach(item -> {
            assertThat(item.getTitle()).isNotBlank();
            assertThat(item.getLink()).isNotBlank();
            assertThat(item.getSource()).isEqualTo(source);
        });
    }
}