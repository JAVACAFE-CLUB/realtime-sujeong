package javacafe.realtime_sujeong.collection.wiki.batch.job;

import javacafe.realtime_sujeong.collection.wiki.batch.processor.WikiPageProcessor;
import javacafe.realtime_sujeong.collection.wiki.batch.writer.WikiDataWriter;
import javacafe.realtime_sujeong.common.dto.WikiPage;
import javacafe.realtime_sujeong.collection.wiki.domain.WikiRawData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Wiki 수집 배치 Job 설정
 * Reader → Processor → Writer 파이프라인 구성
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WikiCollectionJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final StaxEventItemReader<WikiPage> wikiPageReader;
    private final WikiPageProcessor wikiPageProcessor;
    private final WikiDataWriter wikiDataWriter;

    @Value("${wiki.batch.chunk-size:100}")
    private int chunkSize;

    /**
     * Wiki 수집 Job
     */
    @Bean
    public Job wikiCollectionJob() {
        return new JobBuilder("wikiCollectionJob", jobRepository)
                .start(wikiCollectionStep())
                .build();
    }

    /**
     * Wiki 수집 Step
     * - Reader: Wiki XML 파일에서 WikiPage 읽기 (StAX 스트리밍)
     * - Processor: WikiPage → WikiRawData 변환 및 검증
     * - Writer: MongoDB Bulk Insert + Kafka 메시지 전송
     */
    @Bean
    public Step wikiCollectionStep() {
        return new StepBuilder("wikiCollectionStep", jobRepository)
                .<WikiPage, WikiRawData>chunk(chunkSize, transactionManager)
                .reader(wikiPageReader)
                .processor(wikiPageProcessor)
                .writer(wikiDataWriter)
                .build();
    }
}
