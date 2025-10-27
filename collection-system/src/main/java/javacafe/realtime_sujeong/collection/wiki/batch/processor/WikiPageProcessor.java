package javacafe.realtime_sujeong.collection.wiki.batch.processor;

import javacafe.realtime_sujeong.collection.wiki.dto.WikiPage;
import javacafe.realtime_sujeong.collection.wiki.domain.WikiRawData;
import javacafe.realtime_sujeong.collection.common.util.DataIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * WikiPage를 WikiRawData로 변환하는 ItemProcessor
 * - 데이터 검증
 * - WikiRawData 엔티티 변환
 * (중복 체크는 Writer에서 청크 단위로 처리)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WikiPageProcessor implements ItemProcessor<WikiPage, WikiRawData> {

    private final DataIdGenerator dataIdGenerator;

    private static final ThreadLocal<Long> processCount = ThreadLocal.withInitial(() -> 0L);

    @Override
    public WikiRawData process(WikiPage wikiPage) throws Exception {
        try {
            long count = processCount.get() + 1;
            processCount.set(count);

            if (count % 100 == 0) {
                log.info("Processor 진행상황: {}개 처리 완료", count);
            }

            // 1. 필수 필드 검증
            if (!isValid(wikiPage)) {
                if (wikiPage != null) {
                    log.warn("유효하지 않은 Wiki 페이지 스킵: {}", wikiPage.getTitle());
                } else {
                    log.warn("null Wiki 페이지 스킵");
                }
                return null;  // null 반환 시 해당 아이템 skip
            }

            // 2. DataId 생성
            String pageId = wikiPage.getPageId();
            String revisionId = wikiPage.getRevision() != null ?
                    wikiPage.getRevision().getRevisionId() : "unknown";
            String dataId = dataIdGenerator.generateWikiDataId(pageId, revisionId);

            // 3. WikiRawData 엔티티 생성
            WikiRawData wikiRawData = WikiRawData.builder()
                    .dataId(dataId)
                    .namespace(wikiPage.getNamespace())
                    .title(wikiPage.getTitle())
                    .wikiPage(wikiPage)
                    .collectedAt(LocalDateTime.now())
                    .build();

            log.debug("Wiki 페이지 변환 완료: {} (pageId: {})", wikiPage.getTitle(), pageId);
            return wikiRawData;

        } catch (Exception e) {
            String title = (wikiPage != null && wikiPage.getTitle() != null) ?
                    wikiPage.getTitle() : "unknown";
            log.error("Wiki 페이지 처리 실패: {}", title, e);
            return null;  // 에러 발생 시 해당 아이템 skip
        }
    }

    /**
     * Wiki 페이지 유효성 검증
     */
    private boolean isValid(WikiPage wikiPage) {
        if (wikiPage == null) {
            return false;
        }

        // 필수 필드 체크
        if (wikiPage.getPageId() == null || wikiPage.getPageId().isEmpty()) {
            return false;
        }

        if (wikiPage.getTitle() == null || wikiPage.getTitle().isEmpty()) {
            return false;
        }

        // Revision 정보 체크
        if (wikiPage.getRevision() == null) {
            return false;
        }

        // 본문 내용 체크 (TextContent가 null이거나 내용이 비어있으면 skip)
        WikiPage.Revision revision = wikiPage.getRevision();
        if (revision.getText() == null ||
            revision.getText().getContent() == null ||
            revision.getText().getContent().isEmpty()) {
            return false;
        }

        return true;
    }
}