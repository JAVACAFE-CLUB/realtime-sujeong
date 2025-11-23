package javacafe.realtime_sujeong.cleaning.domain.view;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * WikiRawDataView Repository (read-only)
 * cleaning-system에서 Wiki 원본 데이터 조회 전용
 */
public interface WikiRawDataViewRepository extends MongoRepository<WikiRawDataView, String> {

    /**
     * dataId로 단일 데이터 조회
     *
     * @param dataId 데이터 고유 식별자
     * @return WikiRawDataView (Optional)
     */
    Optional<WikiRawDataView> findByDataId(String dataId);

    /**
     * dataId 리스트로 배치 데이터 조회 (MongoDB bulk query)
     *
     * @param dataIds 데이터 고유 식별자 리스트
     * @return WikiRawDataView 리스트
     */
    List<WikiRawDataView> findAllByDataIdIn(List<String> dataIds);
}