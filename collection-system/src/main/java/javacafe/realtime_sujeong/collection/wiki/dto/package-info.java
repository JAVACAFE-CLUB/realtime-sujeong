/**
 * Wiki XML 네임스페이스 설정
 *
 * 이 패키지의 모든 JAXB 클래스는 MediaWiki XML 네임스페이스를 사용합니다.
 * 개별 @XmlElement마다 namespace를 반복 선언하지 않도록 패키지 레벨에서 설정.
 *
 * @see WikiPage
 * @see <a href="https://www.mediawiki.org/xml/export-0.11/">MediaWiki Export Schema</a>
 */
@XmlSchema(
    namespace = "http://www.mediawiki.org/xml/export-0.11/",
    elementFormDefault = XmlNsForm.QUALIFIED
)
package javacafe.realtime_sujeong.collection.wiki.dto;

import jakarta.xml.bind.annotation.XmlNsForm;
import jakarta.xml.bind.annotation.XmlSchema;
