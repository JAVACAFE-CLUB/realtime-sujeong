package javacafe.realtime_sujeong.collection.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DataIdGenerator 테스트")
class DataIdGeneratorTest {

    @Test
    @DisplayName("RSS 데이터 ID 생성 - 정상 케이스")
    void generateRssDataId_Success() {
        // given
        String link = "https://www.example.com/news/12345";
        String pubDate = "2025-10-25T10:30:00";

        // when
        String dataId = DataIdGenerator.generateRssDataId(link, LocalDateTime.parse(pubDate));

        // then
        assertThat(dataId).isNotNull();
        assertThat(dataId).hasSize(64); // SHA-256은 64자리 16진수
        assertThat(dataId).matches("^[a-f0-9]{64}$"); // 소문자 16진수
    }

    @Test
    @DisplayName("RSS 데이터 ID 생성 - 동일 입력시 동일 ID 생성")
    void generateRssDataId_SameInputSameOutput() {
        // given
        String link = "https://www.example.com/news/12345";
        String pubDate = "2025-10-25T10:30:00";

        // when
        String dataId1 = DataIdGenerator.generateRssDataId(link, LocalDateTime.parse(pubDate));
        String dataId2 = DataIdGenerator.generateRssDataId(link, LocalDateTime.parse(pubDate));

        // then
        assertThat(dataId1).isEqualTo(dataId2);
    }

    @Test
    @DisplayName("RSS 데이터 ID 생성 - 다른 입력시 다른 ID 생성")
    void generateRssDataId_DifferentInputDifferentOutput() {
        // given
        String link1 = "https://www.example.com/news/12345";
        String link2 = "https://www.example.com/news/67890";
        String pubDate = "2025-10-25T10:30:00";

        // when
        String dataId1 = DataIdGenerator.generateRssDataId(link1, LocalDateTime.parse(pubDate));
        String dataId2 = DataIdGenerator.generateRssDataId(link2, LocalDateTime.parse(pubDate));

        // then
        assertThat(dataId1).isNotEqualTo(dataId2);
    }

    @Test
    @DisplayName("RSS 데이터 ID 생성 - link가 null이면 예외 발생")
    void generateRssDataId_NullLink_ThrowsException() {
        // given
        String link = null;
        String pubDate = "2025-10-25T10:30:00";

        // when & then
        assertThatThrownBy(() -> DataIdGenerator.generateRssDataId(link, LocalDateTime.parse(pubDate)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null일 수 없습니다");
    }

    @Test
    @DisplayName("RSS 데이터 ID 생성 - pubDate가 null이면 예외 발생")
    void generateRssDataId_NullPubDate_ThrowsException() {
        // given
        String link = "https://www.example.com/news/12345";
        String pubDate = null;

        // when & then
        assertThatThrownBy(() -> DataIdGenerator.generateRssDataId(link, LocalDateTime.parse(pubDate)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null일 수 없습니다");
    }

    @Test
    @DisplayName("Wiki 데이터 ID 생성 - 정상 케이스")
    void generateWikiDataId_Success() {
        // given
        String title = "대한민국";
        String timestamp = "2025-10-25T10:30:00";

        // when
        String dataId = DataIdGenerator.generateWikiDataId(title, timestamp);

        // then
        assertThat(dataId).isNotNull();
        assertThat(dataId).hasSize(64);
        assertThat(dataId).matches("^[a-f0-9]{64}$");
    }

    @Test
    @DisplayName("Wiki 데이터 ID 생성 - 동일 입력시 동일 ID 생성")
    void generateWikiDataId_SameInputSameOutput() {
        // given
        String title = "대한민국";
        String timestamp = "2025-10-25T10:30:00";

        // when
        String dataId1 = DataIdGenerator.generateWikiDataId(title, timestamp);
        String dataId2 = DataIdGenerator.generateWikiDataId(title, timestamp);

        // then
        assertThat(dataId1).isEqualTo(dataId2);
    }

    @Test
    @DisplayName("Wiki 데이터 ID 생성 - title이 null이면 예외 발생")
    void generateWikiDataId_NullTitle_ThrowsException() {
        // given
        String title = null;
        String timestamp = "2025-10-25T10:30:00";

        // when & then
        assertThatThrownBy(() -> DataIdGenerator.generateWikiDataId(title, timestamp))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null일 수 없습니다");
    }

    @Test
    @DisplayName("Wiki 데이터 ID 생성 - timestamp가 null이면 예외 발생")
    void generateWikiDataId_NullTimestamp_ThrowsException() {
        // given
        String title = "대한민국";
        String timestamp = null;

        // when & then
        assertThatThrownBy(() -> DataIdGenerator.generateWikiDataId(title, timestamp))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null일 수 없습니다");
    }
}