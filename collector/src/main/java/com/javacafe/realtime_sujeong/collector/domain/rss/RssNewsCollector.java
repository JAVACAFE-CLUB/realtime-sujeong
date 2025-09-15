package com.javacafe.realtime_sujeong.collector.domain.rss;

import com.javacafe.realtime_sujeong.collector.common.CrawledData;
import com.javacafe.realtime_sujeong.collector.common.KafkaProducerService;
import com.javacafe.realtime_sujeong.collector.dto.DataReference;
import com.javacafe.realtime_sujeong.collector.infrastructure.storage.DataStorageService;
import com.javacafe.realtime_sujeong.collector.service.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class RssNewsCollector {

    private final RssParser rssParser;
    private final DuplicateChecker duplicateChecker;
    private final DataStorageService dataStorageService;
    private final KafkaProducerService kafkaProducerService;
    private final MinioStorageService minioStorageService;
    
    private final Executor executor = Executors.newFixedThreadPool(8);
    
    private static final Map<String, String> RSS_URLS = Map.of(
        "통합뉴스", "https://akngs.github.io/knews-rss/all.xml"
    );

    public void collectAllRssNews() {
        try {
            log.info("RSS 뉴스 수집 시작 - {}개 카테고리", RSS_URLS.size());
            
            minioStorageService.ensureBucketExists(minioStorageService.getNaverNewsBucket());
            
            AtomicInteger totalSuccess = new AtomicInteger(0);
            AtomicInteger totalFail = new AtomicInteger(0);
            
            List<CompletableFuture<Void>> futures = RSS_URLS.entrySet().stream()
                    .map(entry -> CompletableFuture.runAsync(() -> 
                        collectCategoryNews(entry.getKey(), entry.getValue(), totalSuccess, totalFail), executor))
                    .toList();
            
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            log.info("RSS 뉴스 수집 완료 - 성공: {}, 실패: {}", totalSuccess.get(), totalFail.get());
            
        } catch (Exception e) {
            log.error("RSS 뉴스 수집 실패", e);
        }
    }
    
    private void collectCategoryNews(String category, String rssUrl, AtomicInteger totalSuccess, AtomicInteger totalFail) {
        try {
            log.info("[{}] RSS 피드 처리 시작", category);
            
            List<CrawledData> newItems = parseRssFeedWithEarlyStop(rssUrl, category);
            log.info("[{}] 신규 데이터 {}개 발견", category, newItems.size());
            
            int successCount = processNewsItems(newItems, category);
            int failCount = newItems.size() - successCount;
            
            totalSuccess.addAndGet(successCount);
            totalFail.addAndGet(failCount);
            
            log.info("[{}] 처리 완료 - 성공: {}, 실패: {}", category, successCount, failCount);
            
        } catch (Exception e) {
            log.error("[{}] RSS 피드 처리 실패", category, e);
            totalFail.incrementAndGet();
        }
    }
    
    private List<CrawledData> parseRssFeedWithEarlyStop(String rssUrl, String category) throws Exception {
        CrawledData lastCrawledData = duplicateChecker.getLastCrawledData();
        List<CrawledData> allItems = rssParser.parseRssFeed(rssUrl, category);
        
        if (lastCrawledData == null) {
            LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
            LocalDateTime todayEnd = todayStart.plusDays(1);
            
            log.info("기존 데이터 없음. 오늘 날짜 데이터만 처리");
            
            return allItems.stream()
                    .filter(item -> {
                        LocalDateTime publishedAt = item.getPublishedAt();
                        return publishedAt.isAfter(todayStart) && publishedAt.isBefore(todayEnd);
                    })
                    .toList();
        } else {
            log.info("마지막 데이터: {}", lastCrawledData.getTitle());
            
            List<CrawledData> newItems = new ArrayList<>();
            
            for (int i = allItems.size() - 1; i >= 0; i--) {
                CrawledData item = allItems.get(i);
                
                if (duplicateChecker.isSameNews(item, lastCrawledData)) {
                    log.info("동일한 뉴스 발견 - 파싱 중단");
                    break;
                }
                
                newItems.add(0, item);
            }
            
            return newItems;
        }
    }
    
    private int processNewsItems(List<CrawledData> items, String category) {
        int successCount = 0;
        
        for (int i = 0; i < items.size(); i++) {
            CrawledData item = items.get(i);
            try {
                DataReference dataRef = dataStorageService.storeNewsData(item, category, i);
                kafkaProducerService.send("naver-rss-news", dataRef);
                duplicateChecker.markAsProcessed(item.getUrl());
                successCount++;
                
                if ((i + 1) % 50 == 0) {
                    log.info("[{}] 진행률: {}/{}", category, i + 1, items.size());
                }
                
            } catch (Exception e) {
                log.error("[{}] 데이터 처리 실패: {}", category, item.getUrl(), e);
            }
        }
        
        return successCount;
    }
    
    public void clearCache() {
        duplicateChecker.clearCache();
    }
    
    public int getCacheSize() {
        return duplicateChecker.getCacheSize();
    }
}