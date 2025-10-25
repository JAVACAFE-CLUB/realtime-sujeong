package javacafe.realtime_sujeong.collection.rss.collector.crawler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 기사 본문 크롤링 전략 Factory
 * 소스별로 적절한 크롤링 전략을 선택
 */
@Slf4j
@Component
public class ArticleCrawlingStrategyFactory {

    private final Map<String, ArticleCrawlingStrategy> strategyMap;

    /**
     * 생성자 주입으로 모든 전략을 받아서 Map으로 관리
     */
    public ArticleCrawlingStrategyFactory(List<ArticleCrawlingStrategy> strategies) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(
                        ArticleCrawlingStrategy::getSupportedSource,
                        Function.identity()
                ));

        log.info("등록된 기사 크롤링 전략: {}", strategyMap.keySet());
    }

    /**
     * 소스에 맞는 크롤링 전략 반환
     *
     * @param source 데이터 소스 (언론사)
     * @return 해당 소스에 맞는 크롤링 전략
     * @throws IllegalArgumentException 지원하지 않는 소스인 경우
     */
    public ArticleCrawlingStrategy getStrategy(String source) {
        ArticleCrawlingStrategy strategy = strategyMap.get(source);

        if (strategy == null) {
            throw new IllegalArgumentException(
                    String.format("지원하지 않는 소스입니다: %s (지원 소스: %s)",
                            source, strategyMap.keySet())
            );
        }

        return strategy;
    }

    /**
     * 지원하는 모든 소스 목록 반환
     */
    public List<String> getSupportedSources() {
        return List.copyOf(strategyMap.keySet());
    }

    /**
     * 해당 소스 지원 여부 확인
     */
    public boolean isSupported(String source) {
        return strategyMap.containsKey(source);
    }
}