package javacafe.realtime_sujeong.indexing.cleaning.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * Cleaned Data Domain Model
 *
 * MongoDB cleaned_data 컬렉션의 도메인 모델
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "cleaned_data")
public class CleanedData {

    @Id
    private String id;

    /**
     * 데이터 고유 식별자 (URL 또는 pageId)
     */
    @Field("dataId")
    private String dataId;

    /**
     * 데이터 소스 (rss, wiki)
     */
    @Field("source")
    private String source;

    /**
     * 정제된 제목
     */
    @Field("cleanedTitle")
    private String cleanedTitle;

    /**
     * 정제된 본문
     */
    @Field("cleanedContent")
    private String cleanedContent;

    /**
     * 언어 (ko, en 등)
     */
    @Field("language")
    private String language;

    /**
     * 메타데이터
     */
    @Field("metadata")
    private Object metadata;

    /**
     * 정제 완료 시각
     */
    @Field("cleanedAt")
    private LocalDateTime cleanedAt;

    /**
     * 생성 시각
     */
    @Field("createdAt")
    private LocalDateTime createdAt;
}