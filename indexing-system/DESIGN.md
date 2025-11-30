# Indexing System 설계 문서

## 목차
1. [개요](#1-개요)
2. [Elasticsearch 기초 개념](#2-elasticsearch-기초-개념)
3. [NER (개체명 인식)](#3-ner-개체명-인식)
4. [시스템 아키텍처](#4-시스템-아키텍처)
5. [데이터 흐름](#5-데이터-흐름)
6. [핵심 기능 상세](#6-핵심-기능-상세)
7. [디렉토리 구조](#7-디렉토리-구조)
8. [Elasticsearch 인덱스 설계](#8-elasticsearch-인덱스-설계)
9. [Redis 캐시 설계](#9-redis-캐시-설계)
10. [구현 계획](#10-구현-계획)
11. [참고 자료](#10-참고-자료)

---

## 1. 개요

### 1.1 목적
Indexing System은 Cleaning System에서 정제된 데이터를 받아 **형태소 분석**을 통해 명사를 추출하고, **빈도수 기반 가중치**를 계산하여 Elasticsearch에 색인하는 시스템입니다.

### 1.2 주요 기능
| 기능 | 설명 |
|------|------|
| 명사 추출 | 한글 형태소 분석을 통해 명사만 추출 |
| 빈도수 계산 | 키워드 출현 빈도를 계산하여 가중치 부여 |
| ES 색인 | 분석된 데이터를 Elasticsearch에 저장 |
| 캐시 관리 | 일별 Top 10 키워드를 Redis에 캐시 |

### 1.3 입출력
```
[입력] Kafka: cleaning-to-indexing 토픽
       └─ CleaningPayload (dataId, cleanedContent, metadata)

[출력] Elasticsearch: keyword_index
       └─ 키워드별 빈도수, 가중치, 출처 정보
       
       Redis: daily_top_keywords
       └─ 일별 Top 10 키워드 캐시
```

---

## 2. Elasticsearch 기초 개념

### 2.1 Elasticsearch란?
Elasticsearch는 **분산형 검색 및 분석 엔진**입니다. JSON 문서를 저장하고, 풀텍스트 검색, 구조화된 검색, 분석 기능을 제공합니다.

### 2.2 핵심 용어

| 용어 | RDBMS 비유 | 설명 |
|------|-----------|------|
| **Index** | Database | 문서의 논리적 컬렉션 |
| **Document** | Row | JSON 형태의 데이터 단위 |
| **Field** | Column | 문서 내 개별 데이터 항목 |
| **Mapping** | Schema | 문서의 구조 정의 |
| **Shard** | Partition | 인덱스를 분산 저장하는 단위 |
| **Replica** | Replica | 샤드의 복제본 (고가용성) |

### 2.3 Analyzer (분석기)
Elasticsearch는 텍스트를 색인하기 전에 **Analyzer**를 통해 처리합니다.

```
원본 텍스트 → Character Filter → Tokenizer → Token Filter → 색인
```

**분석 과정 예시:**
```
"삼성전자가 신제품을 발표했다"
    ↓ (한글 형태소 분석기)
["삼성전자", "신제품", "발표"] (명사만 추출)
```

### 2.4 한글 형태소 분석기

| 분석기 | 특징 | 설치 방법 |
|--------|------|-----------|
| **Nori** | ES 공식 한글 플러그인, 기본 제공 | `bin/elasticsearch-plugin install analysis-nori` |
| **Arirang** | 한국어 특화, 커스터마이징 용이 | 별도 설치 필요 |
| **OpenKoreanText** | 트위터 개발, SNS 텍스트에 강점 | 별도 설치 필요 |

**우리 프로젝트에서는 Nori 분석기를 사용합니다** (ES 공식 지원, 설치 간편)

### 2.5 Nori 분석기 설정 예시
```json
{
  "settings": {
    "analysis": {
      "tokenizer": {
        "nori_tokenizer": {
          "type": "nori_tokenizer",
          "decompound_mode": "mixed"
        }
      },
      "filter": {
        "nori_pos_filter": {
          "type": "nori_part_of_speech",
          "stoptags": ["E", "J", "SC", "SE", "SF", "VCN", "VCP", "VX"]
        }
      },
      "analyzer": {
        "korean_noun_analyzer": {
          "type": "custom",
          "tokenizer": "nori_tokenizer",
          "filter": ["nori_pos_filter", "lowercase"]
        }
      }
    }
  }
}
```

**Nori 품사 태그 설명:**
- `E`: 어미 (제외)
- `J`: 조사 (제외)
- `NNG`: 일반 명사 (포함)
- `NNP`: 고유 명사 (포함)
- `VV`: 동사 (제외)

---

## 3. NER (개체명 인식)

### 3.1 형태소 분석 vs NER

| 구분 | 형태소 분석 | NER (개체명 인식) |
|------|------------|-------------------|
| **목적** | 문장을 형태소 단위로 분리 | 고유명사(인물, 기관, 지역 등) 인식 |
| **예시** | "삼성전자" → "삼성" + "전자" | "삼성전자" → ORG (기관) |
| **도구** | KOMORAN, Nori | BERT 기반 NER 모델 |
| **사전 필요** | 사용자 사전 직접 관리 필요 | 사전학습 모델이 자동 인식 |

**프로젝트 요구사항:**
- 뉴스/위키 데이터에서 **기업명, 인물명** 등을 정확히 인식해야 함
- 수십만 개의 고유명사를 사용자 사전으로 관리하는 것은 비현실적
- → **NER(개체명 인식) 모델 필수**

### 3.2 NER 태그 (KLUE 표준)

koorukuroo/korean_bert_ner 모델이 출력하는 KLUE 기반 개체명 태그:

| 태그 | 의미 | 예시 |
|------|------|------|
| **PER** | 인물 (Person) | 이재용, 윤석열, BTS |
| **ORG** | 기관 (Organization) | 삼성전자, 카카오, 국회 |
| **LOC** | 지역 (Location) | 서울, 강남구, 미국 |
| **DAT** | 날짜 (Date) | 2024년 1월, 어제 |
| **TIM** | 시간 (Time) | 오후 3시, 아침 |
| **QT** | 수량 (Quantity) | 100만원, 3개 |
| **EV** | 이벤트 (Event) | 월드컵, 올림픽 |
| **AF** | 인공물 (Artifact) | 갤럭시, 아이폰 |
| **CV** | 문명 (Civilization) | 불교, 한국어 |
| **FD** | 학문분야 (Field) | 물리학, 경제학 |
| **TR** | 이론 (Theory) | 상대성이론 |
| **AM** | 동물 (Animal) | 호랑이, 코끼리 |
| **PT** | 식물 (Plant) | 소나무, 장미 |
| **MT** | 물질 (Material) | 금, 석유 |
| **TM** | 용어 (Term) | AI, 블록체인 |

### 3.3 NER 모델 선택

#### 2025년 기준 한국어 NER 모델 성능 비교

| 모델 | 데이터셋 | Entity F1 | 특징 |
|------|----------|-----------|------|
| **klue/roberta-large** | KLUE NER | ~90.8% | 한국어 특화, Fine-tuning 필요 |
| **koorukuroo/korean_bert_ner** | Naver NER | **88.43%** | ✅ 바로 사용 가능, 21개 태그 |
| KLUE-BERT-base | KLUE NER | 83.97% | 공식 베이스라인 |
| taeminlee/gliner_ko | KONNE | 75.99% | Zero-shot 가능 |
| Pororo | KONNE | 63.50% | ❌ 서비스 종료 |

#### 선택: `koorukuroo/korean_bert_ner`

| 항목 | 내용 |
|------|------|
| 모델 | KLUE BERT 기반 NER (이미 Fine-tuned) |
| 성능 | **Entity F1 88.43%** |
| 태그 | 21개 (PER, ORG, LOC, DAT, TIM 등) |
| 장점 | 바로 사용 가능, 높은 정확도 |
| 단점 | Python 서버 별도 운영 필요 |

**태그별 성능:**

| 태그 | 의미 | F1 |
|------|------|-----|
| PER | 인물명 | 94% |
| ORG | 기관/기업 | 88% |
| LOC | 지역 | 74% |
| DAT | 날짜 | 96% |
| TIM | 시간 | 86% |

### 3.4 NER 구현 방식: 자체 NER 서버

HuggingFace의 **사전학습된 한국어 NER 모델**을 활용한 자체 서버 구축

| 항목 | 내용 |
|------|------|
| 모델 | `koorukuroo/korean_bert_ner` |
| 통신 | **gRPC** (REST 대비 2~10배 빠름) |
| 장점 | 무제한 호출, 높은 성능 |
| 대용량 처리 | ✅ 배치 처리, GPU 활용 가능 |

### 3.5 권장 아키텍처: gRPC 기반 NER 서버

```
┌─────────────────────────────────────────────────────────────────┐
│                        Indexing System                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐       │
│  │    Kafka     │───▶│   Indexing   │───▶│Elasticsearch │       │
│  │   Consumer   │    │   Service    │    │              │       │
│  └──────────────┘    └──────┬───────┘    └──────────────┘       │
│                             │                                    │
│                             │ gRPC (HTTP/2)                      │
│                             ▼                                    │
│                    ┌──────────────────┐                         │
│                    │   NER Server     │  ← Python (gRPC)        │
│                    │ (korean_bert_ner)│                         │
│                    └──────────────────┘                         │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

#### gRPC vs REST 비교

| 항목 | gRPC | REST |
|------|------|------|
| 프로토콜 | HTTP/2 | HTTP/1.1 |
| 데이터 형식 | Protocol Buffers (바이너리) | JSON (텍스트) |
| 속도 | **2~10배 빠름** | 상대적으로 느림 |
| 스트리밍 | 양방향 지원 | 제한적 |
| 타입 안정성 | 강타입 (.proto) | 런타임 검증 |

**대용량 Wiki 데이터 처리에 gRPC가 적합한 이유:**
- 바이너리 직렬화로 네트워크 오버헤드 감소
- 스트리밍으로 배치 처리 효율화
- 연결 재사용 (HTTP/2 멀티플렉싱)

### 3.6 NER 통신 방식: gRPC

**gRPC 프로토콜 정의 필요:**
- Protocol Buffers로 인터페이스 정의 (ner.proto)
- 단일 요청/응답 및 스트리밍 배치 지원

**Java 클라이언트 구현:**
- gRPC stub을 통한 NER 서버 호출
- 동기/비동기 호출 지원

### 3.7 NER 모델 운영 가이드

**선택된 모델:** `koorukuroo/korean_bert_ner`

| 환경 | GPU | 배치 크기 | 예상 처리량 |
|------|-----|----------|-------------|
| 개발/테스트 | CPU | 1 | ~10 req/s |
| 운영 (소규모) | GPU (T4) | 32 | ~200 req/s |
| 운영 (대규모) | GPU (A100) | 64 | ~500 req/s |

**권장 설정:**
- **개발**: CPU, 단일 요청
- **운영**: GPU + gRPC 스트리밍 배치

### 3.8 대용량 처리 전략 (Wiki 데이터)

Wiki 데이터는 수십만 건 이상이므로 효율적인 처리 전략 필요:

```
┌─────────────────────────────────────────────────────────────┐
│                    대용량 NER 처리 파이프라인                  │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  1. Kafka Consumer (배치 수집)                               │
│     └─ 100건씩 묶어서 처리                                   │
│                                                              │
│  2. NER 서버 배치 호출 (gRPC 스트리밍)                        │
│     └─ GPU 병렬 처리                                         │
│                                                              │
│  3. 결과 집계                                                │
│     └─ 키워드별 빈도수 계산                                  │
│     └─ ES Bulk API로 일괄 색인                              │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

**배치 처리 설정:**
- Kafka Consumer: 100건씩 배치 수집
- NER 서버: gRPC 스트리밍으로 처리
- ES Bulk API: 일괄 색인

### 3.9 NER 서버 Docker 구성

**필수 구성:**
- Python 3.10+
- PyTorch, Transformers
- gRPC
- GPU 지원 (선택사항)

**Docker 이미지:**
- `koorukuroo/korean_bert_ner` 모델 사전 다운로드
- gRPC 서버 구동 (포트 50051)

---

## 4. 시스템 아키텍처

### 4.1 전체 흐름
```
┌─────────────────────────────────────────────────────────────────┐
│                      Indexing System                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐      │
│  │    Kafka     │───▶│   Indexing   │───▶│Elasticsearch │      │
│  │   Consumer   │    │   Service    │    │   (색인)     │      │
│  └──────────────┘    └──────────────┘    └──────────────┘      │
│                             │                    │               │
│                             │                    ▼               │
│                             │            ┌──────────────┐       │
│                             └───────────▶│    Redis     │       │
│                              (캐시 갱신)  │   (캐시)     │       │
│                                          └──────────────┘       │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 4.2 컴포넌트 역할

| 컴포넌트 | 역할 |
|----------|------|
| **Kafka Consumer** | `cleaning-to-indexing` 토픽에서 메시지 소비 |
| **Indexing Service** | 비즈니스 로직 (명사 추출, 빈도 계산, 가중치 부여) |
| **Elasticsearch** | 키워드 색인 및 검색 |
| **Redis** | Top 10 키워드 캐시 (TTL: 24시간) |

---

## 5. 데이터 흐름

### 5.1 Kafka 메시지 수신

**cleaning-system이 전송하는 메시지 (메타데이터만):**
```json
{
  "version": "1.0",
  "messageId": "uuid-xxx",
  "timestamp": "2025-01-01T12:00:00",
  "eventType": "DATA_CLEANED",
  "payload": {
    "dataId": "https://www.mk.co.kr/news/...",
    "source": "rss",
    "mongoCollectionName": "cleaned_data",
    "priority": "NORMAL",
    "sourceDetails": null
  },
  "retryCount": 0
}
```

**indexing-system이 할 일:**
1. Kafka 메시지에서 `dataId` 추출
2. MongoDB `cleaned_data` 컬렉션에서 해당 문서 조회
3. 조회한 데이터에서 `cleanedTitle`, `cleanedContent` 사용

### 5.2 처리 단계
```
1. Kafka 메시지 수신 (메타데이터만)
   - payload에서 dataId, source, mongoCollectionName 추출
       ↓
2. MongoDB에서 실제 데이터 조회
   - collection: cleaned_data
   - query: { dataId: "..." }
   - 조회 결과: { cleanedTitle, cleanedContent, metadata, ... }
       ↓
3. NER 서버 호출
   - 제목 + 본문에서 개체명 추출
   - 결과: [
       {word: "삼성전자", entity: "ORG"},
       {word: "이재용", entity: "PER"},
       {word: "서울", entity: "LOC"}
     ]
       ↓
4. 빈도수 계산
   - 개체명별 출현 횟수 집계
   - 결과: {"삼성전자": 5, "이재용": 3, ...}
       ↓
5. 가중치 계산
   - 빈도수 기반 점수 산정
   - 제목 키워드 가중치 2배
   - 개체 유형별 가중치 (기관 1.5, 인물 1.2 등)
       ↓
6. Elasticsearch 색인
   - 키워드 문서 upsert
   - 개체 유형 정보 포함
       ↓
7. Redis 캐시 갱신
   - Top 10 키워드 업데이트 (비동기)
```

---

## 6. 핵심 기능 상세

### 6.1 개체명 추출 (NER)

**구현 방식: gRPC를 통한 NER 서버 호출**

- Python NER 서버를 gRPC로 호출하여 개체명 추출
- 신뢰도 80% 이상 필터링
- PER(인물), ORG(기관), LOC(지역), AF(인공물) 등 주요 개체 추출

### 6.2 빈도수 계산 및 가중치 부여

**가중치 계산 공식:**
```
Score = (titleFreq × 2.0 × entityWeight) + (contentFreq × 1.0 × entityWeight)
```

| 출처 | 가중치 | 이유 |
|------|--------|------|
| 제목 (title) | 2.0 | 핵심 키워드가 제목에 포함됨 |
| 본문 (content) | 1.0 | 기본 가중치 |

| 개체 유형 | 가중치 | 이유 |
|-----------|--------|------|
| ORG (기관) | 1.5 | 뉴스에서 기업/기관명 중요 |
| PER (인물) | 1.3 | 인물 관련 뉴스 중요 |
| LOC (지역) | 1.0 | 기본 가중치 |
| AF (인공물) | 1.2 | 제품명 등 |

**빈도수 집계 로직:**
- 개체명별 출현 횟수 계산
- 제목 가중치: 2.0 × 개체 유형 가중치
- 본문 가중치: 1.0 × 개체 유형 가중치
- 동일 키워드는 점수 누적

### 6.3 Elasticsearch 색인

**색인 전략: Upsert (Update or Insert)**

- 동일 키워드가 여러 문서에서 등장하면 점수 누적
- Painless 스크립트로 기존 문서 업데이트
- 일별 키워드 집계 (keywordId: date_keyword)

### 6.4 Redis 캐시

**캐시 키 설계:**
```
키: daily_top_keywords:{yyyy-MM-dd}
값: List<KeywordScore> (Top 10)
TTL: 25시간 (하루 + 1시간 버퍼)
```

**캐시 갱신 전략:**
1. 새로운 데이터 색인 후 비동기로 Top 10 재계산
2. ES aggregation 쿼리로 일별 Top 10 조회
3. Redis에 결과 캐시 (TTL: 25시간)

---

## 7. 디렉토리 구조

DDD 원칙에 따른 indexing-system 구조:

```
indexing-system/
├── src/main/java/javacafe/realtime_sujeong/indexing/
│   │
│   ├── IndexingSystemApplication.java      # 메인 애플리케이션
│   │
│   ├── keyword/                            # 키워드 바운디드 컨텍스트
│   │   ├── domain/                         # 도메인 레이어
│   │   │   ├── Keyword.java               # 키워드 도메인 모델
│   │   │   ├── KeywordScore.java          # 키워드 점수 VO
│   │   │   ├── NamedEntity.java           # 개체명 VO
│   │   │   └── KeywordRepository.java     # ES Repository
│   │   │
│   │   ├── service/                        # 애플리케이션 서비스
│   │   │   ├── KeywordIndexingService.java    # 색인 서비스
│   │   │   └── KeywordScoreCalculator.java    # 점수 계산기
│   │   │
│   │   └── dto/                            # DTO
│   │       ├── KeywordDocument.java        # ES 문서 DTO
│   │       └── KeywordScoreDto.java        # 점수 조회 DTO
│   │
│   ├── ner/                                # NER 바운디드 컨텍스트
│   │   ├── client/
│   │   │   └── NerClient.java             # NER 서버 HTTP 클라이언트
│   │   ├── dto/
│   │   │   ├── NerRequest.java            # NER 요청 DTO
│   │   │   ├── NerResponse.java           # NER 응답 DTO
│   │   │   └── NerBatchRequest.java       # 배치 요청 DTO
│   │   └── service/
│   │       └── NerService.java            # NER 서비스 (클라이언트 래퍼)
│   │
│   ├── cache/                              # 캐시 바운디드 컨텍스트
│   │   ├── service/                        
│   │   │   └── KeywordCacheService.java    # Redis 캐시 서비스
│   │   └── dto/
│   │       └── CachedTopKeywords.java      # 캐시 데이터 DTO
│   │
│   ├── kafka/                              # Kafka 컨슈머
│   │   ├── consumer/
│   │   │   └── CleaningDataConsumer.java   # cleaning-to-indexing 컨슈머
│   │   └── dto/
│   │       └── CleaningPayload.java        # 수신 메시지 DTO
│   │
│   └── common/                             # 공통 모듈
│       ├── config/
│       │   ├── ElasticsearchConfig.java    # ES 설정
│       │   ├── RedisConfig.java            # Redis 설정
│       │   ├── RestTemplateConfig.java     # HTTP 클라이언트 설정
│       │   └── KafkaConsumerConfig.java    # Kafka Consumer 설정
│       └── exception/
│           └── IndexingException.java      # 커스텀 예외
│
├── src/main/resources/
│   ├── application.properties
│   ├── application-dev.properties
│   └── elasticsearch/
│       └── keyword-index-mapping.json      # ES 인덱스 매핑 설정
│
├── src/test/java/javacafe/realtime_sujeong/indexing/
│   ├── keyword/
│   │   ├── service/
│   │   │   └── KeywordIndexingServiceTest.java
│   │   └── domain/
│   │       └── KeywordTest.java
│   ├── ner/
│   │   └── NerClientTest.java
│   └── kafka/
│       └── CleaningDataConsumerTest.java
│
├── build.gradle
└── DESIGN.md                               # 이 문서

# NER 서버 (별도 디렉토리)
ner-server/
├── ner.proto                               # gRPC 인터페이스 정의
├── ner_server.py                           # gRPC 서버
├── requirements.txt                        # Python 의존성
├── Dockerfile                              # Docker 빌드
└── README.md                               # NER 서버 가이드
```

---

## 8. Elasticsearch 인덱스 설계

### 8.1 인덱스 매핑

**인덱스명:** `keyword_index`

```json
{
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 1,
    "analysis": {
      "tokenizer": {
        "nori_tokenizer": {
          "type": "nori_tokenizer",
          "decompound_mode": "mixed",
          "discard_punctuation": true
        }
      },
      "filter": {
        "nori_pos_filter": {
          "type": "nori_part_of_speech",
          "stoptags": [
            "E", "IC", "J", "MAG", "MAJ", "MM", 
            "SC", "SE", "SF", "SP", "SSC", "SSO", 
            "SY", "UNA", "VCN", "VCP", "VSV", "VV", "VX", "XPN", "XSA", "XSN", "XSV"
          ]
        }
      },
      "analyzer": {
        "korean_noun_analyzer": {
          "type": "custom",
          "tokenizer": "nori_tokenizer",
          "filter": ["nori_pos_filter", "lowercase", "nori_readingform"]
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "keywordId": {
        "type": "keyword",
        "doc_values": true
      },
      "keyword": {
        "type": "text",
        "analyzer": "korean_noun_analyzer",
        "fields": {
          "raw": {
            "type": "keyword"
          }
        }
      },
      "score": {
        "type": "double"
      },
      "frequency": {
        "type": "integer"
      },
      "date": {
        "type": "date",
        "format": "yyyy-MM-dd"
      },
      "sourceDataIds": {
        "type": "keyword"
      },
      "sources": {
        "type": "keyword"
      },
      "createdAt": {
        "type": "date",
        "format": "strict_date_time"
      },
      "lastUpdated": {
        "type": "date",
        "format": "strict_date_time"
      }
    }
  }
}
```

### 8.2 문서 구조

```json
{
  "keywordId": "2025-01-01_삼성전자",
  "keyword": "삼성전자",
  "score": 125.5,
  "frequency": 42,
  "date": "2025-01-01",
  "sourceDataIds": ["sha256-abc", "sha256-def"],
  "sources": ["rss", "wiki"],
  "createdAt": "2025-01-01T00:05:00Z",
  "lastUpdated": "2025-01-01T23:55:00Z"
}
```

### 8.3 주요 쿼리

**일별 Top 10 키워드 조회:**
```json
{
  "size": 0,
  "query": {
    "term": {
      "date": "2025-01-01"
    }
  },
  "aggs": {
    "top_keywords": {
      "terms": {
        "field": "keyword.raw",
        "size": 10,
        "order": {
          "total_score": "desc"
        }
      },
      "aggs": {
        "total_score": {
          "sum": {
            "field": "score"
          }
        }
      }
    }
  }
}
```

---

## 9. Redis 캐시 설계

### 9.1 캐시 구조

| 키 패턴 | 값 타입 | TTL | 용도 |
|---------|---------|-----|------|
| `daily_top_keywords:{date}` | List<KeywordScore> | 25h | 일별 Top 10 |
| `keyword_score:{date}:{keyword}` | Double | 25h | 개별 키워드 점수 |

### 9.2 캐시 데이터 형식

```json
{
  "date": "2025-01-01",
  "keywords": [
    {"rank": 1, "keyword": "삼성전자", "score": 1250.5},
    {"rank": 2, "keyword": "현대차", "score": 980.3},
    {"rank": 3, "keyword": "카카오", "score": 875.0},
    ...
  ],
  "cachedAt": "2025-01-01T23:55:00Z"
}
```

### 9.3 캐시 갱신 정책

1. **Write-Through**: 새 데이터 색인 시 캐시 무효화
2. **Lazy Refresh**: 캐시 미스 시 ES에서 조회 후 캐시
3. **Scheduled Refresh**: 매 정시(00분)에 Top 10 재계산

---

## 10. 구현 계획

### Phase 1: NER 서버 구축 (Python) ✅
- [x] Protocol Buffers 정의 (`protos/ner.proto`)
- [x] gRPC 서버 구현 (`app/server.py`)
- [x] NER 모델 레이어 (`app/models/ner_model.py`)
- [x] gRPC 서비스 레이어 (`app/services/ner_servicer.py`)
- [x] 단일/배치 처리 엔드포인트 (ExtractEntities, ExtractEntitiesBatch)
- [x] 테스트 코드 작성 (pytest)
- [x] Docker 이미지 빌드 (Dockerfile)
- [x] docker-compose 설정 (ner-server 서비스)

### Phase 2: 기본 구조 설정 (Java) ✅
- [x] 디렉토리 구조 생성 (DDD 패턴)
- [x] build.gradle 의존성 추가 (gRPC, Elasticsearch, Redis, Kafka, MongoDB)
- [x] IndexingSystemApplication.java 메인 클래스 생성
- [x] application.properties 설정 (환경변수 지원)
- [x] application-dev.properties 설정 (개발 서버 MongoDB 주소)

### Phase 3: NER 클라이언트 ✅
- [x] build.gradle에 protobuf 플러그인 추가
- [x] ner.proto 파일 위치 설정 (src/main/proto)
- [x] NerClientConfig 구현 (gRPC ManagedChannel 설정)
- [x] NamedEntity, NerResult DTO 구현
- [x] NerGrpcClient 구현 (gRPC 통신)
- [x] NerService 구현 (비즈니스 로직 래퍼)

### Phase 4: Elasticsearch 설정 ✅
- [x] ElasticsearchConfig 구현 (ClientConfiguration 기반)
- [x] 인덱스 매핑 JSON 작성 (개체 유형 필드 포함, Nori 분석기 설정)
- [x] ElasticsearchInitializer 구현 (인덱스 자동 생성)
- [x] application.properties Elasticsearch 설정 추가
- [x] application-dev.properties Elasticsearch 설정 추가

### Phase 5: 도메인 레이어 ✅
- [x] Keyword 도메인 모델 (@Document, Elasticsearch 매핑)
- [x] KeywordScore VO (점수 계산 결과)
- [x] KeywordRepository (ElasticsearchRepository 확장)
- [x] KeywordDocument DTO (ES 문서 조회/색인용)
- [x] KeywordScoreDto (API 응답용)

### Phase 6: 서비스 레이어 ✅
- [x] KeywordScoreCalculator (점수 계산 로직, 가중치 적용)
- [x] KeywordIndexingService (색인 서비스, Upsert 로직)
- [x] 제목/본문 빈도수 분리 계산
- [x] 개체 유형별 가중치 적용 (ORG: 1.5, PER: 1.3, AF: 1.2, LOC: 1.0)
- [x] 배치 처리 지원

### Phase 7: Kafka 연동 ✅
- [x] CleanedData 도메인 모델 (MongoDB cleaned_data 컬렉션)
- [x] CleanedDataRepository (MongoDB Repository)
- [x] CleaningDataConsumer (Kafka 컨슈머, 배치 처리)
- [x] KafkaConsumerConfig (배치 리스너, 수동 커밋)
- [x] MongoConfig (Repository 활성화)
- [x] cleaning-to-indexing 토픽 소비
- [x] MongoDB 조회 → NER → Elasticsearch 색인 파이프라인

### Phase 8: Redis 캐시
- [ ] RedisConfig
- [ ] KeywordCacheService

### Phase 9: 테스트 및 검증
- [ ] NER 서버 테스트
- [ ] 단위 테스트
- [ ] 통합 테스트
- [ ] 대용량 배치 테스트 (Wiki 데이터)

---

## 11. 참고 자료

### 11.1 공식 문서
- [Elasticsearch 공식 문서](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- [Spring Data Elasticsearch](https://docs.spring.io/spring-data/elasticsearch/reference/)
- [HuggingFace Transformers](https://huggingface.co/docs/transformers)
- [KLUE Benchmark](https://github.com/KLUE-benchmark/KLUE)
- [KLUE-RoBERTa Models](https://huggingface.co/klue)
- [FastAPI 공식 문서](https://fastapi.tiangolo.com/)

### 11.2 관련 의존성

```groovy
// build.gradle
dependencies {
    // Elasticsearch
    implementation 'org.springframework.boot:spring-boot-starter-data-elasticsearch'
    
    // Redis
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    
    // Kafka
    implementation 'org.springframework.kafka:spring-kafka'
    
    // Common 모듈
    implementation project(':common')
}
```

### 11.3 Docker 환경

```yaml
# docker-compose.yml (추가 필요)
elasticsearch:
  image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
  environment:
    - discovery.type=single-node
    - xpack.security.enabled=false
  ports:
    - "9200:9200"

redis:
  image: redis:7-alpine
  ports:
    - "6379:6379"
```

---

## 변경 이력

| 버전 | 날짜 | 작성자 | 내용 |
|------|------|--------|------|
| 1.0 | 2025-11-23 | Claude | 최초 작성 |
| 1.1 | 2025-11-24 | Claude | NER 섹션 추가, 형태소 분석 → NER 기반으로 변경 |
| 1.2 | 2025-11-24 | Claude | NER 모델 `koorukuroo/korean_bert_ner` 선정 (F1 88.43%), gRPC 통신 방식 적용 |
| 1.3 | 2025-11-30 | Claude | Phase 1 (NER 서버) 완료, gRPC 기반 구현 |
