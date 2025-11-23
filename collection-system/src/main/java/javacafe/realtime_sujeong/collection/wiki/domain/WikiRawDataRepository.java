package javacafe.realtime_sujeong.collection.wiki.domain;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Wiki 원본 데이터 Repository
 */
@Repository
public interface WikiRawDataRepository extends MongoRepository<WikiRawData, String> {

    /**
     * dataId로 데이터 조회
     */
    Optional<WikiRawData> findByDataId(String dataId);

    /**
     * dataId 존재 여부 확인
     */
    boolean existsByDataId(String dataId);

    /**
     * 여러 dataId를 한 번에 조회하여 존재하는 dataId 리스트 반환
     * 배치 처리 시 성능 최적화용
     */
    @Query(value = "{ 'dataId': { $in: ?0 } }", fields = "{ 'dataId': 1 }")
    List<WikiRawData> findDataIdsByDataIdIn(List<String> dataIds);

    /**
     * 여러 dataId를 한 번에 조회하여 전체 데이터 반환
     * upsert 처리 시 timestamp 비교를 위해 사용
     */
    List<WikiRawData> findAllByDataIdIn(List<String> dataIds);

    /**
     * 특정 네임스페이스의 데이터 조회
     */
    List<WikiRawData> findByNamespace(String namespace);
}
