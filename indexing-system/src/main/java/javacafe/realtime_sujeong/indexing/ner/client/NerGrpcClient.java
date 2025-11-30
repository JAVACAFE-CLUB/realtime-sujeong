package javacafe.realtime_sujeong.indexing.ner.client;

import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import javacafe.realtime_sujeong.indexing.ner.dto.NamedEntity;
import javacafe.realtime_sujeong.indexing.ner.dto.NerResult;
import lombok.extern.slf4j.Slf4j;
import ner.Ner;
import ner.NerServiceGrpc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * NER gRPC Client
 *
 * NER 서버와 gRPC 통신을 담당하는 클라이언트
 */
@Slf4j
@Component
public class NerGrpcClient {

    private final NerServiceGrpc.NerServiceBlockingStub blockingStub;
    private final long deadlineSeconds;

    @Value("${ner.confidence.threshold:0.8}")
    private float defaultConfidenceThreshold;

    @Autowired
    public NerGrpcClient(ManagedChannel nerGrpcChannel, long nerDeadlineSeconds) {
        this.blockingStub = NerServiceGrpc.newBlockingStub(nerGrpcChannel);
        this.deadlineSeconds = nerDeadlineSeconds;
        log.info("NerGrpcClient initialized with deadline: {}s", deadlineSeconds);
    }

    /**
     * 단일 텍스트에서 개체명 추출
     *
     * @param text 분석할 텍스트
     * @return NER 결과
     */
    public NerResult extractEntities(String text) {
        return extractEntities(text, defaultConfidenceThreshold);
    }

    /**
     * 단일 텍스트에서 개체명 추출 (신뢰도 임계값 지정)
     *
     * @param text                 분석할 텍스트
     * @param confidenceThreshold 신뢰도 임계값
     * @return NER 결과
     */
    public NerResult extractEntities(String text, float confidenceThreshold) {
        try {
            log.debug("Extracting entities from text (length: {})", text.length());

            Ner.NerRequest request = Ner.NerRequest.newBuilder()
                    .setText(text)
                    .setConfidenceThreshold(confidenceThreshold)
                    .build();

            Ner.NerResponse response = blockingStub
                    .withDeadlineAfter(deadlineSeconds, TimeUnit.SECONDS)
                    .extractEntities(request);

            List<NamedEntity> entities = response.getEntitiesList().stream()
                    .map(this::convertToNamedEntity)
                    .collect(Collectors.toList());

            log.debug("Extracted {} entities in {}ms",
                    entities.size(), response.getProcessingTimeMs());

            return NerResult.builder()
                    .entities(entities)
                    .processingTimeMs(response.getProcessingTimeMs())
                    .originalText(text)
                    .build();

        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed: {}", e.getStatus(), e);
            throw new RuntimeException("Failed to extract entities from NER server", e);
        }
    }

    /**
     * 배치 텍스트에서 개체명 추출
     *
     * @param texts 분석할 텍스트 리스트 (최대 100개)
     * @return NER 결과 리스트
     */
    public List<NerResult> extractEntitiesBatch(List<String> texts) {
        return extractEntitiesBatch(texts, defaultConfidenceThreshold);
    }

    /**
     * 배치 텍스트에서 개체명 추출 (신뢰도 임계값 지정)
     *
     * @param texts                분석할 텍스트 리스트
     * @param confidenceThreshold 신뢰도 임계값
     * @return NER 결과 리스트
     */
    public List<NerResult> extractEntitiesBatch(List<String> texts, float confidenceThreshold) {
        try {
            log.debug("Extracting entities from {} texts", texts.size());

            Ner.NerBatchRequest request = Ner.NerBatchRequest.newBuilder()
                    .addAllTexts(texts)
                    .setConfidenceThreshold(confidenceThreshold)
                    .build();

            Ner.NerBatchResponse response = blockingStub
                    .withDeadlineAfter(deadlineSeconds, TimeUnit.SECONDS)
                    .extractEntitiesBatch(request);

            List<NerResult> results = response.getResultsList().stream()
                    .map(batchResult -> {
                        List<NamedEntity> entities = batchResult.getEntitiesList().stream()
                                .map(this::convertToNamedEntity)
                                .collect(Collectors.toList());

                        return NerResult.builder()
                                .entities(entities)
                                .processingTimeMs(response.getProcessingTimeMs())
                                .originalText(batchResult.getText())
                                .build();
                    })
                    .collect(Collectors.toList());

            log.debug("Extracted entities from {} texts in {}ms",
                    response.getTotalTexts(), response.getProcessingTimeMs());

            return results;

        } catch (StatusRuntimeException e) {
            log.error("gRPC batch call failed: {}", e.getStatus(), e);
            throw new RuntimeException("Failed to extract entities batch from NER server", e);
        }
    }

    /**
     * Protobuf NerEntity를 DTO로 변환
     */
    private NamedEntity convertToNamedEntity(Ner.NerEntity entity) {
        return NamedEntity.builder()
                .word(entity.getWord())
                .entityType(entity.getEntity())
                .confidence(entity.getConfidence())
                .start(entity.getStart())
                .end(entity.getEnd())
                .build();
    }
}