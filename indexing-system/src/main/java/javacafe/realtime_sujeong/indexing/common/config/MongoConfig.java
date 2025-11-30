package javacafe.realtime_sujeong.indexing.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * MongoDB Configuration
 *
 * MongoDB Repository 활성화
 */
@Configuration
@EnableMongoRepositories(basePackages = "javacafe.realtime_sujeong.indexing.cleaning.domain")
public class MongoConfig {
}