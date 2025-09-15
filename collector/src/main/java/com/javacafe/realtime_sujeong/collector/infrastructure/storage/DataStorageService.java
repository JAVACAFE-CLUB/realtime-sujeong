package com.javacafe.realtime_sujeong.collector.infrastructure.storage;

import com.javacafe.realtime_sujeong.collector.common.CrawledData;
import com.javacafe.realtime_sujeong.collector.dto.DataReference;
import com.javacafe.realtime_sujeong.collector.service.MinioStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataStorageService {

    private final MinioStorageService minioStorageService;
    private final ObjectMapper objectMapper;

    public DataReference storeNewsData(CrawledData crawledData, String category, int order) throws Exception {
        String id = generateIdFromUrl(crawledData.getUrl());
        String objectKey = String.format("news/%s/%s/%05d_%s.json", 
                LocalDateTime.now().toLocalDate(), 
                category, 
                order,
                id);

        return storeData(crawledData, id, objectKey, minioStorageService.getNaverNewsBucket());
    }

    public DataReference storeWikiData(CrawledData crawledData, String pageId) throws Exception {
        String objectKey = String.format("wiki/%s/page_%s.json", 
                LocalDateTime.now().toLocalDate(), 
                pageId);

        return storeData(crawledData, pageId, objectKey, minioStorageService.getWikimediaBucket());
    }

    private DataReference storeData(CrawledData crawledData, String id, String objectKey, String bucketName) throws Exception {
        String jsonContent = objectMapper.writeValueAsString(crawledData);
        byte[] contentBytes = jsonContent.getBytes(StandardCharsets.UTF_8);

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(contentBytes)) {
            minioStorageService.uploadFile(
                    bucketName, 
                    objectKey, 
                    inputStream, 
                    contentBytes.length, 
                    "application/json"
            );
        }

        return DataReference.builder()
                .id(id)
                .source(crawledData.getSource())
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

    private String generateIdFromUrl(String url) {
        return String.valueOf(url.hashCode() & 0x7FFFFFFF);
    }
}