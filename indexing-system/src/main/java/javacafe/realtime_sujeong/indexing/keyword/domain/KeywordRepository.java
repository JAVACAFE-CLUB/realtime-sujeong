package javacafe.realtime_sujeong.indexing.keyword.domain;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Keyword Repository
 *
 * Elasticsearch keyword_index에 대한 Repository
 */
@Repository
public interface KeywordRepository extends ElasticsearchRepository<Keyword, String> {

    /**
     * keywordId로 키워드 조회
     *
     * @param keywordId 키워드 ID (날짜_키워드)
     * @return Optional<Keyword>
     */
    Optional<Keyword> findByKeywordId(String keywordId);

    /**
     * 날짜별 모든 키워드 조회
     *
     * @param date 날짜
     * @return 키워드 리스트
     */
    List<Keyword> findByDate(LocalDate date);

    /**
     * 날짜별 키워드 조회 (점수 내림차순)
     *
     * @param date 날짜
     * @return 점수 순으로 정렬된 키워드 리스트
     */
    List<Keyword> findByDateOrderByScoreDesc(LocalDate date);

    /**
     * 날짜별 Top N 키워드 조회 (점수 내림차순)
     *
     * @param date 날짜
     * @return 점수 순으로 정렬된 상위 키워드 리스트
     */
    List<Keyword> findTop10ByDateOrderByScoreDesc(LocalDate date);

    /**
     * 날짜와 키워드로 조회
     *
     * @param date    날짜
     * @param keyword 키워드
     * @return Optional<Keyword>
     */
    Optional<Keyword> findByDateAndKeyword(LocalDate date, String keyword);

    /**
     * 날짜와 개체 유형으로 조회
     *
     * @param date       날짜
     * @param entityType 개체 유형 (PER, ORG, LOC 등)
     * @return 키워드 리스트
     */
    List<Keyword> findByDateAndEntityType(LocalDate date, String entityType);

    /**
     * 날짜별 키워드 존재 여부 확인
     *
     * @param date    날짜
     * @param keyword 키워드
     * @return 존재 여부
     */
    boolean existsByDateAndKeyword(LocalDate date, String keyword);

    /**
     * 날짜 범위로 키워드 조회
     *
     * @param startDate 시작 날짜
     * @param endDate   종료 날짜
     * @return 키워드 리스트
     */
    List<Keyword> findByDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * 특정 출처의 키워드 조회
     *
     * @param date   날짜
     * @param source 출처 (rss, wiki)
     * @return 키워드 리스트
     */
    List<Keyword> findByDateAndSourcesContaining(LocalDate date, String source);
}