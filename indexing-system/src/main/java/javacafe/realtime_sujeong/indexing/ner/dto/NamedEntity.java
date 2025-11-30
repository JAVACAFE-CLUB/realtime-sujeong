package javacafe.realtime_sujeong.indexing.ner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Named Entity DTO
 *
 * NER 서버로부터 받은 개체명 정보를 담는 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NamedEntity {

    /**
     * 개체명 (예: "삼성전자")
     */
    private String word;

    /**
     * 개체 유형 (PER, ORG, LOC, DAT, TIM, etc.)
     */
    private String entityType;

    /**
     * 신뢰도 (0.0 ~ 1.0)
     */
    private float confidence;

    /**
     * 시작 위치 (문자 인덱스)
     */
    private int start;

    /**
     * 종료 위치 (문자 인덱스)
     */
    private int end;
}