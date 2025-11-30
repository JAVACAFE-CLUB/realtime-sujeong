package javacafe.realtime_sujeong.indexing.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Elasticsearch Configuration
 *
 * Elasticsearch 클라이언트 설정 및 Repository 활성화
 */
@Slf4j
@Configuration
@EnableElasticsearchRepositories(basePackages = "javacafe.realtime_sujeong.indexing.keyword.domain")
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Value("${elasticsearch.host}")
    private String host;

    @Value("${elasticsearch.port}")
    private int port;

    @Value("${elasticsearch.username:#{null}}")
    private String username;

    @Value("${elasticsearch.password:#{null}}")
    private String password;

    @Value("${elasticsearch.connection-timeout:5000}")
    private long connectionTimeout;

    @Value("${elasticsearch.socket-timeout:60000}")
    private long socketTimeout;

    @Override
    public ClientConfiguration clientConfiguration() {
        ClientConfiguration.MaybeSecureClientConfigurationBuilder builder = ClientConfiguration.builder()
                .connectedTo(host + ":" + port)
                .withConnectTimeout(connectionTimeout)
                .withSocketTimeout(socketTimeout);

        // 인증 정보가 있는 경우 추가
        if (username != null && password != null) {
            builder.withBasicAuth(username, password);
            log.info("Elasticsearch configured with authentication: {}@{}:{}", username, host, port);
        } else {
            log.info("Elasticsearch configured without authentication: {}:{}", host, port);
        }

        return builder.build();
    }
}
