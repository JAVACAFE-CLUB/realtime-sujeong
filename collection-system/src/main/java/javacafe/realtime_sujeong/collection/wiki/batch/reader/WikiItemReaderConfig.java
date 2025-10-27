package javacafe.realtime_sujeong.collection.wiki.batch.reader;

import javacafe.realtime_sujeong.collection.wiki.dto.WikiPage;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

/**
 * Wiki XML 파일을 읽기 위한 ItemReader 설정
 */
@Configuration
public class WikiItemReaderConfig {

    @Value("${wiki.dump.file.path}")
    private String wikiDumpFilePath;

    /**
     * JAXB Unmarshaller 설정
     * XML을 WikiPage 객체로 변환
     */
    @Bean
    public Jaxb2Marshaller wikiPageMarshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setClassesToBeBound(WikiPage.class);
        return marshaller;
    }

    /**
     * StAX 기반 XML ItemReader
     * Wiki dump 파일을 스트리밍 방식으로 읽어서 WikiPage 객체로 변환
     */
    @Bean
    public StaxEventItemReader<WikiPage> wikiPageReader() {
        return new StaxEventItemReaderBuilder<WikiPage>()
                .name("wikiPageReader")
                .resource(new FileSystemResource(wikiDumpFilePath))
                .addFragmentRootElements("page")  // <page> 태그 단위로 파싱
                .unmarshaller(wikiPageMarshaller())
                .build();
    }
}
