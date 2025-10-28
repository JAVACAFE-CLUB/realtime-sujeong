# Collection System - Design Document

## 📋 목차
1. [개요](#개요)
2. [아키텍처 설계](#아키텍처-설계)
3. [도메인 모델](#도메인-모델)
4. [데이터 흐름](#데이터-흐름)
5. [구현 계획](#구현-계획)
6. [기술 스택](#기술-스택)

---

## 개요

### 목적
외부 소스(RSS 뉴스, Wikipedia dump)로부터 데이터를 수집하여 MongoDB에 저장하고, Kafka를 통해 다음 단계(Cleaning System)로 전달하는 시스템

### 핵심 요구사항
- ✅ **도메인 중심 설계**: RSS와 Wiki를 완전히 독립된 바운디드 컨텍스트로 분리
- ✅ **데이터 무결성**: 중복 방지를 위한 고유 ID 생성 (SHA-256)
- ✅ **확장성**: 각 도메인이 독립적으로 확장 가능
- ✅ **비동기 처리**: Kafka를 통한 느슨한 결합
- ✅ **오류 처리**: 재시도 로직 및 Dead Letter Queue

---

## 아키텍처 설계

### 1. 전체 구조 (Domain-Driven Design)

```
collection-system/
├── rss/                              # RSS 바운디드 컨텍스트
│   ├── domain/                       # 도메인 모델
│   │   ├── RssRawData.java          # MongoDB 엔티티
│   │   └── RssRawDataRepository.java
│   │
│   ├── collector/                    # 수집 로직 (도메인 내부)
│   │   ├── dto/
│   │   │   ├── RssEntry.java        # RSS 피드 엔트리
│   │   │   └── RssItem.java         # 파싱된 RSS 아이템
│   │   ├── parser/
│   │   │   └── RssParsingStrategyManager.java
│   │   └── strategy/
│   │       ├── RssParsingStrategy.java      # 인터페이스
│   │       ├── ChosunParsingStrategy.java
│   │       ├── MaeilKyungjaeParsingStrategy.java
│   │       └── DongaParsingStrategy.java
│   │
│   ├── service/                      # 애플리케이션 서비스
│   │   └── RssCollectionService.java
│   │
│   └── controller/                   # API 엔드포인트
│       └── RssCollectionController.java
│
├── wiki/                             # Wiki 바운디드 컨텍스트
│   ├── domain/                       # 도메인 모델
│   │   ├── WikiRawData.java         # MongoDB 엔티티
│   │   └── WikiRawDataRepository.java
│   │
│   ├── collector/                    # 수집 로직 (도메인 내부)
│   │   └── dto/
│   │       ├── WikiPage.java        # JAXB 매핑 클래스
│   │       ├── LocalDateTimeAdapter.java
│   │       └── package-info.java
│   │
│   ├── batch/                        # 배치 처리 (Wiki 전용)
│   │   ├── job/
│   │   │   └── WikiCollectionJobConfig.java
│   │   ├── reader/
│   │   │   └── WikiItemReaderConfig.java    # StAX Reader
│   │   ├── processor/
│   │   │   └── WikiPageProcessor.java
│   │   └── writer/
│   │       └── WikiDataWriter.java
│   │
│   └── controller/                   # API 엔드포인트
│       └── WikiBatchController.java
│
└── common/                           # 공통 모듈
    ├── kafka/
    │   ├── config/
    │   │   ├── KafkaProducerConfig.java
    │   │   └── KafkaTopicConfig.java
    │   └── service/
    │       └── KafkaMessageService.java
    ├── config/
    │   ├── MongoIndexConfig.java
    │   └── BatchConfig.java
    └── util/
        └── DataIdGenerator.java
```

### 2. 설계 원칙

#### DDD (Domain-Driven Design)
- **Bounded Context**: RSS와 Wiki는 완전히 독립된 도메인
- **High Cohesion**: 관련 코드가 한 곳에 모임
- **Low Coupling**: 도메인 간 의존성 최소화

#### SOLID 원칙
- **S**: 각 클래스는 단일 책임 (RssCollectionService, WikiDataWriter)
- **O**: Strategy Pattern으로 확장에 열려있음 (새 뉴스사 추가 가능)
- **L**: Repository는 인터페이스로 추상화
- **I**: 작은 인터페이스로 분리 (RssParsingStrategy)
- **D**: 추상화에 의존 (구체적인 전략이 아닌 인터페이스에 의존)

---

## 도메인 모델

### RSS Domain

#### RssRawData (Aggregate Root)
```java
@Document(collection = "rss_raw_data")
public class RssRawData {
    @Id
    private String id;                    // MongoDB _id

    @Indexed(unique = true)
    private String dataId;                // SHA-256(link + pubDate)

    @Indexed
    private String source = "rss";        // 항상 "rss"

    @Indexed
    private String strategyName;          // 조선일보, 매일경제 등

    private RssItem rssItem;              // 원본 데이터

    @Indexed
    private LocalDateTime collectedAt;
}
```

**설계 의도**:
- `dataId`: 중복 방지를 위한 비즈니스 키
- `source`: 고정값 "rss" (도메인 식별자)
- `strategyName`: 어떤 파싱 전략을 사용했는지 추적
- `rssItem`: 타입 안전성 보장 (Object가 아닌 구체적 타입)

#### RssItem (Value Object)
```java
public class RssItem {
    private String id;
    private String title;
    private String link;
    private String author;
    private LocalDateTime pubDate;
    private String description;
    private String content;             // 크롤링된 본문
    private String source;              // 전략명
    private LocalDateTime collectedAt;
}
```

### Wiki Domain

#### WikiRawData (Aggregate Root)
```java
@Document(collection = "wiki_raw_data")
public class WikiRawData {
    @Id
    private String id;                    // MongoDB _id

    @Indexed(unique = true)
    private String dataId;                // SHA-256(pageId + revisionId)

    @Indexed
    private String source = "wiki";       // 항상 "wiki"

    @Indexed
    private String namespace;             // 0: 일반, 14: 분류 등

    @Indexed
    private String title;                 // 검색/필터링용

    private WikiPage wikiPage;            // 원본 데이터

    @Indexed
    private LocalDateTime collectedAt;
}
```

**설계 의도**:
- `namespace`: Wiki 문서 분류 (일반 문서만 수집 가능)
- `title`: 인덱싱을 통한 빠른 검색
- `wikiPage`: JAXB로 파싱된 XML 데이터

---

## 데이터 흐름

### RSS 수집 플로우

```
1. HTTP Request
   ↓
2. RssCollectionController
   ↓
3. RssCollectionService
   ├─→ RssParsingStrategyManager (전략 선택)
   ├─→ RssParsingStrategy (RSS 파싱)
   ├─→ DataIdGenerator (고유 ID 생성)
   ├─→ RssRawDataRepository (MongoDB 저장)
   └─→ KafkaMessageService (Kafka 전송)
```

**상세 단계**:
1. 사용자가 `/api/rss/collect?feedUrl=...` 호출
2. Controller가 Service에 위임
3. Strategy Manager가 URL 기반으로 적절한 전략 선택
4. Strategy가 RSS 피드 파싱 → 각 아이템 크롤링
5. DataIdGenerator가 `SHA-256(link + pubDate)` 생성
6. Repository에 중복 체크 후 저장
7. Kafka로 메시지 전송 (`collection-to-cleaning` topic)

### Wiki 수집 플로우 (Batch)

```
1. HTTP Request (Batch 시작)
   ↓
2. WikiBatchController
   ↓
3. Spring Batch Job
   ├─→ WikiItemReaderConfig (StAX XML Reader)
   ├─→ WikiPageProcessor (검증 + 변환)
   ├─→ WikiDataWriter (Chunk 처리)
       ├─→ WikiRawDataRepository (Bulk Insert)
       └─→ KafkaMessageService (Batch 전송)
```

**상세 단계**:
1. 사용자가 `/api/wiki/batch/collection/run` 호출
2. Spring Batch Job 시작
3. **Reader**: StAX로 XML 파일 스트리밍 (메모리 효율적)
4. **Processor**:
   - 필수 필드 검증 (pageId, title, revision)
   - WikiPage → WikiRawData 변환
   - DataIdGenerator로 `SHA-256(pageId + revisionId)` 생성
5. **Writer** (Chunk 단위):
   - 청크 내 중복 체크 (Bulk Query)
   - MongoDB Bulk Insert
   - Kafka 메시지 배치 전송

---

## 구현 계획

### Phase 0: 모듈 분리 및 빌드 설정 ✅
- [x] settings.gradle 설정
- [x] 멀티모듈 build.gradle 구성
- [x] common 모듈 설정
- [x] collection-system 의존성 설정

### Phase 1: 디렉토리 구조 생성 ✅
- [x] RSS 바운디드 컨텍스트 패키지
- [x] Wiki 바운디드 컨텍스트 패키지
- [x] common 패키지
- [x] CollectionSystemApplication.java

### Phase 2: 도메인 레이어 📋
**파일**:
- `rss/domain/RssRawData.java`
- `rss/domain/RssRawDataRepository.java`
- `wiki/domain/WikiRawData.java`
- `wiki/domain/WikiRawDataRepository.java`

**작업 내용**:
- MongoDB Document 정의
- Repository 인터페이스 정의
- 인덱스 설정 (dataId unique, source, strategyName)

### Phase 3: 공통 유틸리티 📋
**파일**:
- `common/util/DataIdGenerator.java`

**작업 내용**:
- SHA-256 해싱 유틸리티
- RSS: `generateRssDataId(link, pubDate)`
- Wiki: `generateWikiDataId(pageId, revisionId)`

### Phase 4: RSS Collector 📋
**파일**:
- `rss/collector/dto/RssEntry.java`
- `rss/collector/dto/RssItem.java`
- `rss/collector/strategy/RssParsingStrategy.java` (인터페이스)
- `rss/collector/strategy/ChosunParsingStrategy.java`
- `rss/collector/strategy/MaeilKyungjaeParsingStrategy.java`
- `rss/collector/strategy/DongaParsingStrategy.java`
- `rss/collector/parser/RssParsingStrategyManager.java`

**작업 내용**:
- RSS 파싱 인터페이스 정의
- 각 뉴스사별 파싱 전략 구현
- Strategy Pattern 적용
- Jsoup 기반 본문 크롤링

### Phase 5: Wiki Collector 📋
**파일**:
- `wiki/collector/dto/WikiPage.java`
- `wiki/collector/dto/LocalDateTimeAdapter.java`
- `wiki/collector/dto/package-info.java`

**작업 내용**:
- JAXB 어노테이션 기반 XML 매핑
- Nested 클래스 (Revision, Contributor, TextContent)
- LocalDateTime 어댑터 구현

### Phase 6: Service 레이어 📋
**파일**:
- `rss/service/RssCollectionService.java`

**작업 내용**:
- RSS 수집 orchestration
- 병렬 처리 (CompletableFuture)
- 중복 체크 및 저장
- Kafka 메시지 전송

### Phase 7: Batch 레이어 📋
**파일**:
- `wiki/batch/reader/WikiItemReaderConfig.java`
- `wiki/batch/processor/WikiPageProcessor.java`
- `wiki/batch/writer/WikiDataWriter.java`
- `wiki/batch/job/WikiCollectionJobConfig.java`

**작업 내용**:
- StAX 기반 XML Reader 설정
- Processor: 검증 + 변환
- Writer: Bulk Insert + Kafka 배치 전송
- Job 설정 (Step, Chunk size)

### Phase 8: Controller 레이어 📋
**파일**:
- `rss/controller/RssCollectionController.java`
- `wiki/controller/WikiBatchController.java`

**작업 내용**:
- REST API 엔드포인트
- RSS: `/api/rss/collect`
- Wiki: `/api/wiki/batch/collection/run`
- 에러 핸들링

### Phase 9: Kafka & Config 📋
**파일**:
- `common/kafka/service/KafkaMessageService.java`
- `common/kafka/config/KafkaProducerConfig.java`
- `common/kafka/config/KafkaTopicConfig.java`
- `common/config/MongoIndexConfig.java`
- `common/config/BatchConfig.java`

**작업 내용**:
- Kafka Producer 설정
- Topic 자동 생성 설정
- MongoDB 인덱스 자동 생성
- Spring Batch 설정

### Phase 10: 테스트 코드 📋
**파일**:
- `rss/service/RssCollectionServiceTest.java`
- 기타 통합 테스트

**작업 내용**:
- Service 레이어 단위 테스트
- Testcontainers (MongoDB, Kafka)
- Mock 기반 테스트

### Phase 11: 빌드 검증 📋
**작업 내용**:
- `./gradlew clean build` 성공 확인
- JAR 생성 확인
- 통합 테스트 실행

---

## 기술 스택

### Core
- **Spring Boot**: 3.5.6
- **Java**: 21
- **Build Tool**: Gradle 8.x

### Database
- **MongoDB**: 원본 데이터 저장
  - Collection: `rss_raw_data`, `wiki_raw_data`
  - Index: dataId (unique), source, strategyName/namespace

### Messaging
- **Apache Kafka**: 비동기 메시지 전달
  - Topic: `collection-to-cleaning`
  - Format: `KafkaMessage<CollectionPayload>`

### Batch Processing
- **Spring Batch**: Wiki 대용량 처리
  - StAX Reader: 메모리 효율적 XML 파싱
  - Chunk 처리: 1000건씩 배치 처리

### HTTP Client
- **Jsoup**: RSS 본문 크롤링
  - HTML 파싱
  - CSS Selector 기반 추출

### XML Processing
- **JAXB (Jakarta XML Binding)**: Wiki XML 파싱
  - Annotation 기반 매핑
  - StAX와 결합하여 스트리밍 파싱

---

## 데이터 모델 상세

### MongoDB Collections

#### rss_raw_data
```json
{
  "_id": "ObjectId",
  "dataId": "SHA-256 hash",           // unique index
  "source": "rss",                    // index
  "strategyName": "조선일보",         // index
  "rssItem": {
    "id": "...",
    "title": "...",
    "link": "...",
    "author": "...",
    "pubDate": "2025-10-25T10:00:00",
    "description": "...",
    "content": "...",                  // 크롤링된 본문
    "source": "조선일보",
    "collectedAt": "2025-10-25T10:05:00"
  },
  "collectedAt": "2025-10-25T10:05:00"  // index
}
```

#### wiki_raw_data
```json
{
  "_id": "ObjectId",
  "dataId": "SHA-256 hash",           // unique index
  "source": "wiki",                   // index
  "namespace": "0",                   // index (0=일반 문서)
  "title": "대한민국",                // index
  "wikiPage": {
    "title": "대한민국",
    "pageId": "12345",
    "namespace": "0",
    "revision": {
      "revisionId": "67890",
      "timestamp": "2025-10-20T15:30:00",
      "contributor": {
        "username": "...",
        "userId": "..."
      },
      "model": "wikitext",
      "format": "text/x-wiki",
      "text": {
        "bytes": 50000,
        "content": "위키텍스트 내용..."
      }
    }
  },
  "collectedAt": "2025-10-25T10:05:00"  // index
}
```

### Kafka Message Format

```json
{
  "version": "1.0",
  "messageId": "UUID",
  "timestamp": "2025-10-25T10:05:00",
  "eventType": "DATA_COLLECTED",
  "payload": {
    "dataId": "SHA-256 hash",
    "source": "rss | wiki",
    "mongoCollectionName": "rss_raw_data | wiki_raw_data",
    "priority": "normal",
    "sourceDetails": {
      "strategyName": "조선일보"  // RSS
      // OR
      "namespace": "0",          // Wiki
      "title": "대한민국"
    }
  },
  "retryCount": 0
}
```

---

## 성능 고려사항

### RSS 수집
- **병렬 처리**: CompletableFuture로 각 아이템 동시 크롤링
- **연결 풀**: HTTP 클라이언트 연결 재사용
- **타임아웃**: 크롤링 시간 제한 (30초)

### Wiki 수집
- **스트리밍 파싱**: StAX로 메모리 효율적 XML 처리
- **Chunk 처리**: 1000건씩 묶어서 Bulk Insert
- **배치 중복 체크**: `findDataIdsByDataIdIn()` 한 번의 쿼리로 청크 전체 확인
- **비동기 Kafka 전송**: Writer는 Kafka 전송을 기다리지 않음

### MongoDB 인덱스
```javascript
db.rss_raw_data.createIndex({ dataId: 1 }, { unique: true })
db.rss_raw_data.createIndex({ source: 1 })
db.rss_raw_data.createIndex({ strategyName: 1 })
db.rss_raw_data.createIndex({ collectedAt: -1 })

db.wiki_raw_data.createIndex({ dataId: 1 }, { unique: true })
db.wiki_raw_data.createIndex({ source: 1 })
db.wiki_raw_data.createIndex({ namespace: 1 })
db.wiki_raw_data.createIndex({ title: 1 })
db.wiki_raw_data.createIndex({ collectedAt: -1 })
```

---

## 확장 계획

### 새 뉴스사 추가
1. `rss/collector/strategy/` 에 새 Strategy 클래스 추가
2. `RssParsingStrategyManager` 에 URL 매핑 추가
3. 코드 변경 없이 확장 가능 (Open-Closed Principle)

### 새 데이터 소스 추가 (예: SNS)
1. 새 바운디드 컨텍스트 생성: `collection/sns/`
2. 독립적인 도메인 모델, Collector, Service
3. 기존 RSS, Wiki에 영향 없음

### 마이크로서비스로 분리
```
현재:  collection-system (rss + wiki)
분리:  rss-collection-service
      wiki-collection-service
```
→ `collection/rss/` 폴더를 통째로 새 프로젝트로 이동 가능!

---

## 참고 문서
- [CLAUDE.md](../CLAUDE.md) - 전체 프로젝트 가이드
- [common 모듈 README](../common/README.md) - Kafka 메시지 포맷
- [Spring Batch 공식 문서](https://spring.io/projects/spring-batch)
