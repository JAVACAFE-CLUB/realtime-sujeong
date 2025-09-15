package com.javacafe.realtime_sujeong.collector.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@Getter
public class CrawledData {
    private String source;
    private String url;
    private String title;
    private String content;
    private LocalDateTime crawledAt;
    private LocalDateTime publishedAt;
    private Map<String, Object> metadata;
}
