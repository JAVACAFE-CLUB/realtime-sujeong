package javacafe.realtime_sujeong.indexing.ner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * NER Result DTO
 *
 * NER 서버로부터 받은 전체 결과를 담는 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NerResult {

    /**
     * 추출된 개체명 리스트
     */
    private List<NamedEntity> entities;

    /**
     * 처리 시간 (밀리초)
     */
    private int processingTimeMs;

    /**
     * 원본 텍스트 (배치 처리 시 사용)
     */
    private String originalText;
}