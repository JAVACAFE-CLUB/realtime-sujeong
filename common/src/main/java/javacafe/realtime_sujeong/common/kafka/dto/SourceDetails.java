package javacafe.realtime_sujeong.common.kafka.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * 소스별 상세 정보 마커 인터페이스
 * RSS와 Wiki 등 다양한 소스의 상세 정보를 추상화
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = RssSourceDetails.class, name = "rss"),
        @JsonSubTypes.Type(value = WikiSourceDetails.class, name = "wiki")
})
public interface SourceDetails {
    // 마커 인터페이스 - 구현체에서 각자의 필드를 정의
}