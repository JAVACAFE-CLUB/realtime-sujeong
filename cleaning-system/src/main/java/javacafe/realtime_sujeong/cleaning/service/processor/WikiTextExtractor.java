package javacafe.realtime_sujeong.cleaning.service.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Wiki wikitext를 plain text로 변환하는 Processor
 * Apache Tika를 사용하여 wikitext 마크업 제거
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WikiTextExtractor {

    private final Tika tika;

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
            // Tika를 사용하여 텍스트 추출
            // wikitext를 바이트 스트림으로 변환 후 파싱
            ByteArrayInputStream inputStream = new ByteArrayInputStream(
                    wikitext.getBytes(StandardCharsets.UTF_8)
            );

            String plainText = tika.parseToString(inputStream);

            log.debug("Extracted plain text: {} chars from {} chars wikitext",
                    plainText.length(), wikitext.length());

            return plainText;

        } catch (IOException e) {
            log.error("IO error while extracting text from wikitext", e);
            // IO 에러 시 원본 반환 (fallback)
            return wikitext;

        } catch (TikaException e) {
            log.error("Tika parsing error while extracting text from wikitext", e);
            // Tika 파싱 에러 시 원본 반환 (fallback)
            return wikitext;

        } catch (Exception e) {
            log.error("Unexpected error while extracting text from wikitext", e);
            // 예상치 못한 에러 시 원본 반환 (fallback)
            return wikitext;
        }
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