package javacafe.realtime_sujeong.common.dto;

import jakarta.xml.bind.annotation.*;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Wiki dump에서 파싱된 페이지 데이터
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "page", namespace = "http://www.mediawiki.org/xml/export-0.11/")
@XmlAccessorType(XmlAccessType.FIELD)
public class WikiPage {

    /**
     * 페이지 제목
     */
    @XmlElement(name = "title")
    private String title;

    /**
     * 페이지 ID (Wiki 고유 ID)
     */
    @XmlElement(name = "id")
    private String pageId;

    /**
     * 네임스페이스
     */
    @XmlElement(name = "ns")
    private String namespace;

    /**
     * revision 정보 (중첩된 객체)
     */
    @XmlElement(name = "revision")
    private Revision revision;

    /**
     * Revision 정보 클래스
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Revision {

        /**
         * 리비전 ID
         */
        @XmlElement(name = "id")
        private String revisionId;

        /**
         * 최종 수정 타임스탬프
         */
        @XmlElement(name = "timestamp")
        @XmlJavaTypeAdapter(LocalDateTimeAdapter.class)
        private LocalDateTime timestamp;

        /**
         * 기여자 정보
         */
        @XmlElement(name = "contributor")
        private Contributor contributor;

        /**
         * 페이지 모델 (wikitext, etc.)
         */
        @XmlElement(name = "model")
        private String model;

        /**
         * 페이지 포맷 (text/x-wiki, etc.)
         */
        @XmlElement(name = "format")
        private String format;

        /**
         * 페이지 본문 (wikitext 형식)
         */
        @XmlElement(name = "text")
        private TextContent text;
    }

    /**
     * 기여자 정보 클래스
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Contributor {

        @XmlElement(name = "username")
        private String username;

        @XmlElement(name = "id")
        private String userId;
    }

    /**
     * Text 내용 클래스 (bytes 속성 포함)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class TextContent {

        @XmlAttribute(name = "bytes")
        private Integer bytes;

        @XmlValue
        private String content;
    }
}