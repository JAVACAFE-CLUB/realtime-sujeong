package javacafe.realtime_sujeong.cleaning.domain.view;

import javacafe.realtime_sujeong.common.dto.WikiPage;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Wiki 원본 데이터 조회용 View (read-only)
 * cleaning-system에서 wiki_raw_data 컬렉션을 읽기 전용으로 접근
 *
 * collection-system의 WikiRawData와 동일한 컬렉션을 참조하지만,
 * cleaning에 필요한 필드만 포함하여 독립성을 확보
 */
@Document(collection = "wiki_raw_data")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WikiRawDataView {

    /**
     * MongoDB Document ID
     */
    @Id
    private String id;

    /**
     * Wiki 데이터 고유 식별자 (SHA-256: pageId + revisionId)
     */
    @Indexed(unique = true)
    private String dataId;

    /**
     * 데이터 소스 (항상 "wiki")
     */
    @Indexed
    private String source;

    /**
     * Wiki 페이지 네임스페이스 (0: 일반 문서, 14: 분류, 등)
     */
    @Indexed
    private String namespace;

    /**
     * Wiki 페이지 제목 (검색/필터링용)
     */
    @Indexed
    private String title;

    /**
     * Wiki 페이지 원본 데이터
     */
    private WikiPage wikiPage;

    // collectedAt 필드 제외 (cleaning에서 불필요)
    // setter 없음 (read-only)
}