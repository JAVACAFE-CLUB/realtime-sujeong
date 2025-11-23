package javacafe.realtime_sujeong.collection.rss.service;

import javacafe.realtime_sujeong.collection.kafka.service.KafkaMessageService;
import javacafe.realtime_sujeong.collection.rss.collector.crawler.ArticleContentCrawler;
import javacafe.realtime_sujeong.collection.rss.collector.crawler.ArticleCrawlingStrategy;
import javacafe.realtime_sujeong.collection.rss.collector.crawler.ArticleCrawlingStrategyFactory;
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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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
    private ArticleContentCrawler articleContentCrawler;

    @Mock
    private ArticleCrawlingStrategyFactory strategyFactory;

    @Mock
    private ArticleCrawlingStrategy mockStrategy;

    @Mock
    private RssRawDataRepository rssRawDataRepository;

    @Mock
    private KafkaMessageService kafkaMessageService;

    @InjectMocks
    private RssCollectionService rssCollectionService;

    @Test
    @DisplayName("RSS 피드 수집 - 정상 케이스 (신규 데이터)")
    void collectFeed_Success() {
        // given
        String feedUrl = "https://example.com/rss";
        String source = "test";

        List<RssItemDto> mockItems = List.of(
                createMockItem("제목1", "https://example.com/1", source),
                createMockItem("제목2", "https://example.com/2", source),
                createMockItem("제목3", "https://example.com/3", source)
        );

        given(strategyFactory.getStrategy(source)).willReturn(mockStrategy);
        given(mockStrategy.getFeedUrl()).willReturn(feedUrl);
        given(rssFeedParser.parse(feedUrl, source)).willReturn(mockItems);
        given(articleContentCrawler.crawl(anyString(), anyString())).willReturn("테스트 기사 본문 내용");
        // 신규 데이터 - findByDataId가 empty 반환
        given(rssRawDataRepository.findByDataId(anyString())).willReturn(Optional.empty());
        given(rssRawDataRepository.save(any(RssRawData.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(kafkaMessageService.sendDataCollectedMessage(anyString(), anyString(), anyString(), any()))
                .willReturn(CompletableFuture.completedFuture(true));

        // when
        RssCollectionService.CollectionResult result = rssCollectionService.collectFeed(source);

        // then
        assertThat(result.totalCount()).isEqualTo(3);
        assertThat(result.savedCount()).isEqualTo(3);
        assertThat(result.duplicateCount()).isEqualTo(0);

        verify(strategyFactory).getStrategy(source);
        verify(mockStrategy).getFeedUrl();
        verify(rssFeedParser).parse(feedUrl, source);
        verify(articleContentCrawler, times(3)).crawl(anyString(), anyString());
        verify(rssRawDataRepository, times(3)).findByDataId(anyString());
        verify(rssRawDataRepository, times(3)).save(any(RssRawData.class));
    }

    @Test
    @DisplayName("RSS 피드 수집 - 기존 데이터가 더 최신이면 스킵")
    void collectFeed_SkipOlderData() {
        // given
        String feedUrl = "https://example.com/rss";
        String source = "test";
        LocalDateTime olderPubDate = LocalDateTime.now().minusHours(1);
        LocalDateTime newerPubDate = LocalDateTime.now();

        List<RssItemDto> mockItems = List.of(
                createMockItem("제목1", "https://example.com/1", source, olderPubDate)
        );

        // 기존 데이터가 더 최신
        RssRawData existingData = RssRawData.builder()
                .dataId("https://example.com/1")
                .source(source)
                .title("제목1")
                .link("https://example.com/1")
                .pubDate(newerPubDate)  // 기존 데이터가 더 최신
                .description("설명")
                .content("본문")
                .build();

        given(strategyFactory.getStrategy(source)).willReturn(mockStrategy);
        given(mockStrategy.getFeedUrl()).willReturn(feedUrl);
        given(rssFeedParser.parse(feedUrl, source)).willReturn(mockItems);
        given(rssRawDataRepository.findByDataId("https://example.com/1"))
                .willReturn(Optional.of(existingData));

        // when
        RssCollectionService.CollectionResult result = rssCollectionService.collectFeed(source);

        // then
        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.savedCount()).isEqualTo(0);
        assertThat(result.duplicateCount()).isEqualTo(1);

        // 크롤링, 저장, Kafka 전송 안 함
        verify(articleContentCrawler, times(0)).crawl(anyString(), anyString());
        verify(rssRawDataRepository, times(0)).save(any(RssRawData.class));
    }

    @Test
    @DisplayName("RSS 피드 수집 - 기존 데이터가 더 오래되면 업데이트")
    void collectFeed_UpdateNewerData() {
        // given
        String feedUrl = "https://example.com/rss";
        String source = "test";
        LocalDateTime olderPubDate = LocalDateTime.now().minusHours(1);
        LocalDateTime newerPubDate = LocalDateTime.now();

        List<RssItemDto> mockItems = List.of(
                createMockItem("제목1 업데이트", "https://example.com/1", source, newerPubDate)
        );

        // 기존 데이터가 더 오래됨
        RssRawData existingData = RssRawData.builder()
                .dataId("https://example.com/1")
                .source(source)
                .title("제목1")
                .link("https://example.com/1")
                .pubDate(olderPubDate)  // 기존 데이터가 더 오래됨
                .description("설명")
                .content("본문")
                .build();

        given(strategyFactory.getStrategy(source)).willReturn(mockStrategy);
        given(mockStrategy.getFeedUrl()).willReturn(feedUrl);
        given(rssFeedParser.parse(feedUrl, source)).willReturn(mockItems);
        given(articleContentCrawler.crawl(anyString(), anyString())).willReturn("업데이트된 기사 본문");
        given(rssRawDataRepository.findByDataId("https://example.com/1"))
                .willReturn(Optional.of(existingData));
        given(rssRawDataRepository.save(any(RssRawData.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(kafkaMessageService.sendDataCollectedMessage(anyString(), anyString(), anyString(), any()))
                .willReturn(CompletableFuture.completedFuture(true));

        // when
        RssCollectionService.CollectionResult result = rssCollectionService.collectFeed(source);

        // then
        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.savedCount()).isEqualTo(1);
        assertThat(result.duplicateCount()).isEqualTo(0);

        // 크롤링 및 저장 수행
        verify(articleContentCrawler, times(1)).crawl(anyString(), anyString());
        verify(rssRawDataRepository, times(1)).save(any(RssRawData.class));
    }

    @Test
    @DisplayName("RSS 피드 수집 - 빈 피드")
    void collectFeed_EmptyFeed() {
        // given
        String feedUrl = "https://example.com/rss";
        String source = "test";

        given(strategyFactory.getStrategy(source)).willReturn(mockStrategy);
        given(mockStrategy.getFeedUrl()).willReturn(feedUrl);
        given(rssFeedParser.parse(feedUrl, source)).willReturn(List.of());

        // when
        RssCollectionService.CollectionResult result = rssCollectionService.collectFeed(source);

        // then
        assertThat(result.totalCount()).isEqualTo(0);
        assertThat(result.savedCount()).isEqualTo(0);
        assertThat(result.duplicateCount()).isEqualTo(0);

        verify(rssFeedParser).parse(feedUrl, source);
        verify(rssRawDataRepository, times(0)).save(any(RssRawData.class));
    }

    private RssItemDto createMockItem(String title, String link, String source) {
        return createMockItem(title, link, source, LocalDateTime.now());
    }

    private RssItemDto createMockItem(String title, String link, String source, LocalDateTime pubDate) {
        return RssItemDto.builder()
                .title(title)
                .link(link)
                .pubDate(pubDate)
                .description("테스트 설명")
                .source(source)
                .build();
    }
}
