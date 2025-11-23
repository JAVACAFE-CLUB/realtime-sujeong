package javacafe.realtime_sujeong.cleaning.service.fetcher;

import javacafe.realtime_sujeong.cleaning.domain.view.RssRawDataView;
import javacafe.realtime_sujeong.cleaning.domain.view.RssRawDataViewRepository;
import javacafe.realtime_sujeong.cleaning.service.fetcher.dto.RawDataContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * RSS 원본 데이터 Fetcher
 * rss_raw_data 컬렉션에서 데이터 조회
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RssRawDataFetcher implements RawDataFetcher {

    private final RssRawDataViewRepository rssRawDataViewRepository;

    @Override
    public RawDataContent fetchContent(String dataId) {
        log.debug("Fetching RSS raw data for dataId: {}", dataId);

        RssRawDataView rawData = rssRawDataViewRepository.findByDataId(dataId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "RSS raw data not found for dataId: " + dataId));

        return RawDataContent.builder()
                .dataId(rawData.getDataId())
                .title(rawData.getTitle())
                .content(rawData.getContent())  // 이미 Collection System에서 크롤링됨
                .url(rawData.getLink())
                .source("rss")
                .build();
    }

    @Override
    public List<RawDataContent> fetchContentBatch(List<String> dataIds) {
        log.debug("Fetching RSS raw data batch for {} dataIds", dataIds.size());

        // MongoDB bulk query 사용
        List<RssRawDataView> rawDataList = rssRawDataViewRepository.findAllByDataIdIn(dataIds);

        return rawDataList.stream()
                .map(raw -> RawDataContent.builder()
                        .dataId(raw.getDataId())
                        .title(raw.getTitle())
                        .content(raw.getContent())
                        .url(raw.getLink())
                        .source("rss")
                        .build())
                .toList();
    }

    @Override
    public String getSupportedSource() {
        return "rss";
    }
}