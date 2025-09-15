package com.javacafe.realtime_sujeong.collector.domain.rss;

import com.javacafe.realtime_sujeong.collector.common.CrawledData;
import com.javacafe.realtime_sujeong.collector.service.MinioStorageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class DuplicateChecker {

    private final MinioStorageService minioStorageService;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, Boolean> processedUrls = new ConcurrentHashMap<>();

    public boolean isDuplicate(String url) {
        return processedUrls.containsKey(url);
    }

    public void markAsProcessed(String url) {
        processedUrls.put(url, true);
    }

    public CrawledData getLastCrawledData() {
        try {
            String bucketName = minioStorageService.getNaverNewsBucket();
            LocalDateTime now = LocalDateTime.now();
            
            for (int i = 0; i < 3; i++) {
                String dateStr = now.minusDays(i).toLocalDate().toString();
                String prefix = "news/" + dateStr + "/통합뉴스/";
                
                try {
                    List<String> objectKeys = minioStorageService.listObjects(bucketName, prefix);
                    
                    if (objectKeys.isEmpty()) {
                        continue;
                    }
                    
                    String lastObjectKey = objectKeys.stream()
                            .filter(key -> key.matches(".*/(\\d{5}_\\d+\\.json)$"))
                            .max(Comparator.comparing(key -> {
                                String fileName = key.substring(key.lastIndexOf('/') + 1);
                                String orderStr = fileName.split("_")[0];
                                return Integer.parseInt(orderStr);
                            }))
                            .orElse(null);
                    
                    if (lastObjectKey != null) {
                        String jsonContent = minioStorageService.getFileAsString(bucketName, lastObjectKey);
                        
                        if (jsonContent.trim().isEmpty()) {
                            continue;
                        }
                        
                        CrawledData lastData = objectMapper.readValue(jsonContent, CrawledData.class);
                        
                        if (lastData != null) {
                            log.info("마지막 데이터 발견: {}", lastData.getTitle());
                            return lastData;
                        }
                    }
                    
                } catch (Exception e) {
                    log.debug("날짜별 데이터 조회 실패: {}", dateStr);
                }
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("마지막 크롤링 데이터 조회 실패", e);
            return null;
        }
    }

    public boolean isSameNews(CrawledData news1, CrawledData news2) {
        if (news1 == null || news2 == null) {
            return false;
        }
        return Objects.equals(news1.getUrl(), news2.getUrl());
    }

    public void clearCache() {
        processedUrls.clear();
        log.info("중복 검사 캐시 초기화 완료");
    }

    public int getCacheSize() {
        return processedUrls.size();
    }
}