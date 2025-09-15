package com.javacafe.realtime_sujeong.collector.crawlerdoc;

import com.javacafe.realtime_sujeong.collector.common.CrawledData;
import com.javacafe.realtime_sujeong.collector.common.KafkaProducerService;
import com.javacafe.realtime_sujeong.collector.dto.DataReference;
import com.javacafe.realtime_sujeong.collector.service.MinioStorageService;
import com.javacafe.realtime_sujeong.collector.service.WikimediaFileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class WikimediaMinioService {

    private final WikimediaFileService wikimediaFileService;
    private final KafkaProducerService kafkaProducerService;
    private final MinioStorageService minioStorageService;
    private final ObjectMapper objectMapper;

    public void processWikimediaData() {
        try {
            log.info("Wikimedia 데이터 처리 시작");

            // 1. 파일 다운로드 및 MinIO 저장
            String objectName = wikimediaFileService.downloadAndStoreWikimediaDump();
            
            // 2. MinIO에서 파일 읽기 및 파싱
            processWikimediaFile(objectName);

        } catch (Exception e) {
            log.error("Wikimedia 데이터 처리 중 오류 발생", e);
        }
    }

    private void processWikimediaFile(String objectName) throws Exception {
        log.info("Wikimedia 파일 처리 시작: {}", objectName);
        
        AtomicInteger processedCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicInteger nullDataCount = new AtomicInteger(0);
        AtomicInteger filteredCount = new AtomicInteger(0);

        try (InputStream fileStream = wikimediaFileService.getFileFromMinio(objectName);
             BZip2CompressorInputStream bzip2Stream = new BZip2CompressorInputStream(
                     new BufferedInputStream(fileStream))) {

            XMLInputFactory factory = XMLInputFactory.newInstance();
            // XML 파싱 시 인코딩 문제 해결을 위한 설정
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            
            XMLStreamReader reader = factory.createXMLStreamReader(bzip2Stream, "UTF-8");

            StringBuilder pageContent = new StringBuilder();
            boolean inPage = false;
            String currentElement = "";

            while (reader.hasNext()) {
                int event = reader.next();

                switch (event) {
                    case XMLStreamConstants.START_ELEMENT:
                        currentElement = reader.getLocalName();
                        if ("page".equals(currentElement)) {
                            inPage = true;
                            pageContent.setLength(0);
                        }
                        if (inPage) {
                            appendStartElement(pageContent, reader);
                        }
                        break;

                    case XMLStreamConstants.CHARACTERS:
                        if (inPage) {
                            try {
                                String text = reader.getText();
                                if (text != null) {
                                    // XML 엔티티 이스케이프 처리 및 유효하지 않은 문자 제거
                                    text = escapeXmlEntities(cleanInvalidXmlCharacters(text));
                                    pageContent.append(text);
                                }
                            } catch (Exception e) {
                                log.debug("XML 문자 처리 중 오류 건너뛰기: {}", e.getMessage());
                            }
                        }
                        break;

                    case XMLStreamConstants.END_ELEMENT:
                        currentElement = reader.getLocalName();
                        if (inPage) {
                            pageContent.append("</").append(currentElement).append(">");
                        }
                        
                        if ("page".equals(currentElement)) {
                            inPage = false;
                            processPage(pageContent.toString(), processedCount, successCount, errorCount, 
                                       nullDataCount, filteredCount);
                            
                            // 진행률 로그
                            if (processedCount.get() % 1000 == 0) {
                                log.info("처리 진행률: {}개 페이지 처리 완료 (성공: {}, 에러: {}, NULL데이터: {}, 필터됨: {})", 
                                        processedCount.get(), successCount.get(), errorCount.get(), 
                                        nullDataCount.get(), filteredCount.get());
                            }
                        }
                        break;
                }
            }

            log.info("Wikimedia 파일 처리 완료 - 총 {}개 페이지 (성공: {}, 에러: {}, NULL데이터: {}, 필터됨: {})", 
                    processedCount.get(), successCount.get(), errorCount.get(), 
                    nullDataCount.get(), filteredCount.get());

        } catch (Exception e) {
            log.error("Wikimedia 파일 처리 중 오류", e);
            throw e;
        }
    }

    private void appendStartElement(StringBuilder content, XMLStreamReader reader) {
        content.append("<").append(reader.getLocalName());
        
        // 속성 추가
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            content.append(" ")
                   .append(reader.getAttributeLocalName(i))
                   .append("=\"")
                   .append(reader.getAttributeValue(i))
                   .append("\"");
        }
        content.append(">");
    }

    private void processPage(String pageXml, AtomicInteger processedCount, 
                           AtomicInteger successCount, AtomicInteger errorCount,
                           AtomicInteger nullDataCount, AtomicInteger filteredCount) {
        try {
            processedCount.incrementAndGet();
            
            // XML 파싱 시 인코딩 안전성 향상
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setValidating(false);
            factory.setFeature("http://xml.org/sax/features/namespaces", false);
            factory.setFeature("http://xml.org/sax/features/validation", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            
            DocumentBuilder builder = factory.newDocumentBuilder();
            
            // UTF-8 바이트로 안전하게 변환
            byte[] xmlBytes = pageXml.getBytes(StandardCharsets.UTF_8);
            Document doc = builder.parse(new ByteArrayInputStream(xmlBytes));

            Element pageElement = doc.getDocumentElement();
            
            String title = getElementText(pageElement, "title");
            String text = getElementText(pageElement, "revision/text");
            String id = getElementText(pageElement, "id");

            if (title == null || text == null) {
                nullDataCount.incrementAndGet();
                return;
            }

            // 리다이렉트나 특별 페이지 필터링
            if (isRedirectOrSpecialPage(title, text)) {
                filteredCount.incrementAndGet();
                return;
            }

            // CrawledData 생성
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("pageId", id);
            metadata.put("source", "wikimedia_minio");
            metadata.put("language", "ko");

            CrawledData crawledData = CrawledData.builder()
                    .source("WIKIMEDIA_MINIO")
                    .url("https://ko.wikipedia.org/wiki/" + title.replaceAll(" ", "_"))
                    .title(title)
                    .content(cleanWikiText(text))
                    .crawledAt(LocalDateTime.now())
                    .publishedAt(LocalDateTime.now())
                    .metadata(metadata)
                    .build();

            // 1. MinIO에 개별 페이지 저장 (네이버 뉴스와 동일한 패턴)
            DataReference dataRef = storeWikiPageToMinio(crawledData, id);
            
            // 2. 메타데이터만 Kafka로 전송
            kafkaProducerService.send("wiki-data", dataRef);
            successCount.incrementAndGet();

        } catch (Exception e) {
            log.debug("페이지 처리 중 오류", e);
            errorCount.incrementAndGet();
        }
    }

    private String getElementText(Element parent, String path) {
        try {
            String[] parts = path.split("/");
            Element current = parent;
            
            for (String part : parts) {
                NodeList nodeList = current.getElementsByTagName(part);
                if (nodeList.getLength() == 0) {
                    return null;
                }
                Node node = nodeList.item(0);
                if (node instanceof Element) {
                    current = (Element) node;
                } else {
                    return node.getTextContent();
                }
            }
            
            return current.getTextContent();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isRedirectOrSpecialPage(String title, String text) {
        return title.startsWith("Wikipedia:") ||
               title.startsWith("파일:") ||
               title.startsWith("틀:") ||
               title.startsWith("분류:") ||
               text.toLowerCase().startsWith("#redirect") ||
               text.toLowerCase().startsWith("#넘겨주기");
    }

    private DataReference storeWikiPageToMinio(CrawledData crawledData, String pageId) throws Exception {
        // 1. 객체 키 생성 (날짜/wiki/pageId 구조)
        String objectKey = String.format("wiki/%s/page_%s.json", 
                LocalDateTime.now().toLocalDate(), 
                pageId);
        
        // 2. JSON으로 직렬화
        String jsonContent = objectMapper.writeValueAsString(crawledData);
        byte[] contentBytes = jsonContent.getBytes(StandardCharsets.UTF_8);
        
        // 3. MinIO에 저장
        String bucketName = minioStorageService.getWikimediaBucket();
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(contentBytes)) {
            minioStorageService.uploadFile(
                    bucketName, 
                    objectKey, 
                    inputStream, 
                    contentBytes.length, 
                    "application/json"
            );
        }
        
        // 4. DataReference 객체 생성
        return DataReference.builder()
                .id(pageId)
                .source("WIKIMEDIA_MINIO")
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

    private String cleanInvalidXmlCharacters(String text) {
        if (text == null) {
            return null;
        }
        
        // XML 1.0 유효 문자만 유지 (UTF-8 호환)
        // 유효한 문자: #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]
        StringBuilder cleaned = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (isValidXmlCharacter(ch)) {
                cleaned.append(ch);
            }
        }
        return cleaned.toString();
    }
    
    private boolean isValidXmlCharacter(char ch) {
        return (ch == 0x09) || (ch == 0x0A) || (ch == 0x0D) ||
               (ch >= 0x20 && ch <= 0xD7FF) ||
               (ch >= 0xE000 && ch <= 0xFFFD);
    }
    
    private String escapeXmlEntities(String text) {
        if (text == null) {
            return null;
        }
        
        // XML 엔티티 이스케이프 처리
        return text.replace("&", "&amp;")   // & 먼저 처리해야 함
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }

    private String cleanWikiText(String text) {
        if (text == null) {
            return null;
        }

        // 위키 문법 정리
        return text.replaceAll("\\{\\{[^}]+\\}\\}", "") // 템플릿 제거
                  .replaceAll("\\[\\[[^|\\]]+\\|([^\\]]+)\\]\\]", "$1") // 링크 텍스트만 유지
                  .replaceAll("\\[\\[([^\\]]+)\\]\\]", "$1") // 내부 링크 정리
                  .replaceAll("\\[http[^\\s]+ ([^\\]]+)\\]", "$1") // 외부 링크 정리
                  .replaceAll("'{2,}", "") // 굵기/기울임 제거
                  .replaceAll("={2,}[^=]+={2,}", "") // 섹션 헤더 제거
                  .replaceAll("\\n{3,}", "\n\n") // 연속 개행 정리
                  .trim();
    }
}