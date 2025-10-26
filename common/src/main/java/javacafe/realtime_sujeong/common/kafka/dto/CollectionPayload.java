package javacafe.realtime_sujeong.common.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Collection System에서 Cleaning System으로 전송하는 페이로드
 * 수집 완료된 데이터의 메타데이터 정보
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionPayload {

    /**
     * 데이터 고유 식별자 (SHA-256 해시)
     */
    private String dataId;

    /**
     * 데이터 소스 (rss, wiki, api 등)
     */
    private String source;

    /**
     * MongoDB 컬렉션 이름
     * 예: rss_raw_data, wiki_raw_data
     */
    private String mongoCollectionName;

    /**
     * 처리 우선순위 (URGENT, HIGH, NORMAL, LOW)
     */
    @Builder.Default
    private String priority = "NORMAL";

    /**
     * 소스별 상세 정보 (Optional)
     * RSS: 언론사 정보, Wiki: 네임스페이스 등
     */
    private Object sourceDetails;
}