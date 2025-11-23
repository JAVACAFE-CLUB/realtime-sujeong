package javacafe.realtime_sujeong.cleaning.service.fetcher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * RawDataFetcher Factory
 * 소스 타입에 따라 적절한 Fetcher를 반환
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RawDataFetcherFactory {

    private final List<RawDataFetcher> fetchers;
    private Map<String, RawDataFetcher> fetcherMap;

    /**
     * 초기화: fetcher 리스트를 Map으로 변환
     */
    private void initializeFetcherMap() {
        if (fetcherMap == null) {
            fetcherMap = fetchers.stream()
                    .collect(Collectors.toMap(
                            RawDataFetcher::getSupportedSource,
                            Function.identity()
                    ));
            log.info("Initialized RawDataFetcherFactory with {} fetchers: {}",
                    fetcherMap.size(), fetcherMap.keySet());
        }
    }

    /**
     * 소스 타입에 맞는 Fetcher 반환
     *
     * @param source 소스 타입 (rss, wiki 등)
     * @return 해당 소스의 Fetcher
     * @throws IllegalArgumentException 지원하지 않는 소스인 경우
     */
    public RawDataFetcher getFetcher(String source) {
        initializeFetcherMap();

        RawDataFetcher fetcher = fetcherMap.get(source.toLowerCase());

        if (fetcher == null) {
            throw new IllegalArgumentException(
                    "Unsupported source type: " + source +
                    ". Supported sources: " + fetcherMap.keySet());
        }

        log.debug("Selected fetcher for source '{}': {}", source, fetcher.getClass().getSimpleName());
        return fetcher;
    }

    /**
     * 지원하는 소스 타입 목록 반환
     *
     * @return 지원 소스 타입 Set
     */
    public java.util.Set<String> getSupportedSources() {
        initializeFetcherMap();
        return fetcherMap.keySet();
    }
}