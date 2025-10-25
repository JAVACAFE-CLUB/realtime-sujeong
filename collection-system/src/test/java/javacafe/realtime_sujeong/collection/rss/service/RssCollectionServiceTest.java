package javacafe.realtime_sujeong.collection.rss.service;

import javacafe.realtime_sujeong.collection.rss.collector.dto.RssItemDto;
import javacafe.realtime_sujeong.collection.rss.collector.parser.RssFeedParser;
import javacafe.realtime_sujeong.collection.rss.domain.RssRawData;
import javacafe.realtime_sujeong.collection.rss.domain.RssRawDataRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RssCollectionService 테스트")
class RssCollectionServiceTest {

    @Mock
    private RssFeedParser rssFeedParser;

    @Mock
    private RssRawDataRepository rssRawDataRepository;

    @InjectMocks
    private RssCollectionService rssCollectionService;

    @Test
    @DisplayName("RSS 피드 수집 - 정상 케이스")
    void collectFeed_Success() {
        // given
        String feedUrl = "https://example.com/rss";
        String source = "test";

        List<RssItemDto> mockItems = List.of(
                createMockItem("제목1", "https://example.com/1", source),
                createMockItem("제목2", "https://example.com/2", source),
                createMockItem("제목3", "https://example.com/3", source)
        );

        given(rssFeedParser.parse(feedUrl, source)).willReturn(mockItems);
        given(rssRawDataRepository.existsByDataId(anyString())).willReturn(false);
        given(rssRawDataRepository.save(any(RssRawData.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        RssCollectionService.CollectionResult result = rssCollectionService.collectFeed(feedUrl, source);

        // then
        assertThat(result.totalCount()).isEqualTo(3);
        assertThat(result.savedCount()).isEqualTo(3);
        assertThat(result.duplicateCount()).isEqualTo(0);

        verify(rssFeedParser).parse(feedUrl, source);
        verify(rssRawDataRepository, times(3)).existsByDataId(anyString());
        verify(rssRawDataRepository, times(3)).save(any(RssRawData.class));
    }

    @Test
    @DisplayName("RSS 피드 수집 - 중복 데이터 스킵")
    void collectFeed_SkipDuplicates() {
        // given
        String feedUrl = "https://example.com/rss";
        String source = "test";

        List<RssItemDto> mockItems = List.of(
                createMockItem("제목1", "https://example.com/1", source),
                createMockItem("제목2", "https://example.com/2", source),
                createMockItem("제목3", "https://example.com/3", source)
        );

        given(rssFeedParser.parse(feedUrl, source)).willReturn(mockItems);

        // 첫 번째와 세 번째는 중복
        given(rssRawDataRepository.existsByDataId(anyString()))
                .willReturn(true)   // 첫 번째 중복
                .willReturn(false)  // 두 번째 저장
                .willReturn(true);  // 세 번째 중복

        given(rssRawDataRepository.save(any(RssRawData.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        RssCollectionService.CollectionResult result = rssCollectionService.collectFeed(feedUrl, source);

        // then
        assertThat(result.totalCount()).isEqualTo(3);
        assertThat(result.savedCount()).isEqualTo(1);
        assertThat(result.duplicateCount()).isEqualTo(2);

        verify(rssFeedParser).parse(feedUrl, source);
        verify(rssRawDataRepository, times(3)).existsByDataId(anyString());
        verify(rssRawDataRepository, times(1)).save(any(RssRawData.class));
    }

    @Test
    @DisplayName("RSS 피드 수집 - 빈 피드")
    void collectFeed_EmptyFeed() {
        // given
        String feedUrl = "https://example.com/rss";
        String source = "test";

        given(rssFeedParser.parse(feedUrl, source)).willReturn(List.of());

        // when
        RssCollectionService.CollectionResult result = rssCollectionService.collectFeed(feedUrl, source);

        // then
        assertThat(result.totalCount()).isEqualTo(0);
        assertThat(result.savedCount()).isEqualTo(0);
        assertThat(result.duplicateCount()).isEqualTo(0);

        verify(rssFeedParser).parse(feedUrl, source);
        verify(rssRawDataRepository, times(0)).save(any(RssRawData.class));
    }

    private RssItemDto createMockItem(String title, String link, String source) {
        return RssItemDto.builder()
                .title(title)
                .link(link)
                .pubDate(LocalDateTime.now())
                .description("테스트 설명")
                .source(source)
                .build();
    }
}