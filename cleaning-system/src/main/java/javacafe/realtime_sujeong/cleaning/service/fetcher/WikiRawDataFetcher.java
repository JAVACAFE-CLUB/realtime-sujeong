package javacafe.realtime_sujeong.cleaning.service.fetcher;

import javacafe.realtime_sujeong.cleaning.domain.view.WikiRawDataView;
import javacafe.realtime_sujeong.cleaning.domain.view.WikiRawDataViewRepository;
import javacafe.realtime_sujeong.cleaning.service.fetcher.dto.RawDataContent;
import javacafe.realtime_sujeong.cleaning.service.processor.WikiTextExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Wiki 원본 데이터 Fetcher
 * wiki_raw_data 컬렉션에서 데이터 조회 및 wikitext 변환
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WikiRawDataFetcher implements RawDataFetcher {

    private final WikiRawDataViewRepository wikiRawDataViewRepository;
    private final WikiTextExtractor wikiTextExtractor;

    @Override
    public RawDataContent fetchContent(String dataId) {
        log.debug("Fetching Wiki raw data for dataId: {}", dataId);

        WikiRawDataView rawData = wikiRawDataViewRepository.findByDataId(dataId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Wiki raw data not found for dataId: " + dataId));

        // wikitext 추출
        String wikitext = rawData.getWikiPage().getRevision().getText().getContent();

        // Tika로 wikitext를 plain text로 변환
        String plainText = wikiTextExtractor.extract(wikitext);

        return RawDataContent.builder()
                .dataId(rawData.getDataId())
                .title(rawData.getTitle())
                .content(plainText)
                .url(null)  // Wiki는 URL 없음
                .source("wiki")
                .build();
    }

    @Override
    public List<RawDataContent> fetchContentBatch(List<String> dataIds) {
        log.debug("Fetching Wiki raw data batch for {} dataIds", dataIds.size());

        // MongoDB bulk query 사용
        List<WikiRawDataView> rawDataList = wikiRawDataViewRepository.findAllByDataIdIn(dataIds);

        return rawDataList.stream()
                .map(raw -> {
                    String wikitext = raw.getWikiPage().getRevision().getText().getContent();

                    // Tika로 wikitext를 plain text로 변환
                    String plainText = wikiTextExtractor.extract(wikitext);

                    return RawDataContent.builder()
                            .dataId(raw.getDataId())
                            .title(raw.getTitle())
                            .content(plainText)
                            .url(null)
                            .source("wiki")
                            .build();
                })
                .toList();
    }

    @Override
    public String getSupportedSource() {
        return "wiki";
    }
}
