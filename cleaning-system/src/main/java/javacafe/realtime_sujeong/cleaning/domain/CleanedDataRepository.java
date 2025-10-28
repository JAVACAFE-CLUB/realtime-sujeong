package javacafe.realtime_sujeong.cleaning.domain;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * CleanedData Repository
 * cleaned_data 컬렉션 접근
 */
@Repository
public interface CleanedDataRepository extends MongoRepository<CleanedData, String> {

    /**
     * dataId로 정제 데이터 조회
     *
     * @param dataId 데이터 고유 식별자
     * @return 정제 데이터 (Optional)
     */
    Optional<CleanedData> findByDataId(String dataId);

    /**
     * dataId 존재 여부 확인
     *
     * @param dataId 데이터 고유 식별자
     * @return 존재 여부
     */
    boolean existsByDataId(String dataId);

    /**
     * source로 정제 데이터 목록 조회
     *
     * @param source 데이터 소스 (rss, wiki 등)
     * @return 정제 데이터 목록
     */
    List<CleanedData> findBySource(String source);

    /**
     * 특정 기간 동안 처리된 데이터 조회
     *
     * @param startDate 시작 시각
     * @param endDate 종료 시각
     * @return 정제 데이터 목록
     */
    List<CleanedData> findByProcessedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 언어별 데이터 조회
     *
     * @param language 언어 코드 (ko, en, ja 등)
     * @return 정제 데이터 목록
     */
    List<CleanedData> findByLanguage(String language);

    /**
     * source와 언어로 데이터 조회
     *
     * @param source 데이터 소스
     * @param language 언어 코드
     * @return 정제 데이터 목록
     */
    List<CleanedData> findBySourceAndLanguage(String source, String language);
}