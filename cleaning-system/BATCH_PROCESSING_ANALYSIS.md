# 🔍 Cleaning System 배치 처리 방식 비교 분석

> **Project:** Real-time Search Keyword Service
> **분석 일자:** 2025-10-28
> **결정 사항:** Kafka Batch Listener 방식 채택

---

## 📌 분석 배경

Kafka를 통해 대량의 메시지를 처리할 때 매번 MongoDB에 접근하면 성능 병목이 발생할 수 있음.
이를 해결하기 위한 배치 처리 방식을 비교 분석.

---

## 📊 처리 방식 비교

### 1️⃣ Kafka Batch Listener 방식 (채택 ✅)

#### 구조
```
Kafka Topic (collection-to-cleaning)
    ↓ (100개씩 배치로 poll)
CleaningConsumer (Batch Listener)
    ↓ (dataId 리스트로 한 번에 조회)
MongoDB (findAllByDataIdIn)
    ↓ (배치 정제)
Processor (병렬 처리 가능)
    ↓ (saveAll)
MongoDB (cleaned_data)
    ↓ (배치 발행)
Kafka Producer (cleaning-to-indexing)
```

#### 성능 지표
| 지표 | 예상 수치 |
|------|-----------|
| Throughput | **200-500 msg/sec** |
| Latency | **5-10초** (배치 누적 대기 시간) |
| MongoDB 부하 | **낮음** (bulk operation) |
| 메모리 사용 | **중간** (배치 크기만큼) |
| 확장성 | **높음** (Kafka partition 기반) |

#### 구현 코드 예시
```java
@KafkaListener(topics = "collection-to-cleaning")
public void consumeBatch(List<KafkaMessage<CollectionPayload>> messages) {
    // 1. dataId 추출
    List<String> dataIds = messages.stream()
        .map(m -> m.getPayload().getDataId())
        .toList();

    // 2. MongoDB에서 일괄 조회
    List<RawData> rawDataList = fetchRawDataInBatch(dataIds);

    // 3. 배치 정제 처리
    List<CleanedData> cleanedList = processInBatch(rawDataList);

    // 4. MongoDB에 일괄 저장
    cleanedDataRepository.saveAll(cleanedList);

    // 5. Kafka에 일괄 발행
    producerService.sendBatch(cleanedList);
}
```

#### 장점
- ✅ 구현 간단 (리스너만 변경)
- ✅ Real-time 특성 유지 (수 초 내 처리)
- ✅ MongoDB bulk operation으로 성능 향상
- ✅ 기존 설계와 호환성 좋음
- ✅ Collection System 수정 불필요
- ✅ Kafka의 스트림 처리 특성과 잘 맞음

#### 단점
- ⚠️ 배치 크기 튜닝 필요
- ⚠️ 메모리 관리 필요 (큰 배치 시)
- ⚠️ 일부 메시지만 실패 시 재처리 복잡

---

### 2️⃣ Spring Batch 방식 (미채택 ❌)

#### 구조
```
Kafka Topic (collection-to-cleaning)
    ↓
Kafka Reader (ItemReader)
    ↓ (chunk: 100)
Processor (정제)
    ↓
Writer (MongoDB + Kafka)
    ↓
Job Repository (작업 상태 관리)
```

#### 성능 지표
| 지표 | 예상 수치 |
|------|-----------|
| Throughput | **500-1000 msg/sec** |
| Latency | **분 단위** (스케줄링 대기) |
| MongoDB 부하 | **매우 낮음** |
| 메모리 사용 | **높음** (chunk + 트랜잭션) |
| 확장성 | **매우 높음** (Partitioning 지원) |

#### 장점
- ✅ 대용량 처리 최적화 (수백만 건)
- ✅ 재시작/복구 기능 내장
- ✅ 트랜잭션 관리 자동
- ✅ 통계/모니터링 내장

#### 단점
- ❌ 구현 복잡도 매우 높음
- ❌ **Real-time 처리 불가** (배치 스케줄링 필요)
- ❌ Job Repository용 별도 DB 필요
- ❌ Kafka와 궁합 안 좋음
- ❌ 설계 문서 전면 수정 필요
- ❌ Collection System에 간접 영향 (지연 발생)

---

## 📋 종합 비교표

| 항목 | Kafka Batch Listener ✅ | Spring Batch |
|------|------------------------|--------------|
| **처리 방식** | 스트림 (준 실시간) | 배치 (스케줄링) |
| **Throughput** | 200-500 msg/sec | 500-1000 msg/sec |
| **Latency** | 5-10초 | 분 단위 |
| **구현 난이도** | ⭐⭐ (쉬움) | ⭐⭐⭐⭐⭐ (매우 어려움) |
| **코드 변경** | Consumer만 수정 | 전체 재구성 |
| **설계 문서** | 소폭 수정 | 대규모 재작성 |
| **Collection 영향** | 없음 | 간접 영향 (지연) |
| **Real-time** | ✅ 유지 | ❌ 불가능 |
| **MongoDB 부하** | 낮음 | 매우 낮음 |
| **추가 인프라** | 없음 | Job Repository DB |
| **모니터링** | Kafka Consumer Lag | Job 실행 통계 |
| **적합한 경우** | **실시간 검색 서비스** | 대용량 일괄 처리 |

---

## 🎯 의사결정

### 채택: Kafka Batch Listener (1번)

#### 결정 근거
1. **Real-time Search Keyword Service 특성**
   - 실시간 검색어 순위 제공이 핵심 기능
   - 데이터 지연 최소화 필수

2. **시스템 아키텍처 일관성**
   - Collection System이 실시간으로 데이터 수집
   - Indexing System이 Elasticsearch에 즉시 색인
   - Cleaning은 중간 단계로 **지연 없이** 처리해야 함

3. **구현 복잡도 vs 성능 trade-off**
   - 200-500 msg/sec면 대부분의 사용 케이스 커버
   - 구현 간단 → 빠른 개발 및 유지보수

4. **기존 설계 호환성**
   - 현재 설계 문서와 충돌 최소화
   - Collection System 수정 불필요

---

## ⚙️ Kafka Batch Listener 구현 계획

### 1. Kafka Consumer Config
```yaml
spring:
  kafka:
    consumer:
      group-id: cleaning-group
      max-poll-records: 100          # 한 번에 100개 메시지 처리
      fetch-min-size: 1048576        # 1MB 이상 모아서 처리
      auto-offset-reset: earliest
```

### 2. MongoDB Bulk Operation
```java
// Repository에 추가
List<RssRawData> findAllByDataIdIn(List<String> dataIds);
List<WikiRawData> findAllByDataIdIn(List<String> dataIds);
```

### 3. MongoDB Connection Pool
```yaml
spring:
  data:
    mongodb:
      connection-pool:
        max-size: 50
        min-size: 10
```

### 4. 성능 튜닝 포인트
- **배치 크기**: 50~200개 (메모리와 latency 고려)
- **Kafka Partition 수**: Consumer 인스턴스 수와 동일
- **MongoDB Index**: dataId unique index (이미 적용)

---

## 🚀 확장 전략

### Horizontal Scaling
```
Kafka Topic: collection-to-cleaning (10 partitions)
    ↓
Cleaning Consumer Instance 1 (partition 0)
Cleaning Consumer Instance 2 (partition 1)
...
Cleaning Consumer Instance 10 (partition 9)
```

### 성능 모니터링 지표
- **Kafka Consumer Lag**: 처리 지연 확인
- **MongoDB Query Latency**: 조회 성능 확인
- **Batch Processing Time**: 배치당 처리 시간
- **DLQ Count**: 실패 메시지 수

---

## 📝 설계 문서 변경 사항

### CLEANING_SYSTEM_DESIGN.md
- Consumer 부분: "Kafka Batch Listener 사용" 명시
- 성능 최적화 섹션 추가

### cleaning_system_sequence.md
- Step 1: "100개씩 배치로 수신" 추가
- Step 2: "일괄 조회 (findAllByDataIdIn)" 추가
- Step 5: "일괄 저장 (saveAll)" 추가

---

## 🔮 향후 고려사항

### Spring Batch 도입 시나리오
만약 다음 상황이 발생하면 Spring Batch 재검토:

1. **처리량이 1000 msg/sec 초과**
2. **Real-time 요구사항 완화** (예: 배치 분석용)
3. **복잡한 재처리 로직 필요**
4. **대용량 히스토리 데이터 일괄 재처리**

→ 현재는 Kafka Batch Listener로 충분

---

> **결론**: Real-time Search Keyword Service의 특성상 **Kafka Batch Listener 방식**이 최적.
> 구현 간단, 실시간 유지, 충분한 성능 제공.

> **Version:** 1.0
> **Last Updated:** 2025-10-28