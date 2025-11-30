package javacafe.realtime_sujeong.indexing.keyword.service;

import javacafe.realtime_sujeong.indexing.keyword.domain.Keyword;
import javacafe.realtime_sujeong.indexing.keyword.domain.KeywordRepository;
import javacafe.realtime_sujeong.indexing.keyword.domain.KeywordScore;
import javacafe.realtime_sujeong.indexing.ner.dto.NamedEntity;
import javacafe.realtime_sujeong.indexing.ner.service.NerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * KeywordIndexingService 단위 테스트 (Mock 사용)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KeywordIndexingService 테스트")
class KeywordIndexingServiceTest {

    @Mock
    private NerService nerService;

    @Mock
    private KeywordScoreCalculator scoreCalculator;

    @Mock
    private KeywordRepository keywordRepository;

    @InjectMocks
    private KeywordIndexingService indexingService;

    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.of(2025, 1, 1);
    }

    @Test
    @DisplayName("새로운 키워드 색인 - 키워드가 존재하지 않을 때")
    void indexKeywords_NewKeyword() {
        // Given
        String title = "삼성전자, 신제품 발표";
        String content = "삼성전자가 신제품을 발표했다.";
        String source = "rss";
        String dataId = "test-id-123";

        List<NamedEntity> titleEntities = List.of(
                createEntity("삼성전자", "ORG")
        );
        List<NamedEntity> contentEntities = List.of(
                createEntity("삼성전자", "ORG"),
                createEntity("신제품", "AF")
        );

        when(nerService.extractEntitiesFromTitleAndContent(title, content))
                .thenReturn(Map.of(
                        "title", titleEntities,
                        "content", contentEntities
                ));

        List<KeywordScore> keywordScores = List.of(
                KeywordScore.builder()
                        .keyword("삼성전자")
                        .entityType("ORG")
                        .titleFrequency(1)
                        .contentFrequency(1)
                        .totalFrequency(2)
                        .score(4.5)
                        .source(source)
                        .dataId(dataId)
                        .build(),
                KeywordScore.builder()
                        .keyword("신제품")
                        .entityType("AF")
                        .titleFrequency(0)
                        .contentFrequency(1)
                        .totalFrequency(1)
                        .score(1.2)
                        .source(source)
                        .dataId(dataId)
                        .build()
        );

        when(scoreCalculator.calculateScores(titleEntities, contentEntities, source, dataId))
                .thenReturn(keywordScores);

        when(keywordRepository.findByKeywordId(anyString()))
                .thenReturn(Optional.empty()); // 키워드가 존재하지 않음

        // When
        indexingService.indexKeywords(title, content, source, dataId, testDate);

        // Then
        verify(nerService, times(1)).extractEntitiesFromTitleAndContent(title, content);
        verify(scoreCalculator, times(1)).calculateScores(titleEntities, contentEntities, source, dataId);
        verify(keywordRepository, times(2)).save(any(Keyword.class)); // 2개 키워드 저장

        // Keyword 저장 검증
        ArgumentCaptor<Keyword> keywordCaptor = ArgumentCaptor.forClass(Keyword.class);
        verify(keywordRepository, times(2)).save(keywordCaptor.capture());

        List<Keyword> savedKeywords = keywordCaptor.getAllValues();
        assertThat(savedKeywords).hasSize(2);

        Keyword samsungKeyword = savedKeywords.stream()
                .filter(k -> k.getKeyword().equals("삼성전자"))
                .findFirst()
                .orElse(null);

        assertThat(samsungKeyword).isNotNull();
        assertThat(samsungKeyword.getEntityType()).isEqualTo("ORG");
        assertThat(samsungKeyword.getScore()).isEqualTo(4.5);
        assertThat(samsungKeyword.getFrequency()).isEqualTo(2);
        assertThat(samsungKeyword.getDate()).isEqualTo(testDate);
    }

    @Test
    @DisplayName("기존 키워드 업데이트 - 키워드가 이미 존재할 때")
    void indexKeywords_ExistingKeyword() {
        // Given
        String title = "삼성전자 주가 상승";
        String content = "삼성전자 주가가 상승했다.";
        String source = "rss";
        String dataId = "test-id-456";

        List<NamedEntity> titleEntities = List.of(
                createEntity("삼성전자", "ORG")
        );
        List<NamedEntity> contentEntities = List.of(
                createEntity("삼성전자", "ORG")
        );

        when(nerService.extractEntitiesFromTitleAndContent(title, content))
                .thenReturn(Map.of(
                        "title", titleEntities,
                        "content", contentEntities
                ));

        KeywordScore keywordScore = KeywordScore.builder()
                .keyword("삼성전자")
                .entityType("ORG")
                .titleFrequency(1)
                .contentFrequency(1)
                .totalFrequency(2)
                .score(4.5)
                .source(source)
                .dataId(dataId)
                .build();

        when(scoreCalculator.calculateScores(titleEntities, contentEntities, source, dataId))
                .thenReturn(List.of(keywordScore));

        // 기존 키워드 존재
        Keyword existingKeyword = Keyword.builder()
                .keywordId("2025-01-01_삼성전자")
                .keyword("삼성전자")
                .entityType("ORG")
                .score(10.0)
                .frequency(5)
                .date(testDate)
                .createdAt(LocalDateTime.now().minusHours(1))
                .lastUpdated(LocalDateTime.now().minusHours(1))
                .build();
        existingKeyword.addSourceDataId("old-id");
        existingKeyword.addSource("wiki");

        when(keywordRepository.findByKeywordId("2025-01-01_삼성전자"))
                .thenReturn(Optional.of(existingKeyword));

        // When
        indexingService.indexKeywords(title, content, source, dataId, testDate);

        // Then
        verify(keywordRepository, times(1)).save(any(Keyword.class));

        ArgumentCaptor<Keyword> keywordCaptor = ArgumentCaptor.forClass(Keyword.class);
        verify(keywordRepository).save(keywordCaptor.capture());

        Keyword updatedKeyword = keywordCaptor.getValue();
        assertThat(updatedKeyword.getScore()).isEqualTo(14.5); // 10.0 + 4.5
        assertThat(updatedKeyword.getFrequency()).isEqualTo(7); // 5 + 2
        assertThat(updatedKeyword.getSourceDataIds()).contains("old-id", dataId);
        assertThat(updatedKeyword.getSources()).contains("wiki", "rss");
    }

    @Test
    @DisplayName("Top 10 키워드 조회")
    void getTopKeywords() {
        // Given
        List<Keyword> topKeywords = List.of(
                createKeyword("삼성전자", "ORG", 100.0),
                createKeyword("현대차", "ORG", 90.0),
                createKeyword("이재용", "PER", 80.0)
        );

        when(keywordRepository.findTop10ByDateOrderByScoreDesc(testDate))
                .thenReturn(topKeywords);

        // When
        List<Keyword> result = indexingService.getTopKeywords(testDate, 10);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getKeyword()).isEqualTo("삼성전자");
        assertThat(result.get(0).getScore()).isEqualTo(100.0);
        verify(keywordRepository, times(1)).findTop10ByDateOrderByScoreDesc(testDate);
    }

    @Test
    @DisplayName("특정 날짜의 모든 키워드 조회")
    void getKeywordsByDate() {
        // Given
        List<Keyword> keywords = List.of(
                createKeyword("삼성전자", "ORG", 100.0),
                createKeyword("현대차", "ORG", 90.0)
        );

        when(keywordRepository.findByDate(testDate))
                .thenReturn(keywords);

        // When
        List<Keyword> result = indexingService.getKeywordsByDate(testDate);

        // Then
        assertThat(result).hasSize(2);
        verify(keywordRepository, times(1)).findByDate(testDate);
    }

    @Test
    @DisplayName("개체 유형별 키워드 조회")
    void getKeywordsByEntityType() {
        // Given
        String entityType = "ORG";
        List<Keyword> orgKeywords = List.of(
                createKeyword("삼성전자", "ORG", 100.0),
                createKeyword("현대차", "ORG", 90.0)
        );

        when(keywordRepository.findByDateAndEntityType(testDate, entityType))
                .thenReturn(orgKeywords);

        // When
        List<Keyword> result = indexingService.getKeywordsByEntityType(testDate, entityType);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(k -> k.getEntityType().equals("ORG"));
        verify(keywordRepository, times(1)).findByDateAndEntityType(testDate, entityType);
    }

    @Test
    @DisplayName("키워드 존재 여부 확인")
    void existsKeyword() {
        // Given
        when(keywordRepository.existsByDateAndKeyword(testDate, "삼성전자"))
                .thenReturn(true);
        when(keywordRepository.existsByDateAndKeyword(testDate, "애플"))
                .thenReturn(false);

        // When & Then
        assertThat(indexingService.existsKeyword(testDate, "삼성전자")).isTrue();
        assertThat(indexingService.existsKeyword(testDate, "애플")).isFalse();
    }

    /**
     * Helper: NamedEntity 생성
     */
    private NamedEntity createEntity(String word, String entityType) {
        return NamedEntity.builder()
                .word(word)
                .entityType(entityType)
                .confidence(0.9f)
                .start(0)
                .end(word.length())
                .build();
    }

    /**
     * Helper: Keyword 생성
     */
    private Keyword createKeyword(String keyword, String entityType, double score) {
        return Keyword.builder()
                .keywordId(testDate + "_" + keyword)
                .keyword(keyword)
                .entityType(entityType)
                .score(score)
                .frequency(10)
                .date(testDate)
                .createdAt(LocalDateTime.now())
                .lastUpdated(LocalDateTime.now())
                .build();
    }
}