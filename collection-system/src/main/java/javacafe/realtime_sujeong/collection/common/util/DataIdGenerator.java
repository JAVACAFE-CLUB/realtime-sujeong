package javacafe.realtime_sujeong.collection.common.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataIdGenerator {

    /**
     * RSS 데이터 ID 생성
     * @param link RSS 아이템 링크
     * @param pubDate 발행 시간
     * @return SHA-256 해시 ID
     */
    public String generateRssDataId(String link, LocalDateTime pubDate) {
        String input = link + pubDate.toString();
        return DigestUtils.sha256Hex(input);
    }

    /**
     * Wiki 데이터 ID 생성
     * @param pageId Wiki 페이지 ID
     * @param revisionId 리비전 ID
     * @return SHA-256 해시 ID
     */
    public String generateWikiDataId(String pageId, String revisionId) {
        String input = "wiki:" + pageId + ":" + revisionId;
        return DigestUtils.sha256Hex(input);
    }
}