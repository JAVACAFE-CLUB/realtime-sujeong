package javacafe.realtime_sujeong.indexing.common.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.StringReader;
import java.util.Map;

/**
 * Elasticsearch Index Initializer
 *
 * 애플리케이션 시작 시 Elasticsearch 인덱스를 초기화하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticsearchInitializer {

    private final ElasticsearchClient elasticsearchClient;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    @Value("${elasticsearch.index.keyword:keyword_index}")
    private String keywordIndexName;

    @Value("${elasticsearch.index.auto-create:true}")
    private boolean autoCreateIndex;

    @PostConstruct
    public void initialize() {
        if (!autoCreateIndex) {
            log.info("Elasticsearch index auto-creation is disabled");
            return;
        }

        try {
            createKeywordIndexIfNotExists();
        } catch (Exception e) {
            log.error("Failed to initialize Elasticsearch indices", e);
            throw new RuntimeException("Elasticsearch initialization failed", e);
        }
    }

    /**
     * keyword_index 인덱스 생성
     */
    private void createKeywordIndexIfNotExists() throws Exception {
        boolean exists = elasticsearchClient.indices()
                .exists(ExistsRequest.of(e -> e.index(keywordIndexName)))
                .value();

        if (exists) {
            log.info("Index [{}] already exists", keywordIndexName);
            return;
        }

        log.info("Creating index [{}]...", keywordIndexName);

        // JSON 매핑 파일 읽기
        Resource resource = resourceLoader.getResource("classpath:elasticsearch/keyword-index-mapping.json");
        String mappingJson;

        try (InputStream inputStream = resource.getInputStream()) {
            mappingJson = new String(inputStream.readAllBytes());
        }

        // JSON을 Map으로 변환
        Map<String, Object> mappingMap = objectMapper.readValue(mappingJson, Map.class);

        // 인덱스 생성
        elasticsearchClient.indices().create(CreateIndexRequest.of(builder -> {
            builder.index(keywordIndexName);

            // settings 적용
            if (mappingMap.containsKey("settings")) {
                builder.settings(s -> s.withJson(new StringReader(
                        toJson(mappingMap.get("settings"))
                )));
            }

            // mappings 적용
            if (mappingMap.containsKey("mappings")) {
                builder.mappings(m -> m.withJson(new StringReader(
                        toJson(mappingMap.get("mappings"))
                )));
            }

            return builder;
        }));

        log.info("Successfully created index [{}]", keywordIndexName);
    }

    /**
     * Object를 JSON 문자열로 변환
     */
    private String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert object to JSON", e);
        }
    }
}