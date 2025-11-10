package javacafe.realtime_sujeong.collection.rss.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * RSS 수집 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RssCollectionResponse {

    /**
     * 성공 여부
     */
    private boolean success;

    /**
     * 메시지
     */
    private String message;

    /**
     * 전체 파싱된 아이템 개수
     */
    private int totalCount;

    /**
     * 저장된 개수
     */
    private int savedCount;

    /**
     * 중복된 개수
     */
    private int duplicateCount;

    /**
     * 수집 시간
     */
    private LocalDateTime collectedAt;

    /**
     * 성공 응답 생성
     */
    public static RssCollectionResponse success(int totalCount, int savedCount, int duplicateCount) {
        return RssCollectionResponse.builder()
                .success(true)
                .message("RSS 피드 수집이 완료되었습니다")
                .totalCount(totalCount)
                .savedCount(savedCount)
                .duplicateCount(duplicateCount)
                .collectedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 실패 응답 생성
     */
    public static RssCollectionResponse failure(String message) {
        return RssCollectionResponse.builder()
                .success(false)
                .message(message)
                .collectedAt(LocalDateTime.now())
                .build();
    }
}
