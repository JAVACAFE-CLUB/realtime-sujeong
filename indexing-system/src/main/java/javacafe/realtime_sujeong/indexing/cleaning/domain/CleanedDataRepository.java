package javacafe.realtime_sujeong.indexing.cleaning.domain;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * CleanedData Repository
 *
 * MongoDB cleaned_data 컬렉션에 대한 Repository
 */
@Repository
public interface CleanedDataRepository extends MongoRepository<CleanedData, String> {

    /**
     * dataId로 조회
     *
     * @param dataId 데이터 ID
     * @return Optional<CleanedData>
     */
    Optional<CleanedData> findByDataId(String dataId);

    /**
     * dataId 존재 여부 확인
     *
     * @param dataId 데이터 ID
     * @return 존재 여부
     */
    boolean existsByDataId(String dataId);
}