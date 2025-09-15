package com.javacafe.realtime_sujeong.collector.domain.wiki;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

@Component
@Slf4j
public class WikiXmlProcessor {

    public void processWikimediaXmlStream(InputStream xmlStream, Consumer<WikiPage> pageProcessor) throws Exception {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        
        XMLStreamReader reader = factory.createXMLStreamReader(xmlStream, "UTF-8");

        StringBuilder pageContent = new StringBuilder();
        boolean inPage = false;
        String currentElement = "";

        while (reader.hasNext()) {
            int event = reader.next();

            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    currentElement = reader.getLocalName();
                    if ("page".equals(currentElement)) {
                        inPage = true;
                        pageContent.setLength(0);
                    }
                    if (inPage) {
                        appendStartElement(pageContent, reader);
                    }
                    break;

                case XMLStreamConstants.CHARACTERS:
                    if (inPage) {
                        try {
                            String text = reader.getText();
                            if (text != null) {
                                text = escapeXmlEntities(cleanInvalidXmlCharacters(text));
                                pageContent.append(text);
                            }
                        } catch (Exception e) {
                            log.debug("XML 문자 처리 중 오류", e);
                        }
                    }
                    break;

                case XMLStreamConstants.END_ELEMENT:
                    currentElement = reader.getLocalName();
                    if (inPage) {
                        pageContent.append("</").append(currentElement).append(">");
                    }
                    
                    if ("page".equals(currentElement)) {
                        inPage = false;
                        WikiPage wikiPage = parsePageXml(pageContent.toString());
                        if (wikiPage != null) {
                            pageProcessor.accept(wikiPage);
                        }
                    }
                    break;
            }
        }
    }

    private WikiPage parsePageXml(String pageXml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setValidating(false);
            factory.setFeature("http://xml.org/sax/features/namespaces", false);
            factory.setFeature("http://xml.org/sax/features/validation", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            
            DocumentBuilder builder = factory.newDocumentBuilder();
            
            byte[] xmlBytes = pageXml.getBytes(StandardCharsets.UTF_8);
            Document doc = builder.parse(new ByteArrayInputStream(xmlBytes));

            Element pageElement = doc.getDocumentElement();
            
            String title = getElementText(pageElement, "title");
            String text = getElementText(pageElement, "revision/text");
            String id = getElementText(pageElement, "id");

            if (title == null || text == null) {
                return null;
            }

            return new WikiPage(id, title, text);

        } catch (Exception e) {
            log.debug("페이지 XML 파싱 실패", e);
            return null;
        }
    }

    private void appendStartElement(StringBuilder content, XMLStreamReader reader) {
        content.append("<").append(reader.getLocalName());
        
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            content.append(" ")
                   .append(reader.getAttributeLocalName(i))
                   .append("=\"")
                   .append(reader.getAttributeValue(i))
                   .append("\"");
        }
        content.append(">");
    }

    private String getElementText(Element parent, String path) {
        try {
            String[] parts = path.split("/");
            Element current = parent;
            
            for (String part : parts) {
                NodeList nodeList = current.getElementsByTagName(part);
                if (nodeList.getLength() == 0) {
                    return null;
                }
                Node node = nodeList.item(0);
                if (node instanceof Element) {
                    current = (Element) node;
                } else {
                    return node.getTextContent();
                }
            }
            
            return current.getTextContent();
        } catch (Exception e) {
            return null;
        }
    }

    private String cleanInvalidXmlCharacters(String text) {
        if (text == null) {
            return null;
        }
        
        StringBuilder cleaned = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (isValidXmlCharacter(ch)) {
                cleaned.append(ch);
            }
        }
        return cleaned.toString();
    }
    
    private boolean isValidXmlCharacter(char ch) {
        return (ch == 0x09) || (ch == 0x0A) || (ch == 0x0D) ||
               (ch >= 0x20 && ch <= 0xD7FF) ||
               (ch >= 0xE000 && ch <= 0xFFFD);
    }
    
    private String escapeXmlEntities(String text) {
        if (text == null) {
            return null;
        }
        
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }

    public static class WikiPage {
        private final String id;
        private final String title;
        private final String text;

        public WikiPage(String id, String title, String text) {
            this.id = id;
            this.title = title;
            this.text = text;
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getText() { return text; }
    }
}