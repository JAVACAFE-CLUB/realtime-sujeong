package javacafe.realtime_sujeong.indexing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Indexing System Application
 *
 * 키워드 색인 및 랭킹 시스템
 * - Cleaning System에서 정제된 데이터를 받아 NER을 통해 개체명 추출
 * - 빈도수 기반 가중치 계산 후 Elasticsearch에 색인
 * - Redis를 통한 Top 10 키워드 캐시
 */
@SpringBootApplication
@EnableKafka
@EnableAsync
@EnableScheduling
@EnableElasticsearchRepositories(basePackages = "javacafe.realtime_sujeong.indexing.keyword.domain")
public class IndexingSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(IndexingSystemApplication.class, args);
    }
}
