package javacafe.realtime_sujeong.collection.common.util;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 데이터 고유 ID 생성기
 * SHA-256 해시를 사용하여 고유 ID 생성
 */
@Slf4j
public class DataIdGenerator {

    private static final String HASH_ALGORITHM = "SHA-256";

    /**
     * RSS 데이터 ID 생성
     * link + pubDate를 조합하여 SHA-256 해시 생성
     *
     * @param link 기사 링크
     * @param pubDate 발행일시 문자열
     * @return SHA-256 해시 (16진수 문자열)
     */
    public static String generateRssDataId(String link, String pubDate) {
        if (link == null || pubDate == null) {
            throw new IllegalArgumentException("link와 pubDate는 null일 수 없습니다.");
        }

        String input = link + pubDate;
        return generateHash(input);
    }

    /**
     * Wiki 데이터 ID 생성
     * title + timestamp를 조합하여 SHA-256 해시 생성
     *
     * @param title 문서 제목
     * @param timestamp 타임스탬프 문자열
     * @return SHA-256 해시 (16진수 문자열)
     */
    public static String generateWikiDataId(String title, String timestamp) {
        if (title == null || timestamp == null) {
            throw new IllegalArgumentException("title과 timestamp는 null일 수 없습니다.");
        }

        String input = title + timestamp;
        return generateHash(input);
    }

    /**
     * SHA-256 해시 생성
     *
     * @param input 입력 문자열
     * @return SHA-256 해시 (16진수 문자열)
     */
    private static String generateHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // 바이트 배열을 16진수 문자열로 변환
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 알고리즘을 찾을 수 없습니다.", e);
            throw new RuntimeException("해시 생성 실패", e);
        }
    }
}