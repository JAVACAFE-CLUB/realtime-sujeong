package com.javacafe.realtime_sujeong.collector.domain.rss;

import com.javacafe.realtime_sujeong.collector.common.CrawledData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class RssParser {

    public List<CrawledData> parseRssFeed(String rssUrl, String category) throws Exception {
        return parseRssFeedFromStream(URI.create(rssUrl).toURL().openStream(), category);
    }

    public List<CrawledData> parseRssFeedFromStream(InputStream inputStream, String category) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(inputStream);
        
        NodeList items = doc.getElementsByTagName("item");
        List<CrawledData> crawledDataList = new ArrayList<>();
        
        for (int i = 0; i < items.getLength(); i++) {
            Node itemNode = items.item(i);
            if (itemNode.getNodeType() == Node.ELEMENT_NODE) {
                Element itemElement = (Element) itemNode;
                try {
                    CrawledData crawledData = parseRssItem(itemElement, category);
                    if (crawledData != null) {
                        crawledDataList.add(crawledData);
                    }
                } catch (Exception e) {
                    log.debug("RSS 아이템 파싱 실패", e);
                }
            }
        }
        
        return crawledDataList;
    }

    private CrawledData parseRssItem(Element itemElement, String category) {
        try {
            String title = getElementText(itemElement, "title");
            String link = getElementText(itemElement, "link");
            String description = getElementText(itemElement, "description");
            String contentEncoded = getElementTextWithNamespace(itemElement, "content:encoded");
            String pubDate = getElementText(itemElement, "pubDate");
            
            if (title == null || link == null) {
                return null;
            }
            
            String actualContent = contentEncoded != null && !contentEncoded.trim().isEmpty() 
                    ? contentEncoded 
                    : description;
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("category", category);
            metadata.put("source", "naver_rss");
            metadata.put("originalPubDate", pubDate);
            metadata.put("hasContentEncoded", contentEncoded != null);
            
            LocalDateTime publishedAt = parsePublishedDate(pubDate);
            
            return CrawledData.builder()
                    .source("NAVER_RSS")
                    .url(link)
                    .title(cleanHtml(title))
                    .content(cleanHtml(actualContent))
                    .crawledAt(LocalDateTime.now())
                    .publishedAt(publishedAt)
                    .metadata(metadata)
                    .build();
                    
        } catch (Exception e) {
            log.debug("RSS 아이템 파싱 중 오류", e);
            return null;
        }
    }
    
    private String getElementText(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            Node node = nodeList.item(0);
            return node.getTextContent();
        }
        return null;
    }
    
    private String getElementTextWithNamespace(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            Node node = nodeList.item(0);
            return node.getTextContent();
        }
        
        if (tagName.contains(":")) {
            String localName = tagName.split(":")[1];
            nodeList = parent.getElementsByTagName(localName);
            if (nodeList.getLength() > 0) {
                Node node = nodeList.item(0);
                return node.getTextContent();
            }
        }
        
        return null;
    }
    
    private LocalDateTime parsePublishedDate(String pubDate) {
        if (pubDate == null) {
            return LocalDateTime.now();
        }
        
        try {
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(pubDate, DateTimeFormatter.RFC_1123_DATE_TIME);
            return zonedDateTime.toLocalDateTime();
        } catch (Exception e1) {
            try {
                ZonedDateTime zonedDateTime = ZonedDateTime.parse(pubDate, DateTimeFormatter.ISO_ZONED_DATE_TIME);
                return zonedDateTime.toLocalDateTime();
            } catch (Exception e2) {
                try {
                    ZonedDateTime zonedDateTime = ZonedDateTime.parse(pubDate.replace("Z", "+00:00"));
                    return zonedDateTime.toLocalDateTime();
                } catch (Exception e3) {
                    log.debug("발행일 파싱 실패: {}", pubDate);
                    return LocalDateTime.now();
                }
            }
        }
    }
    
    private String cleanHtml(String text) {
        if (text == null) {
            return null;
        }
        
        return text.replaceAll("<[^>]*>", "")
                  .replaceAll("&lt;", "<")
                  .replaceAll("&gt;", ">")
                  .replaceAll("&amp;", "&")
                  .replaceAll("&quot;", "\"")
                  .replaceAll("&nbsp;", " ")
                  .trim();
    }
}