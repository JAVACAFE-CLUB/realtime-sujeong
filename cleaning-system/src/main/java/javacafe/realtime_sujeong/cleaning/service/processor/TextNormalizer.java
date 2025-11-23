package javacafe.realtime_sujeong.cleaning.service.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 텍스트 정규화 Processor
 * HTML 태그, 특수문자, 과도한 공백 제거 등
 */
@Slf4j
@Component
public class TextNormalizer {

    // HTML 태그 제거 패턴
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");

    // 연속된 공백 제거 패턴 (2개 이상의 공백을 1개로)
    private static final Pattern MULTIPLE_SPACES_PATTERN = Pattern.compile("\\s{2,}");

    // 연속된 줄바꿈 제거 패턴 (3개 이상의 줄바꿈을 2개로)
    private static final Pattern MULTIPLE_NEWLINES_PATTERN = Pattern.compile("\\n{3,}");

    // 특수문자 패턴 (일부만 제거, 문장 부호는 유지)
    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]");

    /**
     * 텍스트 정규화
     *
     * @param text 원본 텍스트
     * @return 정규화된 텍스트
     */
    public String normalize(String text) {
        if (text == null || text.isEmpty()) {
            log.debug("Empty text provided for normalization");
            return "";
        }

        String normalized = text;

        try {
            // 1. HTML 태그 제거
            normalized = removeHtmlTags(normalized);

            // 2. 제어 문자 제거 (특수문자 중 일부만)
            normalized = removeControlCharacters(normalized);

            // 3. 연속된 공백 정리
            normalized = normalizeSpaces(normalized);

            // 4. 연속된 줄바꿈 정리
            normalized = normalizeNewlines(normalized);

            // 5. 앞뒤 공백 제거
            normalized = normalized.trim();

            log.debug("Normalized text: {} chars -> {} chars", text.length(), normalized.length());

            return normalized;

        } catch (Exception e) {
            log.error("Error during text normalization", e);
            // 에러 발생 시 trim만 적용한 텍스트 반환
            return text.trim();
        }
    }

    /**
     * HTML 태그 제거
     */
    private String removeHtmlTags(String text) {
        return HTML_TAG_PATTERN.matcher(text).replaceAll("");
    }

    /**
     * 제어 문자 제거 (null, bell, backspace 등)
     * 단, 일반 줄바꿈(\n), 탭(\t), 캐리지 리턴(\r)은 유지
     */
    private String removeControlCharacters(String text) {
        return SPECIAL_CHARS_PATTERN.matcher(text).replaceAll("");
    }

    /**
     * 연속된 공백을 단일 공백으로 변경
     */
    private String normalizeSpaces(String text) {
        return MULTIPLE_SPACES_PATTERN.matcher(text).replaceAll(" ");
    }

    /**
     * 연속된 줄바꿈을 최대 2개로 제한
     */
    private String normalizeNewlines(String text) {
        return MULTIPLE_NEWLINES_PATTERN.matcher(text).replaceAll("\n\n");
    }

    /**
     * 텍스트가 유효한지 검증
     *
     * @param text 검증할 텍스트
     * @param minLength 최소 길이
     * @return 유효 여부
     */
    public boolean isValid(String text, int minLength) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        return text.trim().length() >= minLength;
    }

    /**
     * 텍스트가 유효한지 검증 (기본 최소 길이: 10자)
     */
    public boolean isValid(String text) {
        return isValid(text, 10);
    }
}