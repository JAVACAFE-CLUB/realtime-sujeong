package com.javacafe.realtime_sujeong.collector.service;

import com.javacafe.realtime_sujeong.collector.domain.rss.RssNewsCollector;
import com.javacafe.realtime_sujeong.collector.domain.wiki.WikiDataCollector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsCollectionService {

    private final RssNewsCollector rssNewsCollector;
    private final WikiDataCollector wikiDataCollector;

    public void collectRssNews() {
        log.info("RSS 뉴스 수집 작업 시작");
        long startTime = System.currentTimeMillis();

        try {
            rssNewsCollector.collectAllRssNews();
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
            wikiDataCollector.processWikimediaData();
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