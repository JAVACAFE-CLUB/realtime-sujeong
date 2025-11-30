package javacafe.realtime_sujeong.indexing.ner.service;

import javacafe.realtime_sujeong.indexing.ner.client.NerGrpcClient;
import javacafe.realtime_sujeong.indexing.ner.dto.NamedEntity;
import javacafe.realtime_sujeong.indexing.ner.dto.NerResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * NER Service
 *
 * NER 클라이언트를 래핑하여 비즈니스 로직 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NerService {

    private final NerGrpcClient nerGrpcClient;

    /**
     * 텍스트에서 개체명 추출
     *
     * @param text 분석할 텍스트
     * @return 추출된 개체명 리스트
     */
    public List<NamedEntity> extractEntities(String text) {
        if (text == null || text.trim().isEmpty()) {
            log.warn("Empty text provided for NER");
            return Collections.emptyList();
        }

        try {
            NerResult result = nerGrpcClient.extractEntities(text);
            return result.getEntities();
        } catch (Exception e) {
            log.error("Failed to extract entities from text: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 제목과 본문에서 개체명 추출 (제목 개체명 별도 표시)
     *
     * @param title   제목
     * @param content 본문
     * @return 제목/본문 개체명 맵
     */
    public Map<String, List<NamedEntity>> extractEntitiesFromTitleAndContent(String title, String content) {
        List<NamedEntity> titleEntities = Collections.emptyList();
        List<NamedEntity> contentEntities = Collections.emptyList();

        if (title != null && !title.trim().isEmpty()) {
            titleEntities = extractEntities(title);
        }

        if (content != null && !content.trim().isEmpty()) {
            contentEntities = extractEntities(content);
        }

        return Map.of(
                "title", titleEntities,
                "content", contentEntities
        );
    }

    /**
     * 배치 텍스트에서 개체명 추출
     *
     * @param texts 분석할 텍스트 리스트
     * @return NER 결과 리스트
     */
    public List<NerResult> extractEntitiesBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            log.warn("Empty text list provided for batch NER");
            return Collections.emptyList();
        }

        // 빈 텍스트 제거
        List<String> validTexts = texts.stream()
                .filter(text -> text != null && !text.trim().isEmpty())
                .collect(Collectors.toList());

        if (validTexts.isEmpty()) {
            log.warn("No valid texts after filtering");
            return Collections.emptyList();
        }

        try {
            return nerGrpcClient.extractEntitiesBatch(validTexts);
        } catch (Exception e) {
            log.error("Failed to extract entities from batch: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 개체명을 유형별로 그룹화
     *
     * @param entities 개체명 리스트
     * @return 유형별 개체명 맵
     */
    public Map<String, List<NamedEntity>> groupEntitiesByType(List<NamedEntity> entities) {
        return entities.stream()
                .collect(Collectors.groupingBy(NamedEntity::getEntityType));
    }

    /**
     * 특정 유형의 개체명만 필터링
     *
     * @param entities    개체명 리스트
     * @param entityTypes 필터링할 유형 리스트 (예: ["PER", "ORG"])
     * @return 필터링된 개체명 리스트
     */
    public List<NamedEntity> filterEntitiesByType(List<NamedEntity> entities, List<String> entityTypes) {
        return entities.stream()
                .filter(entity -> entityTypes.contains(entity.getEntityType()))
                .collect(Collectors.toList());
    }
}