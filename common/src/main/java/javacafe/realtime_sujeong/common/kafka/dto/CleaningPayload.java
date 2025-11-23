package javacafe.realtime_sujeong.common.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cleaning System에서 Indexing System으로 전송하는 페이로드
 * 정제 완료된 데이터의 메타데이터 정보
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CleaningPayload {

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
     * 예: cleaned_data
     */
    private String mongoCollectionName;

    /**
     * 처리 우선순위 (URGENT, HIGH, NORMAL, LOW)
     */
    @Builder.Default
    private String priority = "NORMAL";

    /**
     * 소스별 상세 정보 (Optional)
     * RSS: RssSourceDetails, Wiki: WikiSourceDetails
     */
    private SourceDetails sourceDetails;
}