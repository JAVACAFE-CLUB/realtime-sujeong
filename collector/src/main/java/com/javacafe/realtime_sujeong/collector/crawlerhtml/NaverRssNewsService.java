package com.javacafe.realtime_sujeong.collector.crawlerhtml;

import com.javacafe.realtime_sujeong.collector.common.CrawledData;
import com.javacafe.realtime_sujeong.collector.common.KafkaProducerService;
import com.javacafe.realtime_sujeong.collector.dto.DataReference;
import com.javacafe.realtime_sujeong.collector.service.MinioStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.jsoup.Jsoup;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class NaverRssNewsService {

    private final KafkaProducerService kafkaProducerService;
    private final MinioStorageService minioStorageService;
    private final ObjectMapper objectMapper;
    
    private final Set<String> processedUrls = ConcurrentHashMap.newKeySet();
    private final Executor executor = Executors.newFixedThreadPool(8);
    
    // 통합 한국 뉴스 RSS URL
    private static final Map<String, String> RSS_URLS = Map.of(
        "통합뉴스", "https://akngs.github.io/knews-rss/all.xml"
    );

    public void collectAllRssNews() {
        try {
            log.info("네이버 RSS 뉴스 수집 시작 - 총 {}개 카테고리", RSS_URLS.size());
            
            // MinIO 버킷 확인 및 생성
            String bucketName = minioStorageService.getNaverNewsBucket();
            minioStorageService.ensureBucketExists(bucketName);
            
            AtomicInteger totalSuccess = new AtomicInteger(0);
            AtomicInteger totalFail = new AtomicInteger(0);
            
            List<CompletableFuture<Void>> futures = RSS_URLS.entrySet().stream()
                    .map(entry -> CompletableFuture.runAsync(() -> 
                        collectCategoryNews(entry.getKey(), entry.getValue(), totalSuccess, totalFail), executor))
                    .toList();
            
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            log.info("네이버 RSS 뉴스 수집 완료 - 성공: {}개, 실패: {}개", totalSuccess.get(), totalFail.get());
            
        } catch (Exception e) {
            log.error("네이버 RSS 뉴스 수집 초기화 실패", e);
        }
    }
    
    private void collectCategoryNews(String category, String rssUrl, AtomicInteger totalSuccess, AtomicInteger totalFail) {
        try {
            log.info("[{}] RSS 피드 처리 시작: {}", category, rssUrl);
            
            log.info("[{}] RSS 피드 파싱 및 신규 데이터 검색 중...", category);
            List<CrawledData> newItems = parseRssFeedWithEarlyStop(rssUrl, category);
            log.info("[{}] 신규 {}개 발견", category, newItems.size());
            
            int successCount = 0;
            int failCount = 0;
            
            log.info("[{}] {}개 뉴스 아이템 처리 시작", category, newItems.size());
            
            for (int i = 0; i < newItems.size(); i++) {
                CrawledData item = newItems.get(i);
                try {
                    log.info("[{}] 처리 중 ({}/{}) - {}", category, i + 1, newItems.size(), item.getTitle());
                    
                    // 1. MinIO에 원본 데이터 저장 (순서 포함)
                    DataReference dataRef = storeToMinio(item, category, i);
                    
                    // 2. 메타데이터만 Kafka로 전송
                    kafkaProducerService.send("naver-rss-news", dataRef);
                    
                    processedUrls.add(item.getUrl());
                    successCount++;
                    
                    log.info("[{}] 처리 완료 ({}/{}) - 성공", category, i + 1, newItems.size());
                } catch (Exception e) {
                    log.error("[{}] 데이터 저장/전송 실패 ({}/{}): {}", category, i + 1, newItems.size(), item.getUrl(), e);
                    failCount++;
                }
            }
            
            totalSuccess.addAndGet(successCount);
            totalFail.addAndGet(failCount);
            
            log.info("[{}] 처리 완료 - 성공: {}개, 실패: {}개", category, successCount, failCount);
            
        } catch (Exception e) {
            log.error("[{}] RSS 피드 처리 중 오류 발생", category, e);
            totalFail.incrementAndGet();
        }
    }
    
    private List<CrawledData> parseRssFeedWithEarlyStop(String rssUrl, String category) throws Exception {
        // MinIO에서 마지막 크롤링 데이터 조회
        CrawledData lastCrawledData = getLastCrawledDataFromMinio();
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(URI.create(rssUrl).toURL().openStream());
        
        NodeList items = doc.getElementsByTagName("item");
        List<CrawledData> newItems = new ArrayList<>();
        
        if (lastCrawledData == null) {
            // MinIO에 데이터가 없는 경우: 오늘 날짜 데이터만
            LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
            LocalDateTime todayEnd = todayStart.plusDays(1);
            
            log.info("MinIO에 기존 데이터 없음. 오늘({}) 날짜 데이터만 처리", todayStart.toLocalDate());
            
            for (int i = 0; i < items.getLength(); i++) {
                Node itemNode = items.item(i);
                if (itemNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element itemElement = (Element) itemNode;
                    
                    try {
                        CrawledData crawledData = parseRssItem(itemElement, category);
                        if (crawledData != null) {
                            LocalDateTime publishedAt = crawledData.getPublishedAt();
                            if (publishedAt.isAfter(todayStart) && publishedAt.isBefore(todayEnd)) {
                                newItems.add(crawledData);
                            }
                        }
                    } catch (Exception e) {
                        log.debug("[{}] RSS 아이템 파싱 실패", category, e);
                    }
                }
            }
        } else {
            // MinIO에 데이터가 있는 경우: 마지막부터 역순으로 검사하며 조기 중단
            log.info("MinIO에서 마지막 데이터 발견: 제목={}, 날짜={}", 
                    lastCrawledData.getTitle(), lastCrawledData.getPublishedAt());
            
            // RSS는 마지막이 최신이므로 뒤에서부터 파싱
            for (int i = items.getLength() - 1; i >= 0; i--) {
                Node itemNode = items.item(i);
                if (itemNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element itemElement = (Element) itemNode;
                    
                    try {
                        CrawledData crawledData = parseRssItem(itemElement, category);
                        if (crawledData != null) {
                            // 동일한 뉴스 발견 시 즉시 중단
                            if (isSameNews(crawledData, lastCrawledData)) {
                                log.info("동일한 뉴스 발견: 인덱스={}, 제목={} - RSS 파싱 중단", i, crawledData.getTitle());
                                break;
                            }
                            
                            // 신규 데이터를 앞에 추가 (순서 유지)
                            newItems.add(0, crawledData);
                        }
                    } catch (Exception e) {
                        log.debug("[{}] RSS 아이템 파싱 실패", category, e);
                    }
                }
            }
        }
        
        return newItems;
    }
    
    private CrawledData parseRssItem(Element itemElement, String category) {
        try {
            String title = getElementText(itemElement, "title");
            String link = getElementText(itemElement, "link");
            String description = getElementText(itemElement, "description");
            String contentEncoded = getElementTextWithNamespace(itemElement, "content:encoded");
            String pubDate = getElementText(itemElement, "pubDate");
            
            if (title == null || link == null) {
                return null;
            }
            
            // content:encoded가 있으면 사용, 없으면 description 사용
            String actualContent = contentEncoded != null && !contentEncoded.trim().isEmpty() 
                    ? contentEncoded 
                    : description;
            
            // 모든 뉴스 소스 허용 (통합 RSS 피드이므로)
            
            // 메타데이터 구성
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("category", category);
            metadata.put("source", "naver_rss");
            metadata.put("originalPubDate", pubDate);
            metadata.put("hasContentEncoded", contentEncoded != null);
            
            // 발행일 파싱
            LocalDateTime publishedAt = parsePublishedDate(pubDate);
            
            return CrawledData.builder()
                    .source("NAVER_RSS")
                    .url(link)
                    .title(cleanHtml(title))
                    .content(cleanHtml(actualContent))
                    .crawledAt(LocalDateTime.now())
                    .publishedAt(publishedAt)
                    .metadata(metadata)
                    .build();
                    
        } catch (Exception e) {
            log.debug("RSS 아이템 파싱 중 오류", e);
            return null;
        }
    }
    
    private String getElementText(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            Node node = nodeList.item(0);
            return node.getTextContent();
        }
        return null;
    }
    
    private String getElementTextWithNamespace(Element parent, String tagName) {
        // content:encoded와 같은 네임스페이스 태그 처리
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            Node node = nodeList.item(0);
            return node.getTextContent();
        }
        
        // encoded 태그만으로도 시도
        if (tagName.contains(":")) {
            String localName = tagName.split(":")[1];
            nodeList = parent.getElementsByTagName(localName);
            if (nodeList.getLength() > 0) {
                Node node = nodeList.item(0);
                return node.getTextContent();
            }
        }
        
        return null;
    }
    
    private LocalDateTime parsePublishedDate(String pubDate) {
        if (pubDate == null) {
            return LocalDateTime.now();
        }
        
        try {
            // RFC 2822 형식 파싱 (예: "Mon, 02 Sep 2025 20:30:00 +0900")
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(pubDate, DateTimeFormatter.RFC_1123_DATE_TIME);
            return zonedDateTime.toLocalDateTime();
        } catch (Exception e1) {
            try {
                // ISO 8601 형식 파싱 (예: "2019-05-27T11:16:00.000Z")
                ZonedDateTime zonedDateTime = ZonedDateTime.parse(pubDate, DateTimeFormatter.ISO_ZONED_DATE_TIME);
                return zonedDateTime.toLocalDateTime();
            } catch (Exception e2) {
                try {
                    // ISO_INSTANT 형식 파싱
                    ZonedDateTime zonedDateTime = ZonedDateTime.parse(pubDate.replace("Z", "+00:00"));
                    return zonedDateTime.toLocalDateTime();
                } catch (Exception e3) {
                    log.debug("발행일 파싱 실패: {} (RFC 2822: {}, ISO 8601: {}, ISO_INSTANT: {})", 
                            pubDate, e1.getMessage(), e2.getMessage(), e3.getMessage());
                    return LocalDateTime.now();
                }
            }
        }
    }
    
    private String cleanHtml(String text) {
        if (text == null) {
            return null;
        }
        
        // 간단한 HTML 태그 제거
        return text.replaceAll("<[^>]*>", "")
                  .replaceAll("&lt;", "<")
                  .replaceAll("&gt;", ">")
                  .replaceAll("&amp;", "&")
                  .replaceAll("&quot;", "\"")
                  .replaceAll("&nbsp;", " ")
                  .trim();
    }
    
    public void clearProcessedUrls() {
        processedUrls.clear();
        log.info("처리된 URL 캐시를 초기화했습니다.");
    }
    
    private DataReference storeToMinio(CrawledData crawledData, String category, int order) throws Exception {
        // 1. 고유 ID 생성 (URL 기반 해시)
        String id = generateIdFromUrl(crawledData.getUrl());
        
        // 2. 객체 키 생성 - 순서 포함 (날짜/카테고리/순서_ID 구조)
        String objectKey = String.format("news/%s/%s/%05d_%s.json", 
                LocalDateTime.now().toLocalDate(), 
                category, 
                order,
                id);
        
        // 3. JSON으로 직렬화
        String jsonContent = objectMapper.writeValueAsString(crawledData);
        byte[] contentBytes = jsonContent.getBytes(StandardCharsets.UTF_8);
        
        // 4. MinIO에 저장
        String bucketName = minioStorageService.getNaverNewsBucket();
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(contentBytes)) {
            minioStorageService.uploadFile(
                    bucketName, 
                    objectKey, 
                    inputStream, 
                    contentBytes.length, 
                    "application/json"
            );
        }
        
        // 5. DataReference 객체 생성
        return DataReference.builder()
                .id(id)
                .source("NAVER_RSS")
                .bucket(bucketName)
                .objectKey(objectKey)
                .url(crawledData.getUrl())
                .title(crawledData.getTitle())
                .crawledAt(crawledData.getCrawledAt())
                .publishedAt(crawledData.getPublishedAt())
                .metadata(crawledData.getMetadata())
                .contentSize(contentBytes.length)
                .build();
    }
    
    private String crawlFullContentFromLink(String url) {
        try {
            log.debug("전체 컨텐츠 크롤링 시작: {}", url);
            
            // Jsoup으로 웹페이지 크롤링 (타임아웃 단축)
            org.jsoup.nodes.Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(3000) // 10초 → 3초로 단축
                    .get();
            
            // 뉴스 본문 추출 (다양한 뉴스 사이트 대응)
            String content = extractNewsContent(doc, url);
            
            if (content != null && content.length() > 100) { // 최소 길이 검증
                log.debug("전체 컨텐츠 크롤링 성공: {}자", content.length());
                return content;
            }
            
        } catch (Exception e) {
            log.debug("전체 컨텐츠 크롤링 실패: {}, 에러: {}", url, e.getMessage());
        }
        
        return null;
    }
    
    private String extractNewsContent(org.jsoup.nodes.Document doc, String url) {
        // 다양한 뉴스 사이트의 본문 선택자들
        String[] contentSelectors = {
            // 네이버 뉴스
            "#dic_area", 
            ".go_trans._article_content",
            // 일반적인 뉴스 사이트
            "article", 
            ".article-content", 
            ".news-content",
            ".content",
            ".post-content",
            ".entry-content",
            "main",
            // 메타 태그에서 추출
            "[property=og:description]",
            "[name=description]"
        };
        
        for (String selector : contentSelectors) {
            org.jsoup.select.Elements elements = doc.select(selector);
            if (!elements.isEmpty()) {
                String text = elements.first().text();
                if (text.length() > 100) { // 최소 길이 검증
                    return text.trim();
                }
            }
        }
        
        // 마지막 대안: body에서 추출
        return doc.body().text().trim();
    }
    
    private List<CrawledData> filterNewItems(List<CrawledData> newsItems) {
        // MinIO에서 마지막 크롤링 데이터 조회
        CrawledData lastCrawledData = getLastCrawledDataFromMinio();
        
        if (lastCrawledData == null) {
            // MinIO에 데이터가 없는 경우: 오늘 날짜 데이터만
            LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
            LocalDateTime todayEnd = todayStart.plusDays(1);
            
            log.info("MinIO에 기존 데이터 없음. 오늘({}) 날짜 데이터만 처리", todayStart.toLocalDate());
            
            return newsItems.stream()
                    .filter(item -> {
                        LocalDateTime publishedAt = item.getPublishedAt();
                        return publishedAt.isAfter(todayStart) && publishedAt.isBefore(todayEnd);
                    })
                    .toList();
        } else {
            // MinIO에 데이터가 존재하는 경우: 마지막부터 역순으로 검사
            log.info("MinIO에서 마지막 데이터 발견: 제목={}, 날짜={}", 
                    lastCrawledData.getTitle(), lastCrawledData.getPublishedAt());
            
            // RSS는 마지막이 최신이므로 뒤에서부터 검사
            List<CrawledData> newItems = new ArrayList<>();
            
            for (int i = newsItems.size() - 1; i >= 0; i--) {
                CrawledData item = newsItems.get(i);
                
                // 동일한 뉴스 발견 시 중단
                if (isSameNews(item, lastCrawledData)) {
                    log.info("동일한 뉴스 발견: 인덱스={}, 제목={} - 여기서 중단", i, item.getTitle());
                    break;
                }
                
                // 신규 데이터를 앞에 추가 (순서 유지)
                newItems.add(0, item);
            }
            
            log.info("신규 데이터 {}개 발견", newItems.size());
            return newItems;
        }
    }
    
    private CrawledData getLastCrawledDataFromMinio() {
        try {
            String bucketName = minioStorageService.getNaverNewsBucket();
            LocalDateTime now = LocalDateTime.now();
            
            log.info("MinIO에서 마지막 크롤링 데이터 조회 시작");
            
            // 최근 3일간의 데이터 확인 (오늘, 어제, 그제)
            for (int i = 0; i < 3; i++) {
                String dateStr = now.minusDays(i).toLocalDate().toString();
                String prefix = "news/" + dateStr + "/통합뉴스/";
                
                log.info("MinIO 경로 확인: {}", prefix);
                
                try {
                    List<String> objectKeys = minioStorageService.listObjects(bucketName, prefix);
                    log.info("MinIO에서 {} 경로에서 {}개 파일 발견", prefix, objectKeys.size());
                    
                    if (objectKeys.isEmpty()) {
                        continue;
                    }
                    
                    // 순서 번호가 가장 큰 파일 찾기 (파일명: 00XXX_ID.json)
                    log.info("파일 목록에서 순서 패턴 확인 중... (총 {}개 파일)", objectKeys.size());
                    
                    String lastObjectKey = objectKeys.stream()
                            .filter(key -> key.matches(".*/(\\d{5}_\\d+\\.json)$"))
                            .max(Comparator.comparing(key -> {
                                String fileName = key.substring(key.lastIndexOf('/') + 1);
                                String orderStr = fileName.split("_")[0];
                                return Integer.parseInt(orderStr);
                            }))
                            .orElse(null);
                    
                    log.info("선택된 마지막 파일: {}", lastObjectKey);
                    
                    if (lastObjectKey != null) {
                        String jsonContent = null;
                        try {
                            jsonContent = minioStorageService.getFileAsString(bucketName, lastObjectKey);
                            log.debug("읽은 JSON 내용 (처음 200자): {}", 
                                    jsonContent.length() > 200 ? jsonContent.substring(0, 200) + "..." : jsonContent);

                            if (jsonContent.trim().isEmpty()) {
                                log.warn("파일이 비어있음: {}", lastObjectKey);
                                continue;
                            }
                            
                            CrawledData lastData = objectMapper.readValue(jsonContent, CrawledData.class);
                            
                            if (lastData == null) {
                                log.warn("CrawledData 객체가 null로 파싱됨: {}", lastObjectKey);
                                continue;
                            }
                            
                            log.info("MinIO에서 마지막 데이터 발견: 파일={}, 제목={}, URL={}", 
                                    lastObjectKey, lastData.getTitle(), lastData.getUrl());
                            return lastData;
                            
                        } catch (JsonProcessingException e) {
                            log.error("JSON 파싱 실패 ({}): {}", lastObjectKey, e.getMessage());
                            if (jsonContent != null) {
                                log.debug("파싱 실패한 JSON: {}", jsonContent);
                            }
                        } catch (Exception e) {
                            log.error("마지막 파일 읽기 실패 ({}): {}", lastObjectKey, e.getMessage(), e);
                        }
                    }
                    
                } catch (Exception e) {
                    log.warn("{} 경로 조회 실패: {}", prefix, e.getMessage());
                }
            }
            
            log.info("MinIO에서 기존 데이터를 찾지 못함 - 첫 실행으로 간주");
            return null;
            
        } catch (Exception e) {
            log.error("MinIO에서 마지막 크롤링 데이터 조회 실패: {}", e.getMessage());
            return null;
        }
    }
    
    private boolean isSameNews(CrawledData news1, CrawledData news2) {
        if (news1 == null || news2 == null) {
            return false;
        }
        
        // URL 기반 비교 - 가장 확실한 방법
        return Objects.equals(news1.getUrl(), news2.getUrl());
    }
    
    private String generateIdFromUrl(String url) {
        return String.valueOf(url.hashCode() & 0x7FFFFFFF); // 양수 보장
    }

    public int getProcessedUrlsCount() {
        return processedUrls.size();
    }
}