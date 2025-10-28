package javacafe.realtime_sujeong.cleaning.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 정제된 데이터 엔티티
 * MongoDB의 cleaned_data 컬렉션에 저장
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "cleaned_data")
public class CleanedData {

    /**
     * MongoDB 내부 ID
     */
    @Id
    private String id;

    /**
     * 데이터 고유 식별자 (SHA-256 해시)
     * raw_data의 dataId와 동일
     */
    @Indexed(unique = true)
    private String dataId;

    /**
     * 데이터 소스 (rss, wiki, api 등)
     */
    @Indexed
    private String source;

    /**
     * 문서 제목
     */
    private String title;

    /**
     * 정제된 본문 텍스트
     */
    private String cleanedContent;

    /**
     * 언어 코드 (ko, en, ja 등)
     */
    @Indexed
    private String language;

    /**
     * 원본 URL (RSS의 경우)
     */
    private String url;

    /**
     * 메타데이터
     * - originalLength: 원본 텍스트 길이
     * - cleanedLength: 정제 후 텍스트 길이
     * - strategyName: RSS 수집 전략명 (RSS만)
     * - namespace: Wiki 네임스페이스 (Wiki만)
     * - pageId: Wiki 페이지 ID (Wiki만)
     */
    private Map<String, Object> metadata;

    /**
     * 원본 데이터 수집 시각
     */
    private LocalDateTime collectedAt;

    /**
     * 정제 처리 완료 시각
     */
    @Indexed
    private LocalDateTime processedAt;

    /**
     * 생성 시각 (MongoDB 저장 시각)
     */
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 수정 시각 (MongoDB 갱신 시각)
     */
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
