package javacafe.realtime_sujeong.cleaning.service;

import javacafe.realtime_sujeong.cleaning.domain.CleanedData;
import javacafe.realtime_sujeong.cleaning.domain.CleanedDataRepository;
import javacafe.realtime_sujeong.cleaning.service.fetcher.RawDataFetcher;
import javacafe.realtime_sujeong.cleaning.service.fetcher.RawDataFetcherFactory;
import javacafe.realtime_sujeong.cleaning.service.fetcher.dto.RawDataContent;
import javacafe.realtime_sujeong.cleaning.service.processor.LanguageDetector;
import javacafe.realtime_sujeong.cleaning.service.processor.TextNormalizer;
import javacafe.realtime_sujeong.common.kafka.dto.CollectionPayload;
import javacafe.realtime_sujeong.common.kafka.dto.KafkaMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 정제 처리 서비스
 * Kafka Consumer로부터 받은 메시지를 일괄 처리하여 데이터 정제 수행
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CleaningService {

    private final RawDataFetcherFactory fetcherFactory;
    private final TextNormalizer textNormalizer;
    private final LanguageDetector languageDetector;
    private final CleanedDataRepository cleanedDataRepository;
    private final javacafe.realtime_sujeong.cleaning.kafka.producer.CleaningProducer cleaningProducer;

    // 최소 컨텐츠 길이 (validation용)
    private static final int MIN_CONTENT_LENGTH = 10;

    /**
     * 배치 정제 처리 (메인 메서드)
     * Kafka Consumer로부터 받은 메시지 리스트를 일괄 처리
     *
     * @param messages Kafka 메시지 리스트 (최대 100개)
     * @return 정제 완료된 CleanedData 리스트
     */
    public List<CleanedData> processBatch(List<KafkaMessage<CollectionPayload>> messages) {
        if (messages == null || messages.isEmpty()) {
            log.warn("Empty message batch received");
            return Collections.emptyList();
        }

        log.info("Processing batch of {} messages", messages.size());
        long startTime = System.currentTimeMillis();

        try {
            // 1. 메시지를 source별로 그룹화 (rss, wiki)
            Map<String, List<String>> dataIdsBySource = groupBySource(messages);
            log.debug("Grouped by source: {}", dataIdsBySource.keySet());

            // 2. source별로 원본 데이터 일괄 조회 (MongoDB bulk query)
            List<RawDataContent> rawDataList = fetchRawDataBatch(dataIdsBySource);
            log.debug("Fetched {} raw data items", rawDataList.size());

            // 3. 각 데이터 정제 처리 (정규화 + 언어 감지)
            List<CleanedData> cleanedDataList = rawDataList.stream()
                    .map(this::cleanData)
                    .filter(Objects::nonNull)  // 에러 발생 건 제외
                    .toList();

            log.debug("Cleaned {} data items", cleanedDataList.size());

            // 정제된 데이터가 없으면 조기 반환
            if (cleanedDataList.isEmpty()) {
                log.warn("No valid data after cleaning");
                return Collections.emptyList();
            }

            // 4. MongoDB 일괄 저장 (bulk insert)
            saveCleanedDataBatch(cleanedDataList);

            // 5. Kafka 일괄 발행 (cleaning-to-indexing 토픽)
            cleaningProducer.sendBatch(cleanedDataList);

            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("Batch processing completed: {}/{} cleaned in {}ms",
                    cleanedDataList.size(), messages.size(), elapsedTime);

            return cleanedDataList;

        } catch (Exception e) {
            log.error("Error during batch processing", e);
            throw new RuntimeException("Batch processing failed", e);
        }
    }

    /**
     * 메시지를 source별로 그룹화
     * RSS와 Wiki를 분리하여 적절한 Fetcher로 처리하기 위함
     *
     * @param messages Kafka 메시지 리스트
     * @return Map<source, List<dataId>>
     */
    private Map<String, List<String>> groupBySource(List<KafkaMessage<CollectionPayload>> messages) {
        Map<String, List<String>> grouped = new HashMap<>();

        for (KafkaMessage<CollectionPayload> message : messages) {
            CollectionPayload payload = message.getPayload();

            // 페이로드 검증
            if (!validatePayload(payload)) {
                continue;
            }

            String source = payload.getSource();
            String dataId = payload.getDataId();

            // source별로 dataId 수집
            grouped.computeIfAbsent(source.toLowerCase(), k -> new ArrayList<>())
                   .add(dataId);
        }

        log.debug("Grouped messages - RSS: {}, Wiki: {}",
                grouped.getOrDefault("rss", Collections.emptyList()).size(),
                grouped.getOrDefault("wiki", Collections.emptyList()).size());

        return grouped;
    }

    /**
     * source별로 원본 데이터 일괄 조회
     * Factory Pattern + MongoDB Bulk Query 사용
     *
     * @param dataIdsBySource source별 dataId 맵
     * @return 원본 데이터 컨텐츠 리스트
     */
    private List<RawDataContent> fetchRawDataBatch(Map<String, List<String>> dataIdsBySource) {
        List<RawDataContent> allData = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : dataIdsBySource.entrySet()) {
            String source = entry.getKey();
            List<String> dataIds = entry.getValue();

            if (dataIds.isEmpty()) {
                continue;
            }

            try {
                // Factory에서 적절한 Fetcher 선택
                RawDataFetcher fetcher = fetcherFactory.getFetcher(source);

                // MongoDB bulk query 실행
                List<RawDataContent> batch = fetcher.fetchContentBatch(dataIds);
                allData.addAll(batch);

                log.debug("Fetched {} items from source '{}'", batch.size(), source);

            } catch (IllegalArgumentException e) {
                log.error("Unsupported source type: {}", source, e);
                // 지원하지 않는 source는 스킵

            } catch (Exception e) {
                log.error("Error fetching data for source: {}", source, e);
                // 해당 source는 스킵하고 다른 source 계속 진행
            }
        }

        return allData;
    }

    /**
     * 단일 데이터 정제 처리
     * 정규화 + 언어 감지 + CleanedData 생성
     *
     * @param rawData 원본 데이터
     * @return 정제된 CleanedData (에러 시 null)
     */
    private CleanedData cleanData(RawDataContent rawData) {
        try {
            log.debug("Cleaning data: {}", rawData.getDataId());

            // 1. 텍스트 정규화 (HTML 태그, 특수문자, 공백 제거)
            String normalizedContent = textNormalizer.normalize(rawData.getContent());

            // 2. 정규화 결과 검증 (최소 길이 체크)
            if (!textNormalizer.isValid(normalizedContent, MIN_CONTENT_LENGTH)) {
                log.warn("Invalid content after normalization for dataId: {} (too short)",
                        rawData.getDataId());
                return null;  // 유효하지 않은 데이터는 스킵
            }

            // 3. 언어 감지
            String language = languageDetector.detect(normalizedContent);
            log.debug("Detected language: {} for dataId: {}",
                    languageDetector.getLanguageName(language), rawData.getDataId());

            // 4. 메타데이터 생성
            Map<String, Object> metadata = buildMetadata(rawData, normalizedContent);

            // 5. CleanedData 객체 생성
            return CleanedData.builder()
                    .dataId(rawData.getDataId())
                    .source(rawData.getSource())
                    .title(rawData.getTitle())
                    .cleanedContent(normalizedContent)
                    .language(language)
                    .url(rawData.getUrl())
                    .metadata(metadata)
                    .processedAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Error cleaning data for dataId: {}", rawData.getDataId(), e);
            return null;  // 에러 발생 시 해당 데이터만 스킵
        }
    }

    /**
     * 정제 결과 메타데이터 생성
     *
     * @param rawData 원본 데이터
     * @param cleanedContent 정제된 컨텐츠
     * @return 메타데이터 맵
     */
    private Map<String, Object> buildMetadata(RawDataContent rawData, String cleanedContent) {
        Map<String, Object> metadata = new HashMap<>();

        // 원본 텍스트 길이
        metadata.put("originalLength", rawData.getContent().length());

        // 정제 후 텍스트 길이
        metadata.put("cleanedLength", cleanedContent.length());

        // source별 추가 정보
        if ("rss".equals(rawData.getSource())) {
            // RSS의 경우 URL 포함
            if (rawData.getUrl() != null) {
                metadata.put("url", rawData.getUrl());
            }
        } else if ("wiki".equals(rawData.getSource())) {
            // Wiki의 경우 추가 정보 (필요시 확장)
            metadata.put("extracted", "tika");
        }

        return metadata;
    }

    /**
     * 정제된 데이터를 MongoDB에 일괄 저장
     * Upsert 방식으로 중복 처리 안전
     *
     * @param cleanedDataList 정제 데이터 리스트
     */
    private void saveCleanedDataBatch(List<CleanedData> cleanedDataList) {
        if (cleanedDataList == null || cleanedDataList.isEmpty()) {
            log.warn("No cleaned data to save");
            return;
        }

        try {
            // MongoDB bulk insert (100회 저장 → 1회 저장)
            List<CleanedData> savedList = cleanedDataRepository.saveAll(cleanedDataList);
            log.info("Saved {} cleaned data to MongoDB", savedList.size());

        } catch (Exception e) {
            log.error("Error saving cleaned data batch, trying fallback", e);
            // 배치 저장 실패 시 개별 저장 시도 (fallback)
            saveFallback(cleanedDataList);
        }
    }

    /**
     * Fallback 저장 (개별 저장)
     * 배치 저장 실패 시 개별로 저장 시도
     *
     * @param cleanedDataList 정제 데이터 리스트
     */
    private void saveFallback(List<CleanedData> cleanedDataList) {
        int successCount = 0;
        int failureCount = 0;

        for (CleanedData data : cleanedDataList) {
            try {
                cleanedDataRepository.save(data);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to save data: {}", data.getDataId(), e);
                failureCount++;
            }
        }

        log.info("Fallback save completed: {} success, {} failed",
                successCount, failureCount);
    }

    /**
     * CollectionPayload 유효성 검증
     *
     * @param payload CollectionPayload
     * @return 유효 여부
     */
    private boolean validatePayload(CollectionPayload payload) {
        if (payload == null) {
            log.warn("Null payload received");
            return false;
        }

        // 필수 필드 검증
        String dataId = payload.getDataId();
        String source = payload.getSource();

        if (dataId == null || dataId.isEmpty()) {
            log.warn("Payload with empty dataId: {}", payload);
            return false;
        }

        if (source == null || source.isEmpty()) {
            log.warn("Payload with empty source: {}", payload);
            return false;
        }

        // Source 타입 검증 (rss, wiki만 허용)
        String normalizedSource = source.toLowerCase();
        if (!normalizedSource.equals("rss") && !normalizedSource.equals("wiki")) {
            log.warn("Unsupported source type: {}", source);
            return false;
        }

        return true;
    }
}
