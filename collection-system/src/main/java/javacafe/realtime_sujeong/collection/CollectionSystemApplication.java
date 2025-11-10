package javacafe.realtime_sujeong.collection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * Collection System Application
 * 데이터 수집 시스템 메인 애플리케이션
 *
 * Note: DataSource와 Batch 자동 설정 제외
 * - RSS 수집은 MongoDB만 사용 (관계형 DB 불필요)
 * - Wiki 수집용 Batch는 나중에 별도 설정으로 활성화 예정
 */
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        BatchAutoConfiguration.class
})
public class CollectionSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(CollectionSystemApplication.class, args);
    }
}
