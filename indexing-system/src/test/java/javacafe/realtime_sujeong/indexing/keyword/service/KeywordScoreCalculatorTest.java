package javacafe.realtime_sujeong.indexing.keyword.service;

import javacafe.realtime_sujeong.indexing.keyword.domain.KeywordScore;
import javacafe.realtime_sujeong.indexing.ner.dto.NamedEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * KeywordScoreCalculator 단위 테스트
 */
@DisplayName("KeywordScoreCalculator 테스트")
class KeywordScoreCalculatorTest {

    private KeywordScoreCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new KeywordScoreCalculator();

        // 가중치 설정 (application.properties 값)
        ReflectionTestUtils.setField(calculator, "titleWeight", 2.0);
        ReflectionTestUtils.setField(calculator, "contentWeight", 1.0);
        ReflectionTestUtils.setField(calculator, "orgWeight", 1.5);
        ReflectionTestUtils.setField(calculator, "perWeight", 1.3);
        ReflectionTestUtils.setField(calculator, "locWeight", 1.0);
        ReflectionTestUtils.setField(calculator, "afWeight", 1.2);
        ReflectionTestUtils.setField(calculator, "defaultWeight", 1.0);
    }

    @Test
    @DisplayName("제목만 있을 때 점수 계산")
    void calculateScores_TitleOnly() {
        // Given
        List<NamedEntity> titleEntities = List.of(
                createEntity("삼성전자", "ORG"),
                createEntity("이재용", "PER")
        );
        List<NamedEntity> contentEntities = new ArrayList<>();

        // When
        List<KeywordScore> scores = calculator.calculateScores(
                titleEntities, contentEntities, "rss", "test-id"
        );

        // Then
        assertThat(scores).hasSize(2);

        KeywordScore samsungScore = findKeywordScore(scores, "삼성전자");
        assertThat(samsungScore).isNotNull();
        assertThat(samsungScore.getEntityType()).isEqualTo("ORG");
        assertThat(samsungScore.getTitleFrequency()).isEqualTo(1);
        assertThat(samsungScore.getContentFrequency()).isEqualTo(0);
        // Score = 1 × 2.0 × 1.5 = 3.0
        assertThat(samsungScore.getScore()).isEqualTo(3.0);

        KeywordScore leeScore = findKeywordScore(scores, "이재용");
        assertThat(leeScore).isNotNull();
        assertThat(leeScore.getEntityType()).isEqualTo("PER");
        // Score = 1 × 2.0 × 1.3 = 2.6
        assertThat(leeScore.getScore()).isEqualTo(2.6);
    }

    @Test
    @DisplayName("본문만 있을 때 점수 계산")
    void calculateScores_ContentOnly() {
        // Given
        List<NamedEntity> titleEntities = new ArrayList<>();
        List<NamedEntity> contentEntities = List.of(
                createEntity("서울", "LOC"),
                createEntity("갤럭시", "AF")
        );

        // When
        List<KeywordScore> scores = calculator.calculateScores(
                titleEntities, contentEntities, "rss", "test-id"
        );

        // Then
        assertThat(scores).hasSize(2);

        KeywordScore seoulScore = findKeywordScore(scores, "서울");
        assertThat(seoulScore).isNotNull();
        assertThat(seoulScore.getEntityType()).isEqualTo("LOC");
        assertThat(seoulScore.getTitleFrequency()).isEqualTo(0);
        assertThat(seoulScore.getContentFrequency()).isEqualTo(1);
        // Score = 1 × 1.0 × 1.0 = 1.0
        assertThat(seoulScore.getScore()).isEqualTo(1.0);

        KeywordScore galaxyScore = findKeywordScore(scores, "갤럭시");
        assertThat(galaxyScore).isNotNull();
        assertThat(galaxyScore.getEntityType()).isEqualTo("AF");
        // Score = 1 × 1.0 × 1.2 = 1.2
        assertThat(galaxyScore.getScore()).isEqualTo(1.2);
    }

    @Test
    @DisplayName("제목과 본문 모두 있을 때 점수 계산")
    void calculateScores_TitleAndContent() {
        // Given
        List<NamedEntity> titleEntities = List.of(
                createEntity("삼성전자", "ORG")
        );
        List<NamedEntity> contentEntities = List.of(
                createEntity("삼성전자", "ORG"),
                createEntity("삼성전자", "ORG"),
                createEntity("현대차", "ORG")
        );

        // When
        List<KeywordScore> scores = calculator.calculateScores(
                titleEntities, contentEntities, "rss", "test-id"
        );

        // Then
        assertThat(scores).hasSize(2);

        KeywordScore samsungScore = findKeywordScore(scores, "삼성전자");
        assertThat(samsungScore).isNotNull();
        assertThat(samsungScore.getTitleFrequency()).isEqualTo(1);
        assertThat(samsungScore.getContentFrequency()).isEqualTo(2);
        assertThat(samsungScore.getTotalFrequency()).isEqualTo(3);
        // Score = (1 × 2.0 × 1.5) + (2 × 1.0 × 1.5) = 3.0 + 3.0 = 6.0
        assertThat(samsungScore.getScore()).isEqualTo(6.0);

        KeywordScore hyundaiScore = findKeywordScore(scores, "현대차");
        assertThat(hyundaiScore).isNotNull();
        assertThat(hyundaiScore.getTitleFrequency()).isEqualTo(0);
        assertThat(hyundaiScore.getContentFrequency()).isEqualTo(1);
        // Score = 1 × 1.0 × 1.5 = 1.5
        assertThat(hyundaiScore.getScore()).isEqualTo(1.5);
    }

    @Test
    @DisplayName("개체 유형별 가중치 테스트")
    void calculateScores_EntityTypeWeights() {
        // Given
        List<NamedEntity> titleEntities = new ArrayList<>();
        List<NamedEntity> contentEntities = List.of(
                createEntity("삼성전자", "ORG"),   // 1.5
                createEntity("이재용", "PER"),     // 1.3
                createEntity("서울", "LOC"),       // 1.0
                createEntity("갤럭시", "AF"),      // 1.2
                createEntity("기타", "OTHER")      // 1.0 (default)
        );

        // When
        List<KeywordScore> scores = calculator.calculateScores(
                titleEntities, contentEntities, "rss", "test-id"
        );

        // Then
        assertThat(scores).hasSize(5);

        assertThat(findKeywordScore(scores, "삼성전자").getScore()).isEqualTo(1.5); // 1 × 1.0 × 1.5
        assertThat(findKeywordScore(scores, "이재용").getScore()).isEqualTo(1.3);   // 1 × 1.0 × 1.3
        assertThat(findKeywordScore(scores, "서울").getScore()).isEqualTo(1.0);     // 1 × 1.0 × 1.0
        assertThat(findKeywordScore(scores, "갤럭시").getScore()).isEqualTo(1.2);   // 1 × 1.0 × 1.2
        assertThat(findKeywordScore(scores, "기타").getScore()).isEqualTo(1.0);     // 1 × 1.0 × 1.0
    }

    @Test
    @DisplayName("동일 키워드가 여러 번 등장할 때 빈도수 집계")
    void calculateScores_MultipleOccurrences() {
        // Given
        List<NamedEntity> titleEntities = List.of(
                createEntity("삼성전자", "ORG"),
                createEntity("삼성전자", "ORG"),
                createEntity("삼성전자", "ORG")
        );
        List<NamedEntity> contentEntities = List.of(
                createEntity("삼성전자", "ORG"),
                createEntity("삼성전자", "ORG")
        );

        // When
        List<KeywordScore> scores = calculator.calculateScores(
                titleEntities, contentEntities, "rss", "test-id"
        );

        // Then
        assertThat(scores).hasSize(1);

        KeywordScore samsungScore = scores.get(0);
        assertThat(samsungScore.getKeyword()).isEqualTo("삼성전자");
        assertThat(samsungScore.getTitleFrequency()).isEqualTo(3);
        assertThat(samsungScore.getContentFrequency()).isEqualTo(2);
        assertThat(samsungScore.getTotalFrequency()).isEqualTo(5);
        // Score = (3 × 2.0 × 1.5) + (2 × 1.0 × 1.5) = 9.0 + 3.0 = 12.0
        assertThat(samsungScore.getScore()).isEqualTo(12.0);
    }

    @Test
    @DisplayName("빈 리스트로 호출 시 빈 결과 반환")
    void calculateScores_EmptyLists() {
        // Given
        List<NamedEntity> titleEntities = new ArrayList<>();
        List<NamedEntity> contentEntities = new ArrayList<>();

        // When
        List<KeywordScore> scores = calculator.calculateScores(
                titleEntities, contentEntities, "rss", "test-id"
        );

        // Then
        assertThat(scores).isEmpty();
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
     * Helper: KeywordScore 찾기
     */
    private KeywordScore findKeywordScore(List<KeywordScore> scores, String keyword) {
        return scores.stream()
                .filter(s -> s.getKeyword().equals(keyword))
                .findFirst()
                .orElse(null);
    }
}
