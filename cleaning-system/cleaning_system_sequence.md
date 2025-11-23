# 🧩 정제 시스템(Cleaning System) 설계 문서

## ⚙️ 기술 스택
- **언어/프레임워크:** Java / Spring Boot
- **메시지 큐:** Kafka
- **텍스트 추출기:** Apache Tika
- **데이터 저장소:** MongoDB

---

## 🔁 정제 시스템 처리 단계

### **1️⃣ Kafka Consumer - 배치 데이터 수신 (성능 최적화)**
- Cleaning System은 `collection-to-cleaning` 토픽을 구독하여 Collection System으로부터 메타데이터를 **배치로** 전달받는다.
- **Kafka Batch Listener 사용**: 한 번에 100개씩 메시지를 수신하여 처리
- 각 메시지에는 다음 정보가 포함됨:
  - `dataId`, `source`, `mongoCollectionName`, `priority`, `sourceDetails`
- Consumer 그룹(`cleaning-group`)으로 구성되어 병렬 처리가 가능하다.
- **배치 설정**: `max-poll-records: 100`, `fetch-min-size: 1MB`

```
[Kafka: collection-to-cleaning]
        ↓ (100개씩 배치)
[Cleaning Batch Consumer]
```

---

### **2️⃣ MongoDB 조회 - 원본 데이터 일괄 획득 (Bulk Operation)**
- 배치로 전달받은 메시지에서 모든 `dataId`를 추출
- `source` 값에 따라 적절한 Fetcher 선택 (Factory Pattern):
  - `rss` → `RssRawDataFetcher.fetchContentBatch()` → `rss_raw_data` 컬렉션 **일괄 조회**
  - `wiki` → `WikiRawDataFetcher.fetchContentBatch()` → `wiki_raw_data` 컬렉션 **일괄 조회**
- **MongoDB Bulk Query**: `findAllByDataIdIn(List<String> dataIds)` 사용
- **성능 향상**: 100개 dataId를 1번의 조회로 처리 (기존 100회 → 1회)

```
Cleaning Batch Consumer
    ↓ (dataId 리스트 추출)
RawDataFetcherFactory
    ↓ (source별 Fetcher 선택)
MongoDB Bulk Query (findAllByDataIdIn)
    ↓
MongoDB (rss_raw_data or wiki_raw_data)
```

---

### **3️⃣ Apache Tika - 텍스트 추출 단계 (Wiki만)**
- **RSS 데이터**: Collection System에서 이미 Jsoup으로 크롤링 완료 → **Tika 사용 안 함**
- **Wiki 데이터**: wikitext 형식 → Apache Tika로 plain text 추출

```
WikiRawDataFetcher → Apache Tika → Plain Text
RssRawDataFetcher → (이미 크롤링된 content 사용)
```

---

### **4️⃣ Normalization - 데이터 정제 및 구조화**
- 추출된 텍스트를 표준 스키마로 정제한다.
- 주요 작업:
  - 언어 감지(`ko`, `en`, `ja` 등)
  - 특수문자, 공백, HTML 태그 제거
  - JSON Schema에 맞게 필드 구성

```json
{
  "docId": "uuid",
  "source": "crawlerA",
  "url": "https://example.com",
  "title": "문서 제목",
  "content": "정제된 본문 텍스트",
  "language": "ko",
  "metadata": {
    "mime_type": "text/html",
    "length": 1423
  },
  "created_at": "2025-10-27T12:00:00Z"
}
```

---

### **5️⃣ MongoDB 저장 - 정제 데이터 일괄 저장 (Bulk Insert)**
- 정제 완료된 문서 리스트를 `cleaned_data` 컬렉션에 **일괄 저장**
- 원본 데이터(`rss_raw_data`, `wiki_raw_data`)와의 관계는 `dataId`로 연결
- **MongoDB Bulk Operation**: `saveAll(List<CleanedData>)` 사용
- Upsert 방식으로 저장 (동일 `dataId` 재처리 시 갱신)
- **성능 향상**: 100개 문서를 1번의 저장으로 처리 (기존 100회 → 1회)

```
Cleaning Service
    ↓ (List<CleanedData>)
MongoDB.cleaned_data.saveAll()
```

---

### **6️⃣ Kafka Producer - 정제 완료 메시지 배치 전송**
- 정제 완료된 데이터 리스트를 `cleaning-to-indexing` 토픽으로 **배치 전송**
- 각 CleaningPayload 포함:
  - `dataId`, `source`, `title`, `cleanedContent`, `language`, `metadata`
- **배치 발행**: 여러 메시지를 한 번에 전송하여 네트워크 overhead 감소

```
Cleaning Service
    ↓ (List<KafkaMessage<CleaningPayload>>)
Kafka.cleaning-to-indexing (배치 전송)
```

---

### **7️⃣ 에러 처리 및 재처리 관리 (DLQ)**
- 정제 중 예외 발생 시 Kafka의 `dead-letter-queue` 토픽으로 메시지를 전송.
- 재시도 정책: 3회 재시도 + 백오프

```
Cleaning Service
    ↓ (Exception 발생)
Kafka.dead-letter-queue
```

---

### **8️⃣ 로깅 및 모니터링**
- Logback을 통한 단계별 로깅 (성공/실패/처리 시간)
- 주요 모니터링 항목:
  - `cleaning_processing_time_ms`
  - `mongodb_query_latency_ms`
  - `tika_processing_time_ms` (Wiki만)
  - `dlq_message_count`

---

## 🧠 전체 시퀀스 다이어그램 (배치 처리 방식)

```
Kafka(collection-to-cleaning)
     │  List<KafkaMessage<CollectionPayload>> (100개)
     ▼
Cleaning Batch Consumer
     │  consumeBatch(List<KafkaMessage>)
     │  → dataIds 추출 (List<String>)
     ▼
RawDataFetcherFactory
     │  getFetcher(source)
     ▼
MongoDB Bulk Query (rss_raw_data or wiki_raw_data)
     │  findAllByDataIdIn(dataIds) → 1회 조회로 100개 데이터 획득
     ▼
[For Each Data in Batch]
     ├─ Apache Tika (Wiki만)
     │   │  extractText(wikitext) → plain text
     │   ▼
     └─ Normalizer
         │  clean/standardize text, detect language
         ▼
[배치 처리 완료]
     ▼
MongoDB Bulk Insert (cleaned_data)
     │  saveAll(List<CleanedData>) → 1회 저장으로 100개 저장
     ▼
Kafka Producer (cleaning-to-indexing)
     │  produceBatch(List<KafkaMessage<CleaningPayload>>)
     ▼
Kafka(dead-letter-queue) ← [if Exception after 3 retries]
```

**성능 개선 효과**:
- MongoDB 조회: 100회 → 1회 (100x 개선)
- MongoDB 저장: 100회 → 1회 (100x 개선)
- Kafka 발행: 100회 → 1회 (네트워크 overhead 감소)
- **예상 처리량**: 20 msg/sec → 200-500 msg/sec

---

## ✅ 최종 요약
| 단계 | 모듈 | 주요 역할 | 최적화 방식 |
|------|------|------------|------------|
| 1 | Kafka Batch Consumer | Collection System으로부터 메타데이터 **배치 수신** | 100개씩 배치 처리 |
| 2 | RawDataFetcher (Factory Pattern) | MongoDB에서 원본 데이터 **일괄 조회** | Bulk Query (findAllByDataIdIn) |
| 3 | Apache Tika (Wiki만) | wikitext → plain text 변환 | - |
| 4 | Normalizer | 데이터 정규화 및 언어 감지 | 배치 내 병렬 처리 |
| 5 | MongoDB 저장소 (cleaned_data) | 정제된 문서 **일괄 저장** | Bulk Insert (saveAll) |
| 6 | Kafka Producer | Indexing System으로 메시지 **배치 전송** | 배치 발행 |
| 7 | DLQ 처리 | 실패 데이터 재처리 (3회 재시도) | - |
| 8 | 로깅/모니터링 | 성능 및 장애 추적 | Batch 단위 메트릭 수집 |

---

> **Project:** Real-time Search Keyword Service
> **Version:** 2.0.0
> **Last Updated:** 2025-10-28

