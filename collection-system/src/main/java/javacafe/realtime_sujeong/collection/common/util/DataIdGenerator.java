package javacafe.realtime_sujeong.collection.common.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataIdGenerator {

    /**
     * RSS 데이터 ID 생성
     * URL(link)을 그대로 dataId로 사용
     * - 같은 URL은 같은 dataId → 같은 Kafka 파티션 → 순서 보장
     * - 새 버전(pubDate 변경)은 upsert로 덮어씀
     * 
     * @param link RSS 아이템 링크 (URL)
     * @return URL 그대로 (dataId)
     */
    public static String generateRssDataId(String link) {
        return link;
    }

    /**
     * Wiki 데이터 ID 생성
     * pageId를 그대로 dataId로 사용
     * - 같은 페이지는 같은 dataId → 같은 Kafka 파티션 → 순서 보장
     * - 새 리비전(timestamp 변경)은 upsert로 덮어씀
     * 
     * @param pageId Wiki 페이지 ID
     * @return pageId 그대로 (dataId)
     */
    public static String generateWikiDataId(String pageId) {
        return pageId;
    }
}