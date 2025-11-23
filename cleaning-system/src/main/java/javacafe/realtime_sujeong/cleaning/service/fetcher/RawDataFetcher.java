package javacafe.realtime_sujeong.cleaning.service.fetcher;

import javacafe.realtime_sujeong.cleaning.service.fetcher.dto.RawDataContent;

import java.util.List;

/**
 * 원본 데이터 조회 인터페이스 (Factory Pattern)
 * 소스별로 다른 구현체를 사용
 */
public interface RawDataFetcher {

    /**
     * 단일 데이터 조회
     *
     * @param dataId 데이터 고유 식별자
     * @return 원본 데이터 컨텐츠
     */
    RawDataContent fetchContent(String dataId);

    /**
     * 배치 데이터 조회 (성능 최적화)
     * MongoDB bulk query 사용
     *
     * @param dataIds 데이터 고유 식별자 리스트
     * @return 원본 데이터 컨텐츠 리스트
     */
    List<RawDataContent> fetchContentBatch(List<String> dataIds);

    /**
     * 지원하는 소스 타입 반환
     *
     * @return 소스 타입 (rss, wiki 등)
     */
    String getSupportedSource();
}