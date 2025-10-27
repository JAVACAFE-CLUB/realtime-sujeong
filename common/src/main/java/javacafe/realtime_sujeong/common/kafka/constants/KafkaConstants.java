package javacafe.realtime_sujeong.common.kafka.constants;

/**
 * Kafka 관련 상수 정의
 * 토픽명, 이벤트 타입, 우선순위 등
 */
public final class KafkaConstants {

    private KafkaConstants() {
        // 유틸리티 클래스이므로 인스턴스화 방지
    }

    /**
     * Kafka 토픽명
     */
    public static final class Topics {
        /**
         * Collection System → Cleaning System
         */
        public static final String COLLECTION_TO_CLEANING = "collection-to-cleaning";

        /**
         * Cleaning System → Indexing System
         */
        public static final String CLEANING_TO_INDEXING = "cleaning-to-indexing";

        /**
         * 실패한 메시지 저장용 (Dead Letter Queue)
         */
        public static final String DEAD_LETTER_QUEUE = "dead-letter-queue";

        private Topics() {
        }
    }

    /**
     * 이벤트 타입
     */
    public static final class EventTypes {
        /**
         * 데이터 수집 완료
         */
        public static final String DATA_COLLECTED = "DATA_COLLECTED";

        /**
         * 데이터 정제 완료
         */
        public static final String DATA_CLEANED = "DATA_CLEANED";

        /**
         * 데이터 인덱싱 완료
         */
        public static final String DATA_INDEXED = "DATA_INDEXED";

        private EventTypes() {
        }
    }

    /**
     * 데이터 소스 타입
     */
    public enum Sources {
        RSS("rss"),
        WIKI("wiki"),
        API("api");

        private final String value;

        Sources(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * 처리 우선순위
     */
    public static final class Priorities {
        public static final String URGENT = "URGENT";
        public static final String HIGH = "HIGH";
        public static final String NORMAL = "NORMAL";
        public static final String LOW = "LOW";

        private Priorities() {
        }
    }

    /**
     * MongoDB 컬렉션명
     */
    public static final class Collections {
        /**
         * Raw Data 컬렉션 (통합)
         */
        public static final String RAW_DATA = "raw_data";

        /**
         * RSS Raw Data
         */
        public static final String RSS_RAW_DATA = "rss_raw_data";

        /**
         * Wiki Raw Data
         */
        public static final String WIKI_RAW_DATA = "wiki_raw_data";

        /**
         * Cleaned Data
         */
        public static final String CLEANED_DATA = "cleaned_data";

        private Collections() {
        }
    }

    /**
     * 메시지 포맷 버전
     */
    public static final class Versions {
        public static final String DEFAULT = "1.0";

        private Versions() {
        }
    }
}