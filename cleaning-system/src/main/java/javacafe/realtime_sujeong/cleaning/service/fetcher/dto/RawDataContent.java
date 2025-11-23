package javacafe.realtime_sujeong.cleaning.service.fetcher.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 원본 데이터 컨텐츠 DTO
 * Fetcher에서 반환하는 표준 형식
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawDataContent {

    /**
     * 데이터 고유 ID
     */
    private String dataId;

    /**
     * 문서 제목
     */
    private String title;

    /**
     * 본문 내용
     * - RSS: 이미 크롤링된 content
     * - Wiki: Tika로 변환된 plain text
     */
    private String content;

    /**
     * 원본 URL
     * - RSS: 뉴스 URL
     * - Wiki: null (URL 없음)
     */
    private String url;

    /**
     * 데이터 소스 (rss, wiki)
     */
    private String source;
}
