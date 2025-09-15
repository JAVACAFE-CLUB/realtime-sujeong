package com.javacafe.realtime_sujeong.collector.domain.wiki;

import org.springframework.stereotype.Component;

@Component
public class WikiTextCleaner {

    public String cleanWikiText(String text) {
        if (text == null) {
            return null;
        }

        return text.replaceAll("\\{\\{[^}]+\\}\\}", "") // 템플릿 제거
                  .replaceAll("\\[\\[[^|\\]]+\\|([^\\]]+)\\]\\]", "$1") // 링크 텍스트만 유지
                  .replaceAll("\\[\\[([^\\]]+)\\]\\]", "$1") // 내부 링크 정리
                  .replaceAll("\\[http[^\\s]+ ([^\\]]+)\\]", "$1") // 외부 링크 정리
                  .replaceAll("'{2,}", "") // 굵기/기울임 제거
                  .replaceAll("={2,}[^=]+={2,}", "") // 섹션 헤더 제거
                  .replaceAll("\\n{3,}", "\n\n") // 연속 개행 정리
                  .trim();
    }

    public boolean isRedirectOrSpecialPage(String title, String text) {
        return title.startsWith("Wikipedia:") ||
               title.startsWith("파일:") ||
               title.startsWith("틀:") ||
               title.startsWith("분류:") ||
               text.toLowerCase().startsWith("#redirect") ||
               text.toLowerCase().startsWith("#넘겨주기");
    }
}