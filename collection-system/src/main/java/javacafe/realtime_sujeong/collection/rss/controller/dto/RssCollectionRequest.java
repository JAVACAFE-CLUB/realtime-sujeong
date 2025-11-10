package javacafe.realtime_sujeong.collection.rss.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * RSS 수집 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RssCollectionRequest {

    /**
     * 데이터 소스 (언론사)
     * 예: chosun, maeil, donga
     */
    @NotBlank(message = "소스는 필수입니다")
    private String source;
}
