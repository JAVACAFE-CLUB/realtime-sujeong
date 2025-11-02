package javacafe.realtime_sujeong.cleaning.config;

import org.apache.tika.Tika;
import org.apache.tika.langdetect.optimaize.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Apache Tika 설정
 * 텍스트 추출 및 언어 감지를 위한 Tika Bean 등록
 */
@Configuration
public class TikaConfig {

    /**
     * Tika 인스턴스 생성
     * 텍스트 추출에 사용
     */
    @Bean
    public Tika tika() {
        return new Tika();
    }

    /**
     * 언어 감지기 생성
     * Optimaize 기반 언어 감지
     */
    @Bean
    public LanguageDetector languageDetector() throws Exception {
        LanguageDetector detector = new OptimaizeLangDetector();
        detector.loadModels();
        return detector;
    }
}