package javacafe.realtime_sujeong.cleaning.domain.view;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * RssRawDataView Repository (read-only)
 * cleaning-system에서 RSS 원본 데이터 조회 전용
 */
public interface RssRawDataViewRepository extends MongoRepository<RssRawDataView, String> {

    /**
     * dataId로 단일 데이터 조회
     *
     * @param dataId 데이터 고유 식별자
     * @return RssRawDataView (Optional)
     */
    Optional<RssRawDataView> findByDataId(String dataId);

    /**
     * dataId 리스트로 배치 데이터 조회 (MongoDB bulk query)
     *
     * @param dataIds 데이터 고유 식별자 리스트
     * @return RssRawDataView 리스트
     */
    List<RssRawDataView> findAllByDataIdIn(List<String> dataIds);
}