package javacafe.realtime_sujeong.collection.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * MongoDB 설정
 */
@Configuration
@EnableMongoRepositories(basePackages = "javacafe.realtime_sujeong.collection")
@EnableMongoAuditing
public class MongoConfig {
    // Spring Boot Auto-configuration이 자동으로 MongoClient, MongoTemplate 등을 설정
    // application.properties에서 spring.data.mongodb.* 설정 사용
}
