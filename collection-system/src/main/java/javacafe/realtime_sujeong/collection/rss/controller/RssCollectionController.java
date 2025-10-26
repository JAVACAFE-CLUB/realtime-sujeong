package javacafe.realtime_sujeong.collection.rss.controller;

import javacafe.realtime_sujeong.collection.rss.controller.dto.RssCollectionRequest;
import javacafe.realtime_sujeong.collection.rss.controller.dto.RssCollectionResponse;
import javacafe.realtime_sujeong.collection.rss.service.RssCollectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * RSS 수집 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/collection/rss")
@RequiredArgsConstructor
public class RssCollectionController {

    private final RssCollectionService rssCollectionService;

    /**
     * RSS 피드 수집 트리거
     *
     * POST /api/collection/rss/collect
     * {
     *   "source": "chosun"
     * }
     */
    @PostMapping("/collect")
    public ResponseEntity<RssCollectionResponse> collectRssFeed(
            @Valid @RequestBody RssCollectionRequest request) {

        log.info("RSS 수집 요청 - source: {}", request.getSource());

        try {
            RssCollectionService.CollectionResult result =
                    rssCollectionService.collectFeed(request.getSource());

            RssCollectionResponse response = RssCollectionResponse.success(
                    result.totalCount(),
                    result.savedCount(),
                    result.duplicateCount()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("RSS 수집 실패", e);
            RssCollectionResponse response = RssCollectionResponse.failure(
                    "RSS 수집 중 오류가 발생했습니다: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(response);
        }
    }


    /**
     * 수집 통계 조회
     *
     * GET /api/collection/rss/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {

        Map<String, Object> stats = rssCollectionService.getStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * 지원하는 소스 목록 조회
     *
     * GET /api/collection/rss/sources
     */
    @GetMapping("/sources")
    public ResponseEntity<List<String>> getSupportedSources() {
        // 현재 구현된 언론사 크롤링 전략
        List<String> supportedSources = List.of("chosun", "maeil", "donga");
        return ResponseEntity.ok(supportedSources);
    }
}
