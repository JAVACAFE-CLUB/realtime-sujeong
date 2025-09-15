package com.javacafe.realtime_sujeong.collector.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataReference {
    private String id;              // 고유 ID
    private String source;          // NAVER_RSS, WIKIMEDIA_MINIO 등
    private String bucket;          // MinIO 버킷명
    private String objectKey;       // MinIO 객체 키
    private String url;             // 원본 URL
    private String title;           // 제목 (검색용)
    private LocalDateTime crawledAt;
    private LocalDateTime publishedAt;
    private Map<String, Object> metadata;
    private long contentSize;       // 바이트 크기
}