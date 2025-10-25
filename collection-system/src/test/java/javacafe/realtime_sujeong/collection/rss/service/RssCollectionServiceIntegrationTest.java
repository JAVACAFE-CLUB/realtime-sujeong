package javacafe.realtime_sujeong.collection.rss.service;

import javacafe.realtime_sujeong.collection.rss.domain.RssRawData;
import javacafe.realtime_sujeong.collection.rss.domain.RssRawDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.batch.job.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})
@Testcontainers
@DisplayName("RssCollectionService 통합 테스트 (MongoDB)")
class RssCollectionServiceIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private RssCollectionService rssCollectionService;

    @Autowired
    private RssRawDataRepository rssRawDataRepository;

    @BeforeEach
    void setUp() {
        rssRawDataRepository.deleteAll();
    }

    @Test
    @DisplayName("실제 RSS 피드 수집 및 MongoDB 저장 - 조선일보")
    void collectFeed_RealChosunRss_SaveToMongoDB() {
        // given
        String feedUrl = "https://www.chosun.com/arc/outboundfeeds/rss/?outputType=xml";
        String source = "chosun";

        // when
        RssCollectionService.CollectionResult result = rssCollectionService.collectFeed(feedUrl, source);

        // then
        assertThat(result.totalCount()).isGreaterThan(0);
        assertThat(result.savedCount()).isEqualTo(result.totalCount());
        assertThat(result.duplicateCount()).isEqualTo(0);

        // MongoDB 확인
        List<RssRawData> savedData = rssRawDataRepository.findBySource(source);
        assertThat(savedData).hasSize(result.savedCount());

        RssRawData firstData = savedData.get(0);
        assertThat(firstData.getDataId()).isNotBlank();
        assertThat(firstData.getSource()).isEqualTo(source);
        assertThat(firstData.getTitle()).isNotBlank();
        assertThat(firstData.getLink()).startsWith("http");
        assertThat(firstData.getPubDate()).isNotNull();
        assertThat(firstData.getCollectedAt()).isNotNull();
        assertThat(firstData.getStatus()).isEqualTo(RssRawData.DataStatus.COLLECTED);
    }

    @Test
    @DisplayName("중복 수집 방지 - 같은 피드 두 번 수집")
    void collectFeed_TwiceSameFeed_PreventDuplicates() {
        // given
        String feedUrl = "https://www.chosun.com/arc/outboundfeeds/rss/?outputType=xml";
        String source = "chosun";

        // when - 첫 번째 수집
        RssCollectionService.CollectionResult firstResult = rssCollectionService.collectFeed(feedUrl, source);

        // when - 두 번째 수집 (같은 피드)
        RssCollectionService.CollectionResult secondResult = rssCollectionService.collectFeed(feedUrl, source);

        // then
        assertThat(firstResult.savedCount()).isGreaterThan(0);

        // 두 번째 수집은 대부분 중복이어야 함 (일부는 첫 수집 시 실패했다가 성공할 수 있음)
        assertThat(secondResult.duplicateCount()).isGreaterThanOrEqualTo(firstResult.savedCount() - 5);

        // 두 번째 수집에서 새로 저장된 건수는 매우 적어야 함 (실패 재시도 케이스)
        assertThat(secondResult.savedCount()).isLessThanOrEqualTo(5);

        // MongoDB에 저장된 총 데이터는 첫 번째 수집 이상이어야 함
        List<RssRawData> savedData = rssRawDataRepository.findBySource(source);
        assertThat(savedData.size()).isGreaterThanOrEqualTo(firstResult.savedCount());
        assertThat(savedData.size()).isLessThanOrEqualTo(firstResult.savedCount() + secondResult.savedCount());
    }

    @Test
    @DisplayName("실제 RSS 피드 수집 및 MongoDB 저장 - 매일경제")
    void collectFeed_RealMaeilRss_SaveToMongoDB() {
        // given
        String feedUrl = "https://www.mk.co.kr/rss/30000001/";
        String source = "maeil";

        // when
        RssCollectionService.CollectionResult result = rssCollectionService.collectFeed(feedUrl, source);

        // then
        assertThat(result.totalCount()).isGreaterThan(0);
        assertThat(result.savedCount()).isEqualTo(result.totalCount());

        // MongoDB 확인
        List<RssRawData> savedData = rssRawDataRepository.findBySource(source);
        assertThat(savedData).hasSize(result.savedCount());
    }

    @Test
    @DisplayName("Repository 메서드 테스트 - dataId로 조회")
    void repository_FindByDataId() {
        // given
        String feedUrl = "https://www.chosun.com/arc/outboundfeeds/rss/?outputType=xml";
        String source = "chosun";

        rssCollectionService.collectFeed(feedUrl, source);

        List<RssRawData> allData = rssRawDataRepository.findAll();
        assertThat(allData).isNotEmpty();

        String dataId = allData.get(0).getDataId();

        // when
        boolean exists = rssRawDataRepository.existsByDataId(dataId);
        var foundData = rssRawDataRepository.findByDataId(dataId);

        // then
        assertThat(exists).isTrue();
        assertThat(foundData).isPresent();
        assertThat(foundData.get().getDataId()).isEqualTo(dataId);
    }
}