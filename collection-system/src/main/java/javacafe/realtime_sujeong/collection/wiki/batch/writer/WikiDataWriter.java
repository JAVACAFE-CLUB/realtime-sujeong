package javacafe.realtime_sujeong.collection.wiki.batch.writer;

import javacafe.realtime_sujeong.collection.wiki.domain.WikiRawData;
import javacafe.realtime_sujeong.collection.wiki.domain.WikiRawDataRepository;
import javacafe.realtime_sujeong.collection.kafka.service.KafkaMessageService;
import javacafe.realtime_sujeong.common.kafka.constants.KafkaConstants;
import javacafe.realtime_sujeong.common.kafka.dto.WikiSourceDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Wiki 데이터를 MongoDB에 Bulk Insert하고 Kafka 메시지를 전송하는 ItemWriter
 * - MongoDB Bulk Insert: 청크 크기만큼 한 번에 저장
 * - Kafka 메시지 전송: 각 아이템별 비동기 전송
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WikiDataWriter implements ItemWriter<WikiRawData> {

    private final WikiRawDataRepository wikiRawDataRepository;
    private final KafkaMessageService kafkaMessageService;

    @Override
    public void write(Chunk<? extends WikiRawData> chunk) throws Exception {
        long startTime = System.currentTimeMillis();
        List<? extends WikiRawData> items = chunk.getItems();

        if (items.isEmpty()) {
            log.debug("청크가 비어있어 처리를 건너뜁니다.");
            return;
        }

        log.info("=== Wiki 데이터 저장 시작: {} 개 아이템 ===", items.size());

        try {
            @SuppressWarnings("unchecked")
            List<WikiRawData> wikiRawDataList = (List<WikiRawData>) items;

            // 1. 청크 단위로 기존 데이터 조회 (upsert를 위해)
            long dupCheckStartTime = System.currentTimeMillis();
            List<String> dataIds = wikiRawDataList.stream()
                    .map(WikiRawData::getDataId)
                    .toList();

            List<WikiRawData> existingDataList = wikiRawDataRepository.findAllByDataIdIn(dataIds);
            Map<String, WikiRawData> existingDataMap = existingDataList.stream()
                    .collect(Collectors.toMap(WikiRawData::getDataId, Function.identity()));

            // 2. upsert 대상 분류 (신규 또는 더 최신인 데이터만)
            List<WikiRawData> dataToSave = new ArrayList<>();
            List<WikiRawData> dataToUpdate = new ArrayList<>();
            int skippedCount = 0;

            for (WikiRawData newData : wikiRawDataList) {
                WikiRawData existing = existingDataMap.get(newData.getDataId());
                if (existing == null) {
                    // 신규 데이터
                    dataToSave.add(newData);
                } else {
                    // 기존 데이터가 있음 - timestamp 비교
                    var newTimestamp = newData.getWikiPage().getRevision().getTimestamp();
                    if (existing.isOlderThan(newTimestamp)) {
                        // 기존 데이터가 더 오래됨 → 업데이트
                        existing.updateFromNewer(
                                newData.getWikiPage(),
                                newData.getTitle(),
                                newData.getNamespace()
                        );
                        dataToUpdate.add(existing);
                    } else {
                        // 기존 데이터가 더 최신 → 스킵
                        skippedCount++;
                    }
                }
            }

            long dupCheckEndTime = System.currentTimeMillis();
            log.info("upsert 분류 완료: 전체={}, 신규={}, 업데이트={}, 스킵={} (소요시간: {}ms)",
                    wikiRawDataList.size(), dataToSave.size(), dataToUpdate.size(), skippedCount,
                    (dupCheckEndTime - dupCheckStartTime));

            List<WikiRawData> allDataToSave = new ArrayList<>();
            allDataToSave.addAll(dataToSave);
            allDataToSave.addAll(dataToUpdate);

            if (allDataToSave.isEmpty()) {
                log.info("저장할 데이터가 없습니다. 모든 데이터가 최신 상태입니다.");
                long totalTime = System.currentTimeMillis() - startTime;
                log.info("=== Writer 총 소요시간: {}ms ===", totalTime);
                return;
            }

            // 3. MongoDB Bulk Upsert (신규 + 업데이트)c
            long mongoStartTime = System.currentTimeMillis();
            List<WikiRawData> savedItems = wikiRawDataRepository.saveAll(allDataToSave);
            long mongoEndTime = System.currentTimeMillis();
            log.info("MongoDB Bulk Insert 완료: {} 개 저장 (소요시간: {}ms)",
                savedItems.size(), (mongoEndTime - mongoStartTime));

            // 3. Kafka 메시지 전송 (비동기 배치 전송)
            long kafkaStartTime = System.currentTimeMillis();
            sendKafkaMessages(savedItems);
            long kafkaEndTime = System.currentTimeMillis();
            log.info("Kafka 메시지 전송 완료 (소요시간: {}ms)", (kafkaEndTime - kafkaStartTime));

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("=== Writer 총 소요시간: {}ms ===", totalTime);

        } catch (Exception e) {
            log.error("Wiki 데이터 저장 실패: {} 개 아이템", items.size(), e);
            throw e;  // Spring Batch가 재시도 또는 skip 처리
        }
    }

    /**
     * Kafka 메시지 비동기 배치 전송
     */
    private void sendKafkaMessages(List<WikiRawData> items) {
        log.info("Kafka 메시지 전송 시작: {} 개", items.size());

        // 모든 메시지를 비동기로 전송
        List<CompletableFuture<Boolean>> futures = items.stream()
                .map(wikiRawData -> {
                    // WikiSourceDetails 생성
                    WikiSourceDetails sourceDetails = WikiSourceDetails.builder()
                            .namespace(wikiRawData.getNamespace())
                            .title(wikiRawData.getTitle())
                            .pageId(wikiRawData.getWikiPage().getPageId())
                            .revisionId(wikiRawData.getWikiPage().getRevision().getRevisionId())
                            .timestamp(wikiRawData.getWikiPage().getRevision().getTimestamp())
                            .build();

                    return kafkaMessageService.sendDataCollectedMessage(
                            wikiRawData.getDataId(),
                            KafkaConstants.Sources.WIKI.getValue(),
                            KafkaConstants.Collections.WIKI_RAW_DATA,
                            sourceDetails
                    );
                })
                .toList();

        // 모든 전송 완료 대기 (동기적으로 기다림)
        CompletableFuture<Void> allOf = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );

        try {
            // 모든 비동기 작업이 완료될 때까지 블로킹
            allOf.join();

            long successCount = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(success -> success)
                    .count();

            long failCount = futures.size() - successCount;

            log.info("Kafka 메시지 전송 완료: 성공={}, 실패={}", successCount, failCount);

            if (failCount > 0) {
                log.warn("일부 Kafka 메시지 전송 실패: {} 개", failCount);
            }
        } catch (Exception e) {
            log.error("Kafka 메시지 전송 중 오류 발생", e);
            throw new RuntimeException("Kafka 메시지 전송 실패", e);
        }
    }
}