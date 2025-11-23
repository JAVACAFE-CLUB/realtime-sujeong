package javacafe.realtime_sujeong.cleaning.service.processor;

import info.bliki.wiki.filter.PlainTextConverter;
import info.bliki.wiki.model.WikiModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Wiki wikitext를 plain text로 변환하는 Processor
 * Bliki engine을 사용하여 wikitext 마크업 제거
 */
@Slf4j
@Component
public class WikiTextExtractor {

    private final WikiModel wikiModel;

    public WikiTextExtractor() {
        // WikiModel 초기화
        // ${image}와 ${title}은 이미지와 링크 URL 템플릿 (실제로는 사용 안 함)
        this.wikiModel = new WikiModel("${image}", "${title}");
    }

    /**
     * Wikitext를 plain text로 변환
     *
     * @param wikitext 원본 wikitext
     * @return plain text (마크업 제거됨)
     */
    public String extract(String wikitext) {
        if (wikitext == null || wikitext.isEmpty()) {
            log.warn("Empty wikitext provided");
            return "";
        }

        try {
            // Bliki를 사용하여 wikitext를 plain text로 변환
            String plainText = wikiModel.render(new PlainTextConverter(), wikitext);

            // 추가 정리 (과도한 공백, 줄바꿈 정리)
            plainText = normalizeWhitespace(plainText);

            log.debug("Extracted plain text: {} chars from {} chars wikitext",
                    plainText.length(), wikitext.length());

            return plainText;

        } catch (IOException e) {
            log.error("IO error while extracting text from wikitext", e);
            // Fallback: 원본 반환
            return wikitext;

        } catch (Exception e) {
            log.error("Unexpected error while extracting text from wikitext", e);
            // Fallback: 원본 반환
            return wikitext;
        }
    }

    /**
     * 공백 정규화 (과도한 공백, 줄바꿈 정리)
     */
    private String normalizeWhitespace(String text) {
        if (text == null) return "";

        // 연속된 공백을 하나로
        text = text.replaceAll("[ \\t]+", " ");

        // 연속된 줄바꿈을 최대 2개로
        text = text.replaceAll("\\n{3,}", "\n\n");

        // 앞뒤 공백 제거
        text = text.trim();

        return text;
    }

    /**
     * Wikitext가 유효한지 검증
     *
     * @param wikitext 검증할 wikitext
     * @return 유효 여부
     */
    public boolean isValid(String wikitext) {
        return wikitext != null && !wikitext.trim().isEmpty();
    }
}