package javacafe.realtime_sujeong.cleaning.service;

import javacafe.realtime_sujeong.cleaning.domain.CleanedData;
import javacafe.realtime_sujeong.cleaning.domain.CleanedDataRepository;
import javacafe.realtime_sujeong.cleaning.service.fetcher.RawDataFetcher;
import javacafe.realtime_sujeong.cleaning.service.fetcher.RawDataFetcherFactory;
import javacafe.realtime_sujeong.cleaning.service.fetcher.dto.RawDataContent;
import javacafe.realtime_sujeong.cleaning.service.processor.LanguageDetector;
import javacafe.realtime_sujeong.cleaning.service.processor.TextNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * CleaningService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CleaningService 테스트")
class CleaningServiceTest {

    @Mock
    private RawDataFetcherFactory fetcherFactory;

    @Mock
    private TextNormalizer textNormalizer;

    @Mock
    private LanguageDetector languageDetector;

    @Mock
    private CleanedDataRepository cleanedDataRepository;

    @Mock
    private RawDataFetcher rssFetcher;

    @Mock
    private RawDataFetcher wikiFetcher;

    @InjectMocks
    private CleaningService cleaningService;

    private List<Map<String, Object>> sampleMessages;
    private List<RawDataContent> sampleRawDataList;

    @BeforeEach
    void setUp() {
        // 샘플 메시지 준비 (RSS 2개, Wiki 1개)
        sampleMessages = new ArrayList<>();

        // RSS 메시지 1
        Map<String, Object> rssMessage1 = new HashMap<>();
        rssMessage1.put("dataId", "rss-id-1");
        rssMessage1.put("source", "rss");
        sampleMessages.add(rssMessage1);

        // RSS 메시지 2
        Map<String, Object> rssMessage2 = new HashMap<>();
        rssMessage2.put("dataId", "rss-id-2");
        rssMessage2.put("source", "rss");
        sampleMessages.add(rssMessage2);

        // Wiki 메시지
        Map<String, Object> wikiMessage = new HashMap<>();
        wikiMessage.put("dataId", "wiki-id-1");
        wikiMessage.put("source", "wiki");
        sampleMessages.add(wikiMessage);

        // 샘플 원본 데이터 준비
        sampleRawDataList = new ArrayList<>();

        sampleRawDataList.add(RawDataContent.builder()
                .dataId("rss-id-1")
                .source("rss")
                .title("뉴스 제목 1")
                .content("<p>뉴스 본문 내용입니다. 이것은 충분히 긴 텍스트입니다.</p>")
                .url("https://news.example.com/1")
                .build());

        sampleRawDataList.add(RawDataContent.builder()
                .dataId("rss-id-2")
                .source("rss")
                .title("뉴스 제목 2")
                .content("<p>또 다른 뉴스 본문 내용입니다. 이것도 충분히 깁니다.</p>")
                .url("https://news.example.com/2")
                .build());

        sampleRawDataList.add(RawDataContent.builder()
                .dataId("wiki-id-1")
                .source("wiki")
                .title("위키 문서")
                .content("위키 본문 내용입니다. Tika로 변환된 텍스트입니다. 충분히 길어야 합니다.")
                .url(null)
                .build());
    }

    @Test
    @DisplayName("정상 배치 처리 - RSS와 Wiki 혼합")
    void processBatch_Success() {
        // Given
        when(fetcherFactory.getFetcher("rss")).thenReturn(rssFetcher);
        when(fetcherFactory.getFetcher("wiki")).thenReturn(wikiFetcher);

        when(rssFetcher.fetchContentBatch(anyList()))
                .thenReturn(sampleRawDataList.subList(0, 2)); // RSS 2개

        when(wikiFetcher.fetchContentBatch(anyList()))
                .thenReturn(sampleRawDataList.subList(2, 3)); // Wiki 1개

        // TextNormalizer 모킹
        when(textNormalizer.normalize(anyString()))
                .thenAnswer(invocation -> {
                    String input = invocation.getArgument(0);
                    return input.replaceAll("<[^>]*>", "").trim();
                });

        when(textNormalizer.isValid(anyString(), anyInt())).thenReturn(true);

        // LanguageDetector 모킹
        when(languageDetector.detect(anyString())).thenReturn("ko");
        when(languageDetector.getLanguageName("ko")).thenReturn("Korean");

        // Repository 모킹
        when(cleanedDataRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        List<CleanedData> result = cleaningService.processBatch(sampleMessages);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).allMatch(data -> data.getLanguage().equals("ko"));
        assertThat(result).allMatch(data -> data.getProcessedAt() != null);

        // RSS 데이터 검증
        assertThat(result).filteredOn(data -> data.getSource().equals("rss"))
                .hasSize(2)
                .allMatch(data -> data.getUrl() != null);

        // Wiki 데이터 검증
        assertThat(result).filteredOn(data -> data.getSource().equals("wiki"))
                .hasSize(1)
                .allMatch(data -> data.getUrl() == null);

        // Mock 호출 검증
        verify(fetcherFactory, times(1)).getFetcher("rss");
        verify(fetcherFactory, times(1)).getFetcher("wiki");
        verify(rssFetcher, times(1)).fetchContentBatch(anyList());
        verify(wikiFetcher, times(1)).fetchContentBatch(anyList());
        verify(textNormalizer, times(3)).normalize(anyString());
        verify(languageDetector, times(3)).detect(anyString());
        verify(cleanedDataRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("빈 메시지 리스트 처리")
    void processBatch_EmptyList() {
        // Given
        List<Map<String, Object>> emptyMessages = Collections.emptyList();

        // When
        List<CleanedData> result = cleaningService.processBatch(emptyMessages);

        // Then
        assertThat(result).isEmpty();
        verify(fetcherFactory, never()).getFetcher(anyString());
        verify(cleanedDataRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Null 메시지 리스트 처리")
    void processBatch_NullList() {
        // When
        List<CleanedData> result = cleaningService.processBatch(null);

        // Then
        assertThat(result).isEmpty();
        verify(fetcherFactory, never()).getFetcher(anyString());
    }

    @Test
    @DisplayName("유효하지 않은 메시지 필터링")
    void processBatch_InvalidMessages() {
        // Given
        List<Map<String, Object>> messages = new ArrayList<>();

        // dataId 없음
        Map<String, Object> invalidMessage1 = new HashMap<>();
        invalidMessage1.put("source", "rss");
        messages.add(invalidMessage1);

        // source 없음
        Map<String, Object> invalidMessage2 = new HashMap<>();
        invalidMessage2.put("dataId", "test-id");
        messages.add(invalidMessage2);

        // 지원하지 않는 source
        Map<String, Object> invalidMessage3 = new HashMap<>();
        invalidMessage3.put("dataId", "test-id-2");
        invalidMessage3.put("source", "unknown");
        messages.add(invalidMessage3);

        // When
        List<CleanedData> result = cleaningService.processBatch(messages);

        // Then
        assertThat(result).isEmpty();
        verify(fetcherFactory, never()).getFetcher(anyString());
    }

    @Test
    @DisplayName("정제 후 컨텐츠가 너무 짧은 경우 제외")
    void processBatch_TooShortContent() {
        // Given
        when(fetcherFactory.getFetcher("rss")).thenReturn(rssFetcher);

        List<RawDataContent> shortContentList = List.of(
                RawDataContent.builder()
                        .dataId("rss-id-1")
                        .source("rss")
                        .title("짧은 뉴스")
                        .content("짧음")
                        .url("https://news.example.com/1")
                        .build()
        );

        when(rssFetcher.fetchContentBatch(anyList())).thenReturn(shortContentList);
        when(textNormalizer.normalize(anyString())).thenReturn("짧음");
        when(textNormalizer.isValid(anyString(), anyInt())).thenReturn(false); // 너무 짧음

        // When
        List<CleanedData> result = cleaningService.processBatch(sampleMessages.subList(0, 1));

        // Then
        assertThat(result).isEmpty();
        // cleanedDataList가 비어있으면 saveAll이 호출되지 않음 (로직상 return)
        verify(cleanedDataRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("MongoDB 조회 실패 시 해당 source 스킵")
    void processBatch_FetcherException() {
        // Given
        when(fetcherFactory.getFetcher("rss")).thenThrow(new IllegalArgumentException("Unsupported source"));
        when(fetcherFactory.getFetcher("wiki")).thenReturn(wikiFetcher);

        when(wikiFetcher.fetchContentBatch(anyList()))
                .thenReturn(sampleRawDataList.subList(2, 3)); // Wiki만 성공

        when(textNormalizer.normalize(anyString())).thenReturn("정제된 텍스트입니다. 충분히 깁니다.");
        when(textNormalizer.isValid(anyString(), anyInt())).thenReturn(true);
        when(languageDetector.detect(anyString())).thenReturn("ko");
        when(languageDetector.getLanguageName("ko")).thenReturn("Korean");
        when(cleanedDataRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        List<CleanedData> result = cleaningService.processBatch(sampleMessages);

        // Then
        assertThat(result).hasSize(1); // Wiki만 처리됨
        assertThat(result.get(0).getSource()).isEqualTo("wiki");
    }

    @Test
    @DisplayName("MongoDB 배치 저장 실패 시 Fallback 실행")
    void processBatch_SaveFallback() {
        // Given
        when(fetcherFactory.getFetcher("rss")).thenReturn(rssFetcher);
        when(rssFetcher.fetchContentBatch(anyList()))
                .thenReturn(sampleRawDataList.subList(0, 1)); // RSS 1개만

        when(textNormalizer.normalize(anyString())).thenReturn("정제된 텍스트입니다. 충분히 깁니다.");
        when(textNormalizer.isValid(anyString(), anyInt())).thenReturn(true);
        when(languageDetector.detect(anyString())).thenReturn("ko");
        when(languageDetector.getLanguageName("ko")).thenReturn("Korean");

        // saveAll 실패, save는 성공
        when(cleanedDataRepository.saveAll(anyList()))
                .thenThrow(new RuntimeException("Batch save failed"));
        when(cleanedDataRepository.save(any(CleanedData.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        List<CleanedData> result = cleaningService.processBatch(sampleMessages.subList(0, 1));

        // Then
        assertThat(result).hasSize(1);
        verify(cleanedDataRepository, times(1)).saveAll(anyList()); // 배치 시도
        verify(cleanedDataRepository, times(1)).save(any(CleanedData.class)); // Fallback 실행
    }

    @Test
    @DisplayName("메타데이터 생성 확인 - RSS")
    void processBatch_MetadataForRss() {
        // Given
        when(fetcherFactory.getFetcher("rss")).thenReturn(rssFetcher);
        when(rssFetcher.fetchContentBatch(anyList()))
                .thenReturn(sampleRawDataList.subList(0, 1));

        String originalContent = sampleRawDataList.get(0).getContent();
        String cleanedContent = "뉴스 본문 내용입니다. 이것은 충분히 긴 텍스트입니다.";

        when(textNormalizer.normalize(anyString())).thenReturn(cleanedContent);
        when(textNormalizer.isValid(anyString(), anyInt())).thenReturn(true);
        when(languageDetector.detect(anyString())).thenReturn("ko");
        when(languageDetector.getLanguageName("ko")).thenReturn("Korean");
        when(cleanedDataRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        List<CleanedData> result = cleaningService.processBatch(sampleMessages.subList(0, 1));

        // Then
        assertThat(result).hasSize(1);
        CleanedData cleanedData = result.get(0);

        assertThat(cleanedData.getMetadata()).isNotNull();
        assertThat(cleanedData.getMetadata().get("originalLength")).isEqualTo(originalContent.length());
        assertThat(cleanedData.getMetadata().get("cleanedLength")).isEqualTo(cleanedContent.length());
        assertThat(cleanedData.getMetadata().get("url")).isEqualTo("https://news.example.com/1");
    }

    @Test
    @DisplayName("메타데이터 생성 확인 - Wiki")
    void processBatch_MetadataForWiki() {
        // Given
        when(fetcherFactory.getFetcher("wiki")).thenReturn(wikiFetcher);
        when(wikiFetcher.fetchContentBatch(anyList()))
                .thenReturn(sampleRawDataList.subList(2, 3));

        String cleanedContent = "위키 본문 내용입니다. Tika로 변환된 텍스트입니다. 충분히 길어야 합니다.";

        when(textNormalizer.normalize(anyString())).thenReturn(cleanedContent);
        when(textNormalizer.isValid(anyString(), anyInt())).thenReturn(true);
        when(languageDetector.detect(anyString())).thenReturn("ko");
        when(languageDetector.getLanguageName("ko")).thenReturn("Korean");
        when(cleanedDataRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        List<CleanedData> result = cleaningService.processBatch(sampleMessages.subList(2, 3));

        // Then
        assertThat(result).hasSize(1);
        CleanedData cleanedData = result.get(0);

        assertThat(cleanedData.getMetadata()).isNotNull();
        assertThat(cleanedData.getMetadata().get("extracted")).isEqualTo("tika");
        assertThat(cleanedData.getUrl()).isNull(); // Wiki는 URL 없음
    }

    @Test
    @DisplayName("대량 메시지 처리 (100개)")
    void processBatch_LargeBatch() {
        // Given
        List<Map<String, Object>> largeMessages = new ArrayList<>();
        List<RawDataContent> rssDataList = new ArrayList<>();
        List<RawDataContent> wikiDataList = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            Map<String, Object> message = new HashMap<>();
            message.put("dataId", "data-id-" + i);
            message.put("source", i % 2 == 0 ? "rss" : "wiki");
            largeMessages.add(message);

            RawDataContent rawData = RawDataContent.builder()
                    .dataId("data-id-" + i)
                    .source(i % 2 == 0 ? "rss" : "wiki")
                    .title("제목 " + i)
                    .content("충분히 긴 본문 내용입니다. 테스트용 데이터 번호 " + i)
                    .url(i % 2 == 0 ? "https://example.com/" + i : null)
                    .build();

            if (i % 2 == 0) {
                rssDataList.add(rawData);
            } else {
                wikiDataList.add(rawData);
            }
        }

        when(fetcherFactory.getFetcher("rss")).thenReturn(rssFetcher);
        when(fetcherFactory.getFetcher("wiki")).thenReturn(wikiFetcher);
        when(rssFetcher.fetchContentBatch(anyList())).thenReturn(rssDataList);
        when(wikiFetcher.fetchContentBatch(anyList())).thenReturn(wikiDataList);
        when(textNormalizer.normalize(anyString())).thenReturn("정제된 텍스트입니다. 충분히 깁니다.");
        when(textNormalizer.isValid(anyString(), anyInt())).thenReturn(true);
        when(languageDetector.detect(anyString())).thenReturn("ko");
        when(languageDetector.getLanguageName("ko")).thenReturn("Korean");
        when(cleanedDataRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        List<CleanedData> result = cleaningService.processBatch(largeMessages);

        // Then
        assertThat(result).hasSize(100);
        verify(cleanedDataRepository, times(1)).saveAll(anyList()); // Bulk operation
    }
}
