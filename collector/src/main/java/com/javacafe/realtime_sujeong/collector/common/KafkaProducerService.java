package com.javacafe.realtime_sujeong.collector.common;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void send(String topic, Object data) {
        kafkaTemplate.send(topic, data);
    }
    
    // 호환성을 위한 기존 메소드 유지
    public void send(String topic, CrawledData data) {
        kafkaTemplate.send(topic, data);
    }
}
