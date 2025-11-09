package javacafe.realtime_sujeong.cleaning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Cleaning System Application
 *
 * 수집된 원본 데이터를 정제하여 Indexing System으로 전달하는 시스템
 * - Kafka Consumer: collection-to-cleaning 토픽 구독
 * - 데이터 정제: 텍스트 정규화, 언어 감지
 * - MongoDB 저장: cleaned_data 컬렉션
 * - Kafka Producer: cleaning-to-indexing 토픽 발행
 */
@EnableKafka
@SpringBootApplication
public class CleaningSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(CleaningSystemApplication.class, args);
    }
}