package javacafe.realtime_sujeong.collection.common.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Spring Batch 설정
 * Wiki dump 수집을 위한 배치 처리 인프라 구성
 *
 * DefaultBatchConfiguration을 상속하여 Spring Batch 인프라를 설정합니다.
 * H2 인메모리 데이터베이스를 사용하여 배치 메타데이터를 저장합니다.
 */
@Configuration
@EnableBatchProcessing
public class BatchConfig extends DefaultBatchConfiguration {

    private final DataSource batchDataSource = DataSourceBuilder.create()
            .driverClassName("org.h2.Driver")
            .url("jdbc:h2:mem:batch")
            .username("sa")
            .password("")
            .build();

    /**
     * Spring Batch가 사용할 DataSource 제공
     */
    @Override
    protected DataSource getDataSource() {
        return batchDataSource;
    }

    /**
     * Spring Batch용 TransactionManager
     * 빈으로 등록하여 다른 컴포넌트에서 주입받을 수 있도록 함
     */
    @Bean
    @Override
    public PlatformTransactionManager getTransactionManager() {
        return new JdbcTransactionManager(batchDataSource);
    }

    /**
     * Spring Batch 스키마 초기화
     * @EnableBatchProcessing 사용 시 자동 초기화가 안되므로 수동으로 설정
     */
    @Bean
    public DataSourceInitializer dataSourceInitializer() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("org/springframework/batch/core/schema-h2.sql"));
        populator.setContinueOnError(false);

        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(batchDataSource);
        initializer.setDatabasePopulator(populator);
        return initializer;
    }
}