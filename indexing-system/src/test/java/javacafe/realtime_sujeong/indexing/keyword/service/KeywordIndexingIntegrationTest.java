package javacafe.realtime_sujeong.indexing.keyword.service;

import javacafe.realtime_sujeong.indexing.cleaning.domain.CleanedData;
import javacafe.realtime_sujeong.indexing.cleaning.domain.CleanedDataRepository;
import javacafe.realtime_sujeong.indexing.keyword.domain.Keyword;
import javacafe.realtime_sujeong.indexing.keyword.domain.KeywordRepository;
import javacafe.realtime_sujeong.indexing.ner.client.NerGrpcClient;
import javacafe.realtime_sujeong.indexing.ner.dto.NamedEntity;
import javacafe.realtime_sujeong.indexing.ner.dto.NerResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.when;

/**
 * Keyword Indexing Integration Test
 * <p>
 * Elasticsearch와 MongoDB를 사용한 전체 색인 플로우 통합 테스트
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class KeywordIndexingIntegrationTest {

    @Container
    static ElasticsearchContainer elasticsearchContainer = new ElasticsearchContainer(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.11.0")
                    .asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch")
    ).withEnv("xpack.security.enabled", "false")
            .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.uris", elasticsearchContainer::getHttpHostAddress);
    }

    @Autowired
    private KeywordIndexingService keywordIndexingService;

    @Autowired
    private KeywordRepository keywordRepository;

    @Autowired
    private CleanedDataRepository cleanedDataRepository;

    @MockitoBean
    private NerGrpcClient nerGrpcClient;

    @BeforeEach
    void setUp() {
        // Clear data before each test
        keywordRepository.deleteAll();
        cleanedDataRepository.deleteAll();
    }

    @Test
    @DisplayName("통합 테스트: RSS 데이터 색인 전체 플로우")
    void indexRssData_FullFlow() {
        // Given: MongoDB에 정제된 데이터 저장
        String dataId = "https://example.com/news/123";
        CleanedData cleanedData = CleanedData.builder()
                .dataId(dataId)
                .source("rss")
                .cleanedTitle("삼성전자 신제품 출시")
                .cleanedContent("삼성전자가 새로운 스마트폰을 출시했다. 삼성전자는 이번 제품에서 AI 기능을 강화했다.")
                .language("ko")
                .cleanedAt(LocalDateTime.now())
                .build();
        cleanedDataRepository.save(cleanedData);

        // NER Mock 설정
        when(nerGrpcClient.extractEntities(any(String.class), anyFloat()))
                .thenAnswer(invocation -> {
                    String text = invocation.getArgument(0);
                    if (text.contains("삼성전자")) {
                        return NerResult.builder()
                                .entities(List.of(
                                        NamedEntity.builder()
                                                .word("삼성전자")
                                                .entityType("ORG")
                                                .confidence(0.95f)
                                                .start(0)
                                                .end(4)
                                                .build()
                                ))
                                .build();
                    }
                    return NerResult.builder().entities(List.of()).build();
                });

        // When: 색인 수행
        keywordIndexingService.indexKeywords(
                cleanedData.getCleanedTitle(),
                cleanedData.getCleanedContent(),
                cleanedData.getSource(),
                dataId,
                LocalDate.now()
        );

        // Then: Elasticsearch에 키워드가 저장되었는지 확인
        LocalDate today = LocalDate.now();
        String keywordId = Keyword.generateKeywordId(today, "삼성전자");
        Optional<Keyword> savedKeyword = keywordRepository.findByKeywordId(keywordId);

        assertThat(savedKeyword).isPresent();
        assertThat(savedKeyword.get().getKeyword()).isEqualTo("삼성전자");
        assertThat(savedKeyword.get().getEntityType()).isEqualTo("ORG");
        assertThat(savedKeyword.get().getDate()).isEqualTo(today);
        assertThat(savedKeyword.get().getScore()).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("통합 테스트: 동일 키워드 중복 색인 시 점수 누적")
    void indexRssData_SameKeyword_AccumulatesScore() {
        // Given: 같은 키워드를 포함한 두 개의 문서
        String dataId1 = "https://example.com/news/1";
        String dataId2 = "https://example.com/news/2";

        CleanedData data1 = CleanedData.builder()
                .dataId(dataId1)
                .source("rss")
                .cleanedTitle("삼성전자 실적 발표")
                .cleanedContent("삼성전자가 분기 실적을 발표했다.")
                .language("ko")
                .cleanedAt(LocalDateTime.now())
                .build();

        CleanedData data2 = CleanedData.builder()
                .dataId(dataId2)
                .source("rss")
                .cleanedTitle("삼성전자 주가 상승")
                .cleanedContent("삼성전자 주가가 상승했다.")
                .language("ko")
                .cleanedAt(LocalDateTime.now())
                .build();

        cleanedDataRepository.save(data1);
        cleanedDataRepository.save(data2);

        // NER Mock 설정
        when(nerGrpcClient.extractEntities(any(String.class), anyFloat()))
                .thenReturn(NerResult.builder()
                        .entities(List.of(
                                NamedEntity.builder()
                                        .word("삼성전자")
                                        .entityType("ORG")
                                        .confidence(0.95f)
                                        .start(0)
                                        .end(4)
                                        .build()
                        ))
                        .build());

        // When: 두 문서를 순차적으로 색인
        keywordIndexingService.indexKeywords(
                data1.getCleanedTitle(), data1.getCleanedContent(), data1.getSource(), dataId1, LocalDate.now()
        );
        keywordIndexingService.indexKeywords(
                data2.getCleanedTitle(), data2.getCleanedContent(), data2.getSource(), dataId2, LocalDate.now()
        );

        // Then: 점수가 누적되었는지 확인
        LocalDate today = LocalDate.now();
        String keywordId = Keyword.generateKeywordId(today, "삼성전자");
        Optional<Keyword> savedKeyword = keywordRepository.findByKeywordId(keywordId);

        assertThat(savedKeyword).isPresent();
        Keyword keyword = savedKeyword.get();
        assertThat(keyword.getFrequency()).isEqualTo(2); // 두 번 등장
        assertThat(keyword.getScore()).isGreaterThan(3.0); // 점수 누적 확인
    }

    @Test
    @DisplayName("통합 테스트: Top 10 키워드 조회")
    void getTopKeywords_ReturnsTop10() {
        // Given: 여러 키워드가 색인된 상태
        LocalDate today = LocalDate.now();

        // 직접 Elasticsearch에 키워드 저장 (테스트용)
        for (int i = 1; i <= 15; i++) {
            Keyword keyword = Keyword.builder()
                    .keywordId(Keyword.generateKeywordId(today, "키워드" + i))
                    .keyword("키워드" + i)
                    .entityType("ORG")
                    .date(today)
                    .score((double) (100 - i * 5)) // 점수: 95, 90, 85, ...
                    .frequency(i)
                    .build();
            keywordRepository.save(keyword);
        }

        // When: Top 10 조회
        List<Keyword> topKeywords = keywordIndexingService.getTopKeywords(today, 10);

        // Then: 상위 10개만 반환되고 점수 내림차순으로 정렬되었는지 확인
        assertThat(topKeywords).hasSize(10);
        assertThat(topKeywords.get(0).getKeyword()).isEqualTo("키워드1");
        assertThat(topKeywords.get(0).getScore()).isEqualTo(95.0);
        assertThat(topKeywords.get(9).getKeyword()).isEqualTo("키워드10");
        assertThat(topKeywords.get(9).getScore()).isEqualTo(50.0);

        // 점수가 내림차순인지 확인
        for (int i = 0; i < topKeywords.size() - 1; i++) {
            assertThat(topKeywords.get(i).getScore())
                    .isGreaterThanOrEqualTo(topKeywords.get(i + 1).getScore());
        }
    }

    @Test
    @DisplayName("통합 테스트: 여러 엔티티 타입 혼합 색인")
    void indexRssData_MultipleEntityTypes() {
        // Given: 여러 엔티티 타입을 포함한 데이터
        String dataId = "https://example.com/news/456";
        CleanedData cleanedData = CleanedData.builder()
                .dataId(dataId)
                .source("rss")
                .cleanedTitle("이재용 부회장 삼성전자 방문")
                .cleanedContent("이재용 삼성전자 부회장이 서울 본사를 방문했다.")
                .language("ko")
                .cleanedAt(LocalDateTime.now())
                .build();
        cleanedDataRepository.save(cleanedData);

        // NER Mock 설정 (PER, ORG, LOC 혼합)
        when(nerGrpcClient.extractEntities(any(String.class), anyFloat()))
                .thenAnswer(invocation -> {
                    String text = invocation.getArgument(0);
                    if (text.contains("이재용")) {
                        return NerResult.builder()
                                .entities(List.of(
                                        NamedEntity.builder()
                                                .word("이재용")
                                                .entityType("PER")
                                                .confidence(0.95f)
                                                .build(),
                                        NamedEntity.builder()
                                                .word("삼성전자")
                                                .entityType("ORG")
                                                .confidence(0.95f)
                                                .build(),
                                        NamedEntity.builder()
                                                .word("서울")
                                                .entityType("LOC")
                                                .confidence(0.90f)
                                                .build()
                                ))
                                .build();
                    }
                    return NerResult.builder().entities(List.of()).build();
                });

        // When: 색인 수행
        keywordIndexingService.indexKeywords(
                cleanedData.getCleanedTitle(),
                cleanedData.getCleanedContent(),
                cleanedData.getSource(),
                dataId,
                LocalDate.now()
        );

        // Then: 모든 엔티티가 색인되었는지 확인
        LocalDate today = LocalDate.now();

        Optional<Keyword> personKeyword = keywordRepository.findByKeywordId(
                Keyword.generateKeywordId(today, "이재용")
        );
        Optional<Keyword> orgKeyword = keywordRepository.findByKeywordId(
                Keyword.generateKeywordId(today, "삼성전자")
        );
        Optional<Keyword> locKeyword = keywordRepository.findByKeywordId(
                Keyword.generateKeywordId(today, "서울")
        );

        assertThat(personKeyword).isPresent();
        assertThat(personKeyword.get().getEntityType()).isEqualTo("PER");

        assertThat(orgKeyword).isPresent();
        assertThat(orgKeyword.get().getEntityType()).isEqualTo("ORG");

        assertThat(locKeyword).isPresent();
        assertThat(locKeyword.get().getEntityType()).isEqualTo("LOC");

        // 엔티티 타입별 가중치 적용 확인 (ORG > PER > LOC)
        assertThat(orgKeyword.get().getScore()).isGreaterThan(personKeyword.get().getScore());
        assertThat(personKeyword.get().getScore()).isGreaterThan(locKeyword.get().getScore());
    }
}
