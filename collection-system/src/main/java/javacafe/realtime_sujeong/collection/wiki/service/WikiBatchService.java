package javacafe.realtime_sujeong.collection.wiki.service;

import javacafe.realtime_sujeong.collection.wiki.exception.WikiBatchException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Wiki 배치 작업 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WikiBatchService {

    private final JobLauncher jobLauncher;
    private final Job wikiCollectionJob;

    /**
     * Wiki 수집 배치 실행
     *
     * @return 실행 결과 정보
     * @throws WikiBatchException.AlreadyRunningException 이미 실행 중인 배치가 있을 경우
     * @throws WikiBatchException.RestartFailedException 배치 재시작 실패 시
     * @throws WikiBatchException.AlreadyCompletedException 이미 완료된 배치일 경우
     * @throws WikiBatchException.InvalidParametersException 잘못된 파라미터일 경우
     * @throws WikiBatchException.ExecutionFailedException 배치 실행 실패
     */
    public Map<String, Object> runWikiCollectionBatch() {
        log.info("Wiki 수집 배치 실행 시작");

        try {
            // Job 파라미터 생성 (매번 다른 파라미터로 실행되도록)
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("startTime", LocalDateTime.now())
                    .toJobParameters();

            // Job 실행
            JobExecution jobExecution = jobLauncher.run(wikiCollectionJob, jobParameters);

            // 실행 결과 정보 생성
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("jobExecutionId", jobExecution.getId());
            result.put("jobName", jobExecution.getJobInstance().getJobName());
            result.put("status", jobExecution.getStatus().name());
            result.put("startTime", jobExecution.getStartTime());
            result.put("message", "Wiki 수집 배치가 시작되었습니다.");

            log.info("Wiki 수집 배치 실행 성공: jobExecutionId={}, status={}",
                    jobExecution.getId(), jobExecution.getStatus());

            return result;

        } catch (JobExecutionAlreadyRunningException e) {
            log.warn("이미 실행 중인 Wiki 수집 배치가 있습니다.", e);
            throw new WikiBatchException.AlreadyRunningException("이미 실행 중인 배치가 있습니다.", e);

        } catch (JobRestartException e) {
            log.error("Wiki 수집 배치 재시작 실패", e);
            throw new WikiBatchException.RestartFailedException("배치 재시작에 실패했습니다.", e);

        } catch (JobInstanceAlreadyCompleteException e) {
            log.warn("이미 완료된 Wiki 수집 배치입니다.", e);
            throw new WikiBatchException.AlreadyCompletedException("이미 완료된 배치입니다.", e);

        } catch (JobParametersInvalidException e) {
            log.error("잘못된 Job 파라미터", e);
            throw new WikiBatchException.InvalidParametersException("잘못된 배치 파라미터입니다.", e);

        } catch (Exception e) {
            log.error("Wiki 수집 배치 실행 중 예상치 못한 오류 발생", e);
            throw new WikiBatchException.ExecutionFailedException("배치 실행 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * Wiki 수집 배치 상태 조회
     *
     * @param jobExecutionId Job 실행 ID
     * @return 배치 상태 정보
     */
    public Map<String, Object> getWikiCollectionStatus(Long jobExecutionId) {
        Map<String, Object> result = new HashMap<>();

        // 실제로는 JobExplorer를 사용해 조회해야 하지만,
        // 간단한 예제로 현재는 기본 응답만 반환
        result.put("jobExecutionId", jobExecutionId);
        result.put("message", "배치 상태 조회 기능은 JobExplorer를 통해 구현 예정입니다.");

        log.debug("배치 상태 조회: jobExecutionId={}", jobExecutionId);

        return result;
    }
}