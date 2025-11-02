package javacafe.realtime_sujeong.cleaning.service.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.language.detect.LanguageResult;
import org.springframework.stereotype.Component;

/**
 * 텍스트 언어 감지 Processor
 * Apache Tika의 언어 감지 기능 사용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LanguageDetector {

    private final org.apache.tika.language.detect.LanguageDetector tikaLanguageDetector;

    // 언어 감지 신뢰도 임계값 (0.0 ~ 1.0)
    private static final double CONFIDENCE_THRESHOLD = 0.5;

    // 최소 텍스트 길이 (언어 감지에 필요한 최소 문자 수)
    private static final int MIN_TEXT_LENGTH = 20;

    // 기본 언어 (감지 실패 시 fallback)
    private static final String DEFAULT_LANGUAGE = "unknown";

    /**
     * 텍스트의 언어 감지
     *
     * @param text 분석할 텍스트
     * @return 언어 코드 (ISO 639-1: ko, en, ja 등) 또는 "unknown"
     */
    public String detect(String text) {
        if (text == null || text.trim().isEmpty()) {
            log.debug("Empty text provided for language detection");
            return DEFAULT_LANGUAGE;
        }

        // 텍스트가 너무 짧으면 감지 불가
        if (text.trim().length() < MIN_TEXT_LENGTH) {
            log.debug("Text too short for language detection: {} chars", text.length());
            return DEFAULT_LANGUAGE;
        }

        try {
            // Tika 언어 감지
            LanguageResult result = tikaLanguageDetector.detect(text);

            if (result == null) {
                log.warn("Language detection returned null result");
                return DEFAULT_LANGUAGE;
            }

            String language = result.getLanguage();
            double confidence = result.getRawScore();

            log.debug("Detected language: {} (confidence: {})", language, confidence);

            // 신뢰도가 임계값 이상이면 감지된 언어 반환
            if (confidence >= CONFIDENCE_THRESHOLD) {
                return language;
            } else {
                log.debug("Low confidence language detection: {} ({}), returning default",
                        language, confidence);
                return DEFAULT_LANGUAGE;
            }

        } catch (Exception e) {
            log.error("Error during language detection", e);
            return DEFAULT_LANGUAGE;
        }
    }

    /**
     * 언어가 지원되는 언어인지 확인
     *
     * @param language 언어 코드
     * @return 지원 여부
     */
    public boolean isSupported(String language) {
        if (language == null || language.isEmpty()) {
            return false;
        }

        // 주요 지원 언어: 한국어, 영어, 일본어, 중국어, 스페인어, 프랑스어, 독일어
        return language.matches("ko|en|ja|zh|es|fr|de");
    }

    /**
     * 언어 코드를 언어 이름으로 변환 (로깅/디버깅용)
     *
     * @param languageCode 언어 코드 (ISO 639-1)
     * @return 언어 이름
     */
    public String getLanguageName(String languageCode) {
        if (languageCode == null) {
            return "Unknown";
        }

        return switch (languageCode) {
            case "ko" -> "Korean";
            case "en" -> "English";
            case "ja" -> "Japanese";
            case "zh" -> "Chinese";
            case "es" -> "Spanish";
            case "fr" -> "French";
            case "de" -> "German";
            default -> "Unknown (" + languageCode + ")";
        };
    }
}