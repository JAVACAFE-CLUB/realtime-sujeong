package javacafe.realtime_sujeong.collection.rss.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * RSS 원본 데이터 Repository
 */
public interface RssRawDataRepository extends MongoRepository<RssRawData, String> {

    /**
     * dataId로 조회
     * 중복 체크에 사용
     */
    Optional<RssRawData> findByDataId(String dataId);

    /**
     * dataId 존재 여부 확인
     */
    boolean existsByDataId(String dataId);

    /**
     * 소스별 조회
     */
    List<RssRawData> findBySource(String source);

    /**
     * 소스별 조회 (페이징)
     */
    Page<RssRawData> findBySource(String source, Pageable pageable);

    /**
     * 소스별 개수 조회
     */
    long countBySource(String source);

    /**
     * 상태별 조회
     */
    List<RssRawData> findByStatus(RssRawData.DataStatus status);

    /**
     * 소스와 상태로 조회
     */
    List<RssRawData> findBySourceAndStatus(String source, RssRawData.DataStatus status);

    /**
     * 특정 기간 이후 수집된 데이터 조회
     */
    List<RssRawData> findByCollectedAtAfter(LocalDateTime collectedAt);

    /**
     * 소스별, 특정 기간 이후 수집된 데이터 조회
     */
    List<RssRawData> findBySourceAndCollectedAtAfter(String source, LocalDateTime collectedAt);
}
