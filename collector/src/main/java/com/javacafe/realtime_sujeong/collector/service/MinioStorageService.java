package com.javacafe.realtime_sujeong.collector.service;

import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket.wikimedia}")
    private String wikimediaBucket;
    
    @Value("${minio.bucket.naver-news}")
    private String naverNewsBucket;

    public void ensureBucketExists(String bucketName) throws Exception {
        boolean bucketExists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucketName).build()
        );

        if (!bucketExists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket(bucketName).build()
            );
            log.info("MinIO 버킷 생성 완료: {}", bucketName);
        } else {
            log.info("MinIO 버킷 이미 존재: {}", bucketName);
        }
    }

    public void uploadFile(String bucketName, String objectName, InputStream inputStream, long size, String contentType) throws Exception {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(inputStream, size, -1)
                        .contentType(contentType)
                        .build()
        );
        log.info("파일 업로드 완료: bucket={}, object={}, size={}MB", bucketName, objectName, size / (1024 * 1024));
    }

    public InputStream downloadFile(String bucketName, String objectName) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
    }

    public boolean fileExists(String bucketName, String objectName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getWikimediaBucket() {
        return wikimediaBucket;
    }
    
    public String getNaverNewsBucket() {
        return naverNewsBucket;
    }
    
    public List<String> listObjects(String bucketName, String prefix) throws Exception {
        List<String> objectKeys = new ArrayList<>();
        
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(prefix)
                        .build()
        );
        
        for (Result<Item> result : results) {
            Item item = result.get();
            objectKeys.add(item.objectName());
        }
        
        return objectKeys;
    }
    
    public String getFileAsString(String bucketName, String objectKey) throws Exception {
        try (InputStream inputStream = downloadFile(bucketName, objectKey)) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}