package javacafe.realtime_sujeong.collection.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DataIdGenerator 테스트")
class DataIdGeneratorTest {

    @Test
    @DisplayName("RSS 데이터 ID 생성 - URL 그대로 반환")
    void generateRssDataId_ReturnsUrlDirectly() {
        // given
        String link = "https://www.example.com/news/12345";

        // when
        String dataId = DataIdGenerator.generateRssDataId(link);

        // then
        assertThat(dataId).isEqualTo(link);
    }

    @Test
    @DisplayName("RSS 데이터 ID 생성 - 동일 URL은 동일 ID 생성")
    void generateRssDataId_SameUrlSameOutput() {
        // given
        String link = "https://www.example.com/news/12345";

        // when
        String dataId1 = DataIdGenerator.generateRssDataId(link);
        String dataId2 = DataIdGenerator.generateRssDataId(link);

        // then
        assertThat(dataId1).isEqualTo(dataId2);
    }

    @Test
    @DisplayName("RSS 데이터 ID 생성 - 다른 URL은 다른 ID 생성")
    void generateRssDataId_DifferentUrlDifferentOutput() {
        // given
        String link1 = "https://www.example.com/news/12345";
        String link2 = "https://www.example.com/news/67890";

        // when
        String dataId1 = DataIdGenerator.generateRssDataId(link1);
        String dataId2 = DataIdGenerator.generateRssDataId(link2);

        // then
        assertThat(dataId1).isNotEqualTo(dataId2);
    }

    @Test
    @DisplayName("Wiki 데이터 ID 생성 - pageId 그대로 반환")
    void generateWikiDataId_ReturnsPageIdDirectly() {
        // given
        String pageId = "12345";

        // when
        String dataId = DataIdGenerator.generateWikiDataId(pageId);

        // then
        assertThat(dataId).isEqualTo(pageId);
    }

    @Test
    @DisplayName("Wiki 데이터 ID 생성 - 동일 pageId는 동일 ID 생성")
    void generateWikiDataId_SamePageIdSameOutput() {
        // given
        String pageId = "12345";

        // when
        String dataId1 = DataIdGenerator.generateWikiDataId(pageId);
        String dataId2 = DataIdGenerator.generateWikiDataId(pageId);

        // then
        assertThat(dataId1).isEqualTo(dataId2);
    }

    @Test
    @DisplayName("Wiki 데이터 ID 생성 - 다른 pageId는 다른 ID 생성")
    void generateWikiDataId_DifferentPageIdDifferentOutput() {
        // given
        String pageId1 = "12345";
        String pageId2 = "67890";

        // when
        String dataId1 = DataIdGenerator.generateWikiDataId(pageId1);
        String dataId2 = DataIdGenerator.generateWikiDataId(pageId2);

        // then
        assertThat(dataId1).isNotEqualTo(dataId2);
    }
}
