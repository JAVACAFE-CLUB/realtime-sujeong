package javacafe.realtime_sujeong.indexing.common.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * NER gRPC Client Configuration
 *
 * NER 서버와의 gRPC 통신을 위한 설정
 */
@Slf4j
@Configuration
public class NerClientConfig {

    @Value("${ner.server.host}")
    private String nerServerHost;

    @Value("${ner.server.port}")
    private int nerServerPort;

    @Value("${ner.server.deadline-seconds}")
    private long deadlineSeconds;

    /**
     * gRPC ManagedChannel 생성
     *
     * NER 서버와의 연결을 관리하는 채널 생성
     * - 연결 재사용 (HTTP/2 멀티플렉싱)
     * - 자동 재연결
     */
    @Bean
    public ManagedChannel nerGrpcChannel() {
        log.info("Creating gRPC channel to NER server: {}:{}", nerServerHost, nerServerPort);

        return ManagedChannelBuilder
                .forAddress(nerServerHost, nerServerPort)
                .usePlaintext() // 개발 환경 - TLS 비활성화
                .keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveTimeout(10, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true)
                .maxInboundMessageSize(50 * 1024 * 1024) // 50MB
                .build();
    }

    /**
     * gRPC 호출 기본 Deadline (초)
     */
    @Bean
    public long nerDeadlineSeconds() {
        return deadlineSeconds;
    }
}