package com.javacafe.realtime_sujeong.collector.service;

import com.javacafe.realtime_sujeong.collector.crawlerhtml.NaverRssNewsService;
import com.javacafe.realtime_sujeong.collector.crawlerdoc.WikimediaMinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsCollectionService {

    private final NaverRssNewsService naverRssNewsService;
    private final WikimediaMinioService wikimediaMinioService;

    public void collectRssNews() {
        log.info("RSS 뉴스 수집 작업 시작");
        long startTime = System.currentTimeMillis();

        try {
            naverRssNewsService.collectAllRssNews();
        } catch (Exception e) {
            log.error("RSS 뉴스 수집 중 오류 발생", e);
        } finally {
            long endTime = System.currentTimeMillis();
            log.info("RSS 뉴스 수집 작업 완료 - 소요시간: {}초", (endTime - startTime) / 1000);
        }
    }

    public void collectWikimediaData() {
        log.info("Wikimedia 데이터 수집 작업 시작");
        long startTime = System.currentTimeMillis();

        try {
            wikimediaMinioService.processWikimediaData();
        } catch (Exception e) {
            log.error("Wikimedia 데이터 수집 중 오류 발생", e);
        } finally {
            long endTime = System.currentTimeMillis();
            log.info("Wikimedia 데이터 수집 작업 완료 - 소요시간: {}초", (endTime - startTime) / 1000);
        }
    }

    public void collectAllNews() {
        log.info("전체 데이터 수집 작업 시작");
        long startTime = System.currentTimeMillis();

        try {
            collectRssNews();
            collectWikimediaData();
        } finally {
            long endTime = System.currentTimeMillis();
            log.info("전체 데이터 수집 작업 완료 - 소요시간: {}초", (endTime - startTime) / 1000);
        }
    }

}