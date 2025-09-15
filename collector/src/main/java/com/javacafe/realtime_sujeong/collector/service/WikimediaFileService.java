package com.javacafe.realtime_sujeong.collector.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class WikimediaFileService {

    private final MinioStorageService minioStorageService;

    @Value("${wikimedia.download-dir}")
    private String downloadDir;

    @Value("${wikimedia.dump-url}")
    private String dumpUrl;

    public String downloadAndStoreWikimediaDump() throws Exception {
        // 다운로드 디렉토리 생성
        Path downloadPath = Paths.get(downloadDir);
        Files.createDirectories(downloadPath);

        // 파일명 생성 (날짜 포함)
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = String.format("kowiki-dump-%s.xml.bz2", timestamp);
        Path localFilePath = downloadPath.resolve(fileName);

        log.info("Wikimedia 덤프 다운로드 시작: {} -> {}", dumpUrl, localFilePath);

        // 파일 다운로드
        downloadFile(dumpUrl, localFilePath);

        // MinIO 버킷 확인 및 생성
        String bucketName = minioStorageService.getWikimediaBucket();
        minioStorageService.ensureBucketExists(bucketName);

        // MinIO에 업로드
        String objectName = fileName;
        uploadToMinio(localFilePath, bucketName, objectName);

        // 로컬 파일 삭제 (선택적)
        Files.deleteIfExists(localFilePath);
        log.info("로컬 임시 파일 삭제 완료: {}", localFilePath);

        return objectName;
    }

    private void downloadFile(String fileUrl, Path targetPath) throws IOException {
        log.info("파일 다운로드 시작: {}", fileUrl);
        
        try (InputStream inputStream = new URL(fileUrl).openStream();
             FileOutputStream outputStream = new FileOutputStream(targetPath.toFile())) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytes = 0;
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
                
                // 진행률 로그 (100MB마다)
                if (totalBytes % (100 * 1024 * 1024) == 0) {
                    log.info("다운로드 진행: {}MB", totalBytes / (1024 * 1024));
                }
            }
            
            log.info("파일 다운로드 완료: {} ({}MB)", targetPath, totalBytes / (1024 * 1024));
        }
    }

    private void uploadToMinio(Path filePath, String bucketName, String objectName) throws Exception {
        log.info("MinIO 업로드 시작: {} -> {}/{}", filePath, bucketName, objectName);
        
        try (FileInputStream fileInputStream = new FileInputStream(filePath.toFile())) {
            long fileSize = Files.size(filePath);
            minioStorageService.uploadFile(
                    bucketName, 
                    objectName, 
                    fileInputStream, 
                    fileSize, 
                    "application/x-bzip2"
            );
        }
    }

    public boolean isFileAlreadyInMinio(String objectName) {
        String bucketName = minioStorageService.getWikimediaBucket();
        return minioStorageService.fileExists(bucketName, objectName);
    }

    public InputStream getFileFromMinio(String objectName) throws Exception {
        String bucketName = minioStorageService.getWikimediaBucket();
        return minioStorageService.downloadFile(bucketName, objectName);
    }
}