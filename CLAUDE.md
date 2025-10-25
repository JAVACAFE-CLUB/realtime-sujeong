# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **real-time search keyword service** built with Spring Boot 3.5.6 and Java 21. The project is designed as a **Domain-Driven multi-module system** that collects data from various sources (Wiki dumps, news articles, SNS APIs), processes it through cleaning and indexing stages, and serves real-time search rankings.

## Architecture

### System Overview

The system follows a **4-module microservices architecture**:

```
[Data Sources] → [Collection] → [Cleaning] → [Indexing] → [Serving]
                     ↓ Kafka      ↓ Kafka      ↓
                   MongoDB     MongoDB   Elasticsearch
```

1. **collection-system**: Collects raw data from external sources
2. **cleaning-system**: Processes and cleans raw data
3. **indexing-system**: Indexes cleaned data into Elasticsearch
4. **serving-system**: Provides REST APIs for search functionality

### Collection System Architecture (Domain-Driven Design)

**완전한 도메인 중심 아키텍처 (Option 1)**를 채택하여 각 바운디드 컨텍스트가 완전히 독립적으로 구성됩니다.

```
collection-system/
├── rss/                              # RSS 바운디드 컨텍스트
│   ├── domain/                       # 도메인 모델
│   │   ├── RssRawData.java          # RSS 원본 데이터 엔티티
│   │   └── RssRawDataRepository.java
│   ├── service/                      # 애플리케이션 서비스
│   │   └── RssCollectionService.java
│   ├── collector/                    # RSS 수집 로직 (도메인 내부)
│   │   ├── dto/
│   │   ├── parser/
│   │   └── strategy/
│   └── controller/                   # API 엔드포인트
│       └── RssCollectionController.java
│
├── wiki/                             # Wiki 바운디드 컨텍스트
│   ├── domain/                       # 도메인 모델
│   │   ├── WikiRawData.java         # Wiki 원본 데이터 엔티티
│   │   └── WikiRawDataRepository.java
│   ├── batch/                        # 배치 처리 (Wiki 전용)
│   │   ├── job/
│   │   ├── reader/
│   │   ├── processor/
│   │   └── writer/
│   ├── collector/                    # Wiki 수집 로직 (도메인 내부)
│   │   └── dto/
│   └── controller/                   # API 엔드포인트
│       └── WikiBatchController.java
│
└── common/                           # 진짜 공통 (여러 도메인이 공유)
    ├── kafka/                        # Kafka 메시징
    │   ├── config/
    │   └── service/
    ├── config/                       # 공통 설정
    └── util/                         # 유틸리티
```

## Development Commands

### Build and Run
```bash
# Build the project
./gradlew build

# Run collection-system (Port 8081)
./gradlew collection-system:bootRun

# Run tests
./gradlew test

# Create JAR
./gradlew bootJar
```

### Kafka Environment
```bash
# Start Kafka cluster (Docker)
docker-compose up -d

# Check Kafka topics
docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list

# Kafka Web UI
open http://localhost:8080
```

### Development
```bash
# Clean build
./gradlew clean build

# Run specific test class
./gradlew test --tests "javacafe.realtime_sujeong.*"

# Build specific module
./gradlew collection-system:build
```

## Technology Stack

- **Framework**: Spring Boot 3.5.6
- **Java Version**: 21
- **Build Tool**: Gradle with Kotlin DSL
- **Database**: MongoDB (for data storage)
- **Message Queue**: Apache Kafka
- **Search Engine**: Elasticsearch
- **Cache**: Redis
- **Testing**: JUnit 5

## Data Pipeline Architecture

### Data Flow
1. **Collection**: Raw data → MongoDB → Kafka message
2. **Cleaning**: Kafka consumer → MongoDB query → Data processing → MongoDB storage → Kafka message
3. **Indexing**: Kafka consumer → MongoDB query → Elasticsearch indexing
4. **Serving**: Elasticsearch queries + MongoDB details + Redis caching

### MongoDB Collections Structure
- **RSS Raw Data**: `rss_raw_data` - RSS 뉴스 원본 데이터
- **Wiki Raw Data**: `wiki_raw_data` - Wikipedia 원본 데이터
- **Cleaned Data**: `cleaned_data` - 정제된 데이터
- Status tracking: `collected → processing → processed → indexed`

### Kafka Message Format
Messages use standardized format implemented in `common/kafka/dto/`:

```java
KafkaMessage<T> {
  version: "1.0"
  messageId: UUID
  timestamp: LocalDateTime
  eventType: "DATA_COLLECTED|DATA_CLEANED|DATA_INDEXED"
  payload: T (CollectionPayload, CleaningPayload, IndexingPayload)
  retryCount: 0
}
```

**Topics:**
- `collection-to-cleaning`: Collection → Cleaning System
- `cleaning-to-indexing`: Cleaning → Indexing System
- `dead-letter-queue`: 실패한 메시지 저장

## Key Design Principles

### 1. Domain-Driven Design (DDD)
- **Bounded Context**: RSS와 Wiki는 완전히 독립된 바운디드 컨텍스트
- **High Cohesion**: 각 도메인 관련 코드가 한 곳에 모임
- **Low Coupling**: 도메인 간 의존성 최소화

### 2. Data Integrity
- Each data item has unique ID (SHA-256 hash) to prevent duplicates
- RSS: `SHA-256(link + pubDate)`
- Wiki: `SHA-256(pageId + revisionId)`

### 3. Async Processing
- Kafka-based message passing between modules
- Non-blocking data collection and processing

### 4. Error Handling
- 3-retry policy with exponential backoff
- Dead letter queues for failed messages

### 5. Scalability
- Independent module scaling via Kafka partitioning
- Microservice-ready architecture

## Module Structure

### Implementation Status

1. **collection-system**: 🚧 **진행 중** - 도메인 중심 아키텍처로 구현 중
   - ✅ Phase 1: 디렉토리 구조 생성 완료
   - 📋 Phase 2~11: 구현 예정

2. **common**: 📋 **미구현** - 공통 모듈
   - Kafka 메시지 포맷
   - 공통 상수 및 유틸리티

3. **cleaning-system**: 📋 **미구현** - 데이터 정제 시스템
4. **indexing-system**: 📋 **미구현** - 검색 인덱싱 시스템
5. **serving-system**: 📋 **미구현** - REST API 서비스

### Planned Data Pipeline
```
RSS/Wiki 수집 → MongoDB 저장 → Kafka 메시지 전송 → collection-to-cleaning Topic
                                                  ↓
                                              (Cleaning System)
```

## Domain Models

### RSS Domain
```java
// collection/rss/domain/RssRawData.java
- dataId: String (SHA-256 hash)
- source: String (항상 "rss")
- strategyName: String (조선일보, 매일경제 등)
- rssItem: RssItem
- collectedAt: LocalDateTime
```

### Wiki Domain
```java
// collection/wiki/domain/WikiRawData.java
- dataId: String (SHA-256 hash)
- source: String (항상 "wiki")
- namespace: String
- title: String
- wikiPage: WikiPage
- collectedAt: LocalDateTime
```

## Data ID Generation Rules

### RSS Collection System
- **ID Format**: SHA-256 hash of `link + pubDate`
- **Purpose**: Ensures uniqueness and prevents duplicate collection across RSS feeds
- **Example**: `SHA-256("https://www.mk.co.kr/news/politics/11425193" + "2025-09-21T15:03:52")`
- **MongoDB Storage**: Used as `dataId` field for upsert operations
- **Kafka Message**: Sent as `dataId` in payload for cleaning-system processing

### Wiki Collection System
- **ID Format**: SHA-256 hash of `pageId + revisionId`
- **Purpose**: Ensures uniqueness per revision
- **MongoDB Storage**: Used as `dataId` field for upsert operations

### Implementation Guidelines
- Use appropriate fields as hash input for consistency
- Apply UTF-8 encoding before hashing
- Store as hexadecimal string in MongoDB
- Same article republished with different timestamp gets new ID (captures updates)

## Development Guidelines

### Package Naming Convention
- Package name uses underscore: `javacafe.realtime_sujeong` (not hyphen due to Java naming restrictions)
- Domain packages: `collection.rss`, `collection.wiki`
- Common packages: `collection.common.{kafka,config,util}`

### Code Organization
- **Domain Layer First**: 도메인 모델부터 구현 (의존성 최소)
- **Bottom-Up Approach**: DTO → Service → Controller 순서
- **Test-Driven**: 각 레이어마다 테스트 코드 작성

### Dependency Direction
```
Controller → Service → Repository
    ↓          ↓
  Common ← Collector
```

### When to Use Each Package

#### rss/domain/
- MongoDB 엔티티 (RssRawData)
- Repository 인터페이스

#### rss/collector/
- RSS 수집 관련 DTO
- 파싱 전략 (Strategy Pattern)
- RSS 파서

#### rss/service/
- 비즈니스 로직
- 수집 orchestration

#### rss/controller/
- REST API 엔드포인트
- HTTP 요청 처리

#### wiki/* (동일한 구조)
- Wiki 전용 로직
- Batch 처리 포함

#### common/*
- **여러 도메인에서 공유**하는 코드만
- Kafka 메시징
- 공통 설정
- 유틸리티

## Current Implementation Phase

**Branch**: `feature/20251025/collection-system`

**Progress**:
- ✅ Phase 1: 디렉토리 구조 생성 (완료)
- 📋 Phase 2: 도메인 레이어 구현 (예정)
- 📋 Phase 3: 공통 유틸리티 구현 (예정)
- 📋 Phase 4: RSS Collector 구현 (예정)
- 📋 Phase 5: Wiki Collector 구현 (예정)
- 📋 Phase 6: Service 레이어 구현 (예정)
- 📋 Phase 7: Batch 레이어 구현 (예정)
- 📋 Phase 8: Controller 레이어 구현 (예정)
- 📋 Phase 9: Kafka & Config 구현 (예정)
- 📋 Phase 10: 테스트 코드 작성 (예정)
- 📋 Phase 11: 빌드 검증 (예정)

## Migration from Previous Architecture

**Previous**: 기존에는 `collector/` 패키지가 도메인 외부에 존재
**Current**: Collector가 각 도메인 내부로 이동하여 완전한 응집도 달성

**Benefits**:
- ✅ 완전한 도메인 독립성
- ✅ 마이크로서비스로 전환 용이
- ✅ 변경 영향 범위 최소화
- ✅ 도메인별 독립적인 확장 가능

## Best Practices

### When Creating New Features

1. **도메인 파악**: RSS인가? Wiki인가? 아니면 정말 공통인가?
2. **올바른 패키지 선택**:
   - RSS 전용 → `collection/rss/`
   - Wiki 전용 → `collection/wiki/`
   - 진짜 공통 → `collection/common/`
3. **레이어 순서 준수**: Domain → Service → Controller
4. **테스트 작성**: 각 레이어마다 테스트 코드

### Code Style
- Uses Lombok for reducing boilerplate code
- All modules follow the same Spring Boot project structure
- MongoDB collections require specific indexes for performance

### Testing
- Unit tests for each service
- Integration tests for controllers
- Mock external dependencies (Kafka, MongoDB)

## Important Notes

- **DO NOT** mix RSS and Wiki logic
- **DO NOT** put domain-specific code in `common/`
- **DO** keep each bounded context independent
- **DO** write tests for each layer
- **DO** follow DDD principles

## Questions?

If you're unsure about:
- Where to put a new class → Check the domain it belongs to
- How to structure code → Follow existing patterns in the same domain
- When to use common → Only if truly shared across multiple domains

Remember: **Domain-driven, not layer-driven!** 🎯
