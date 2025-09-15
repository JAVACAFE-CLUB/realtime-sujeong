package com.javacafe.realtime_sujeong.collector.domain.wiki;

import com.javacafe.realtime_sujeong.collector.common.CrawledData;
import com.javacafe.realtime_sujeong.collector.common.KafkaProducerService;
import com.javacafe.realtime_sujeong.collector.dto.DataReference;
import com.javacafe.realtime_sujeong.collector.infrastructure.storage.DataStorageService;
import com.javacafe.realtime_sujeong.collector.service.WikimediaFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class WikiDataCollector {

    private final WikimediaFileService wikimediaFileService;
    private final WikiXmlProcessor xmlProcessor;
    private final WikiTextCleaner textCleaner;
    private final DataStorageService dataStorageService;
    private final KafkaProducerService kafkaProducerService;

    private static final int LOG_INTERVAL = 5000;

    public void processWikimediaData() {
        try {
            log.info("Wikimedia 데이터 처리 시작");

            String objectName = wikimediaFileService.downloadAndStoreWikimediaDump();
            processWikimediaFile(objectName);

        } catch (Exception e) {
            log.error("Wikimedia 데이터 처리 실패", e);
        }
    }

    private void processWikimediaFile(String objectName) throws Exception {
        log.info("Wikimedia 파일 처리 시작: {}", objectName);
        
        AtomicInteger processedCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicInteger filteredCount = new AtomicInteger(0);

        try (InputStream fileStream = wikimediaFileService.getFileFromMinio(objectName);
             BZip2CompressorInputStream bzip2Stream = new BZip2CompressorInputStream(
                     new BufferedInputStream(fileStream))) {

            xmlProcessor.processWikimediaXmlStream(bzip2Stream, wikiPage -> {
                processPage(wikiPage, processedCount, successCount, errorCount, filteredCount);
            });

            log.info("Wikimedia 파일 처리 완료 - 총 {}개 페이지 (성공: {}, 에러: {}, 필터됨: {})", 
                    processedCount.get(), successCount.get(), errorCount.get(), filteredCount.get());

        } catch (Exception e) {
            log.error("Wikimedia 파일 처리 중 오류", e);
            throw e;
        }
    }

    private void processPage(WikiXmlProcessor.WikiPage wikiPage, 
                           AtomicInteger processedCount, AtomicInteger successCount, 
                           AtomicInteger errorCount, AtomicInteger filteredCount) {
        try {
            int currentCount = processedCount.incrementAndGet();
            
            if (wikiPage.getTitle() == null || wikiPage.getText() == null) {
                return;
            }

            if (textCleaner.isRedirectOrSpecialPage(wikiPage.getTitle(), wikiPage.getText())) {
                filteredCount.incrementAndGet();
                return;
            }

            CrawledData crawledData = createCrawledData(wikiPage);
            
            DataReference dataRef = dataStorageService.storeWikiData(crawledData, wikiPage.getId());
            kafkaProducerService.send("wiki-data", dataRef);
            successCount.incrementAndGet();

            if (currentCount % LOG_INTERVAL == 0) {
                log.info("처리 진행률: {}개 완료 (성공: {}, 에러: {}, 필터됨: {})", 
                        currentCount, successCount.get(), errorCount.get(), filteredCount.get());
            }

        } catch (Exception e) {
            log.debug("페이지 처리 중 오류", e);
            errorCount.incrementAndGet();
        }
    }

    private CrawledData createCrawledData(WikiXmlProcessor.WikiPage wikiPage) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("pageId", wikiPage.getId());
        metadata.put("source", "wikimedia_minio");
        metadata.put("language", "ko");

        return CrawledData.builder()
                .source("WIKIMEDIA_MINIO")
                .url("https://ko.wikipedia.org/wiki/" + wikiPage.getTitle().replaceAll(" ", "_"))
                .title(wikiPage.getTitle())
                .content(textCleaner.cleanWikiText(wikiPage.getText()))
                .crawledAt(LocalDateTime.now())
                .publishedAt(LocalDateTime.now())
                .metadata(metadata)
                .build();
    }
}