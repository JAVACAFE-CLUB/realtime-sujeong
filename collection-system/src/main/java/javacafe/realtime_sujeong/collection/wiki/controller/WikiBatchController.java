package javacafe.realtime_sujeong.collection.wiki.controller;

import javacafe.realtime_sujeong.collection.wiki.exception.WikiBatchException;
import javacafe.realtime_sujeong.collection.wiki.service.WikiBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Wiki Batch Job 실행 및 모니터링 REST API Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/wiki")
@RequiredArgsConstructor
public class WikiBatchController {

    private final WikiBatchService wikiBatchService;

    /**
     * Wiki 수집 배치 실행
     *
     * @return 실행 결과
     */
    @PostMapping("/collection/run")
    public ResponseEntity<Map<String, Object>> runWikiCollection() {
        try {
            Map<String, Object> result = wikiBatchService.runWikiCollectionBatch();
            return ResponseEntity.ok(result);
        } catch (WikiBatchException.AlreadyRunningException | WikiBatchException.AlreadyCompletedException e) {
            return createErrorResponse(HttpStatus.CONFLICT, e.getMessage());
        } catch (WikiBatchException.RestartFailedException | WikiBatchException.InvalidParametersException e) {
            return createErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (WikiBatchException.ExecutionFailedException e) {
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Wiki 수집 배치 상태 조회
     *
     * @param jobExecutionId Job 실행 ID
     * @return 배치 상태 정보
     */
    @GetMapping("/collection/status/{jobExecutionId}")
    public ResponseEntity<Map<String, Object>> getWikiCollectionStatus(
            @PathVariable Long jobExecutionId) {

        try {
            Map<String, Object> result = wikiBatchService.getWikiCollectionStatus(jobExecutionId);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("배치 상태 조회 실패: jobExecutionId={}", jobExecutionId, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "상태 조회에 실패했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 헬스 체크 API
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Wiki Batch Controller");
        return ResponseEntity.ok(response);
    }

    /**
     * 에러 응답 생성 헬퍼 메서드
     */
    private ResponseEntity<Map<String, Object>> createErrorResponse(HttpStatus status, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return ResponseEntity.status(status).body(response);
    }
}