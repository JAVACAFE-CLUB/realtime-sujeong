package com.javacafe.realtime_sujeong.collector.domain.rss;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NewsContentExtractor {

    public String extractFullContent(String url) {
        try {
            org.jsoup.nodes.Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(3000)
                    .get();
            
            String content = extractNewsContent(doc, url);
            
            if (content != null && content.length() > 100) {
                return content;
            }
            
        } catch (Exception e) {
            log.debug("컨텐츠 추출 실패: {}", url);
        }
        
        return null;
    }
    
    private String extractNewsContent(org.jsoup.nodes.Document doc, String url) {
        String[] contentSelectors = {
            "#dic_area", 
            ".go_trans._article_content",
            "article", 
            ".article-content", 
            ".news-content",
            ".content",
            ".post-content",
            ".entry-content",
            "main",
            "[property=og:description]",
            "[name=description]"
        };
        
        for (String selector : contentSelectors) {
            Elements elements = doc.select(selector);
            if (!elements.isEmpty()) {
                String text = elements.first().text();
                if (text.length() > 100) {
                    return text.trim();
                }
            }
        }
        
        return doc.body().text().trim();
    }
}