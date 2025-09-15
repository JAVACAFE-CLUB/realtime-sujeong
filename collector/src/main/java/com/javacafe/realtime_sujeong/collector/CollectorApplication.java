package com.javacafe.realtime_sujeong.collector;

import com.javacafe.realtime_sujeong.collector.service.NewsCollectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@SpringBootApplication(scanBasePackages = {"com.javacafe.realtime_sujeong.collector", "com.javacafe.realtime_sujeong.common"})
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class CollectorApplication {

    private final NewsCollectionService newsCollectionService;

    // RSS 뉴스는 자주 수집 (테스트용 5분마다, 운영시 30분마다)
    @Scheduled(fixedRate = 60_000 * 24 * 60)  // 24시간마다 실행
    public void collectRssNews() {
        log.info("RSS 뉴스 수집 시작");
        newsCollectionService.collectRssNews();
    }

    // Wikimedia는 일 1회 수집 (대용량 파일)
//    @Scheduled(cron = "0 0 2 * * *")  // 매일 새벽 2시에 실행
    @Scheduled(fixedRate = 60_000 * 60 * 24)  // 24시간마다 실행
    public void collectWikimediaData() {
        log.info("Wikimedia 데이터 수집 시작");
        newsCollectionService.collectWikimediaData();
    }

	public static void main(String[] args) {
		SpringApplication.run(CollectorApplication.class, args);
	}

}