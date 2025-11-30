# NER Server 설계 문서

## 목차
1. [개요](#1-개요)
2. [기술 스택](#2-기술-스택)
3. [시스템 아키텍처](#3-시스템-아키텍처)
4. [API 설계](#4-api-설계)
5. [NER 모델](#5-ner-모델)
6. [데이터 모델](#6-데이터-모델)
7. [성능 최적화](#7-성능-최적화)
8. [에러 처리](#8-에러-처리)
9. [배포 전략](#9-배포-전략)
10. [구현 계획](#10-구현-계획)

---

## 1. 개요

### 1.1 목적
NER Server는 **한국어 개체명 인식(Named Entity Recognition)** 서비스를 제공하는 독립적인 Python 서버입니다. indexing-system에서 **gRPC**를 통해 호출하여 텍스트에서 개체명(인물, 기관, 지역 등)을 추출합니다.

### 1.2 주요 기능
| 기능 | 설명 |
|------|------|
| 단일 텍스트 NER | 하나의 텍스트에서 개체명 추출 |
| 배치 NER | 여러 텍스트를 한 번에 처리 (대용량 Wiki 데이터) |
| 신뢰도 필터링 | 신뢰도 80% 이상 결과만 반환 |
| 개체 유형 분류 | PER(인물), ORG(기관), LOC(지역) 등 21개 카테고리 |

### 1.3 입출력
```
[입력] gRPC: NerService.ExtractEntities
       NerRequest {
         text: "삼성전자 이재용 회장이 서울에서 신제품을 발표했다."
         confidence_threshold: 0.8
       }

[출력] gRPC Response
       NerResponse {
         entities: [
           {word: "삼성전자", entity: "ORG", confidence: 0.95, start: 0, end: 4},
           {word: "이재용", entity: "PER", confidence: 0.92, start: 5, end: 8},
           {word: "서울", entity: "LOC", confidence: 0.89, start: 13, end: 15}
         ]
         processing_time_ms: 45
       }
```

---

## 2. 기술 스택

### 2.1 Core Technologies

| 카테고리 | 기술 | 버전 | 용도 |
|----------|------|------|------|
| **언어** | Python | 3.10+ | 서버 구현 |
| **RPC 프레임워크** | gRPC | 1.60+ | 고성능 RPC 통신 (HTTP/2) |
| **프로토콜** | Protocol Buffers | 3.20+ | 인터페이스 정의 및 직렬화 |
| **ML 프레임워크** | PyTorch | 2.1+ | 딥러닝 추론 |
| **NLP 라이브러리** | Transformers | 4.35+ | HuggingFace 모델 |
| **비동기 처리** | asyncio | 표준 라이브러리 | 비동기 gRPC 서버 |

### 2.2 NER 모델
- **모델**: `koorukuroo/korean_bert_ner`
- **베이스**: KLUE BERT
- **성능**: Entity F1 88.43%
- **태그**: 21개 (PER, ORG, LOC, DAT, TIM, QT 등)

### 2.3 개발/배포 도구
| 도구 | 용도 |
|------|------|
| Docker | 컨테이너화 |
| pytest | 테스트 |
| black | 코드 포맷팅 |
| mypy | 타입 체크 (선택사항) |

---

## 3. 시스템 아키텍처

### 3.1 전체 구조
```
┌─────────────────────────────────────────────────────────┐
│                      NER Server                         │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ┌──────────────┐                                       │
│  │ gRPC Server  │  ← gRPC Requests (HTTP/2)             │
│  │  (server.py) │                                       │
│  └──────┬───────┘                                       │
│         │                                                │
│         ▼                                                │
│  ┌──────────────┐                                       │
│  │ NerServicer  │  ← gRPC Service Implementation        │
│  │              │                                       │
│  └──────┬───────┘                                       │
│         │                                                │
│         ▼                                                │
│  ┌──────────────┐                                       │
│  │   NerModel   │  ← Model Loading & Inference          │
│  │ (Singleton)  │                                       │
│  └──────────────┘                                       │
│         │                                                │
│         ▼                                                │
│  ┌──────────────┐                                       │
│  │ BERT Model   │  ← koorukuroo/korean_bert_ner         │
│  │  (PyTorch)   │                                       │
│  └──────────────┘                                       │
│                                                          │
└─────────────────────────────────────────────────────────┘

External:
┌──────────────────┐
│ indexing-system  │ ─── gRPC (Port 50051) ───▶ NER Server
│   (Java)         │
└──────────────────┘
```

### 3.2 컴포넌트 역할

| 컴포넌트 | 파일 | 역할 |
|----------|------|------|
| **Proto Definition** | `protos/ner.proto` | gRPC 인터페이스 정의 (Protocol Buffers) |
| **gRPC Server** | `app/server.py` | gRPC 서버 실행 및 관리 |
| **Service Layer** | `app/services/ner_servicer.py` | gRPC Servicer 구현 (비즈니스 로직) |
| **Model Layer** | `app/models/ner_model.py` | 모델 로딩, 추론 |
| **Config** | `app/config.py` | 설정 관리 |

### 3.3 Request Flow
```
1. gRPC Request (HTTP/2)
   ↓
2. Protocol Buffers Deserialization
   ↓
3. NerServicer.ExtractEntities() / ExtractEntitiesBatch()
   ↓
4. Request Validation (길이, 임계값 체크)
   ↓
5. NerModel.predict()
   ↓
6. BERT Forward Pass
   ↓
7. Post-processing (필터링, 변환)
   ↓
8. Protocol Buffers Serialization
   ↓
9. gRPC Response
```

### 3.4 gRPC vs REST 비교

| 항목 | gRPC | REST |
|------|------|------|
| **프로토콜** | HTTP/2 | HTTP/1.1 |
| **데이터 형식** | Protocol Buffers (바이너리) | JSON (텍스트) |
| **속도** | **2~10배 빠름** | 상대적으로 느림 |
| **네트워크** | 바이너리 직렬화로 오버헤드 감소 | JSON 파싱 오버헤드 |
| **스트리밍** | 양방향 스트리밍 지원 | 제한적 |
| **타입 안정성** | 강타입 (.proto) | 런타임 검증 |
| **대용량 처리** | **배치 스트리밍에 최적** | 비효율적 |

**대용량 Wiki 데이터 처리에 gRPC가 적합한 이유:**
- ✅ 바이너리 직렬화로 네트워크 오버헤드 최소화
- ✅ 스트리밍으로 배치 처리 효율화
- ✅ HTTP/2 멀티플렉싱으로 연결 재사용
- ✅ Indexing System 설계 문서의 권장 아키텍처

---

## 4. API 설계 (gRPC)

### 4.1 gRPC Service 정의

NER Server는 `NerService`라는 gRPC 서비스를 제공하며, 2개의 RPC 메서드를 구현합니다.

| RPC 메서드 | 요청 타입 | 응답 타입 | 설명 |
|-----------|----------|----------|------|
| `ExtractEntities` | `NerRequest` | `NerResponse` | 단일 텍스트 NER |
| `ExtractEntitiesBatch` | `NerBatchRequest` | `NerBatchResponse` | 배치 NER (최대 100개) |

### 4.2 RPC 메서드 상세

#### 4.2.1 ExtractEntities (단일 텍스트 NER)

**요청 (NerRequest):**
```protobuf
message NerRequest {
  string text = 1;                      // 분석할 텍스트 (최대 10,000자)
  float confidence_threshold = 2;        // 신뢰도 임계값 (기본값: 0.8)
}
```

**응답 (NerResponse):**
```protobuf
message NerResponse {
  repeated NerEntity entities = 1;       // 추출된 개체명 리스트
  int32 processing_time_ms = 2;          // 처리 시간 (밀리초)
}

message NerEntity {
  string word = 1;                       // 개체명
  string entity = 2;                     // 개체 유형 (PER, ORG, LOC 등)
  float confidence = 3;                  // 신뢰도 (0.0 ~ 1.0)
  int32 start = 4;                       // 시작 위치
  int32 end = 5;                         // 종료 위치
}
```

**예시:**
```
요청: NerRequest {
  text: "삼성전자 이재용 회장이 서울에서 신제품을 발표했다."
  confidence_threshold: 0.8
}

응답: NerResponse {
  entities: [
    { word: "삼성전자", entity: "ORG", confidence: 0.95, start: 0, end: 4 },
    { word: "이재용", entity: "PER", confidence: 0.92, start: 5, end: 8 },
    { word: "서울", entity: "LOC", confidence: 0.89, start: 13, end: 15 }
  ]
  processing_time_ms: 45
}
```

#### 4.2.2 ExtractEntitiesBatch (배치 NER)

**요청 (NerBatchRequest):**
```protobuf
message NerBatchRequest {
  repeated string texts = 1;             // 분석할 텍스트 리스트 (최대 100개)
  float confidence_threshold = 2;        // 신뢰도 임계값 (기본값: 0.8)
}
```

**응답 (NerBatchResponse):**
```protobuf
message NerBatchResponse {
  repeated NerBatchResult results = 1;   // 각 텍스트별 결과
  int32 total_texts = 2;                 // 처리된 텍스트 개수
  int32 processing_time_ms = 3;          // 총 처리 시간 (밀리초)
}

message NerBatchResult {
  string text = 1;                       // 입력 텍스트
  repeated NerEntity entities = 2;       // 추출된 개체명 리스트
}
```

**예시:**
```
요청: NerBatchRequest {
  texts: [
    "삼성전자가 신제품을 발표했다.",
    "현대자동차는 서울 강남구에 본사가 있다."
  ]
  confidence_threshold: 0.8
}

응답: NerBatchResponse {
  results: [
    {
      text: "삼성전자가 신제품을 발표했다."
      entities: [
        { word: "삼성전자", entity: "ORG", confidence: 0.95, start: 0, end: 4 }
      ]
    },
    {
      text: "현대자동차는 서울 강남구에 본사가 있다."
      entities: [
        { word: "현대자동차", entity: "ORG", confidence: 0.93, start: 0, end: 5 },
        { word: "서울", entity: "LOC", confidence: 0.91, start: 7, end: 9 },
        { word: "강남구", entity: "LOC", confidence: 0.88, start: 10, end: 13 }
      ]
    }
  ]
  total_texts: 2
  processing_time_ms: 78
}
```

### 4.3 gRPC 에러 처리

gRPC는 표준 상태 코드를 사용하여 에러를 전달합니다.

**gRPC Status Codes:**
| 코드 | 상태 | 의미 | 예시 |
|------|------|------|------|
| 0 | OK | 성공 | NER 처리 완료 |
| 3 | INVALID_ARGUMENT | 잘못된 입력 | 텍스트 길이 초과, 잘못된 임계값 |
| 13 | INTERNAL | 내부 서버 에러 | 모델 추론 실패 |
| 14 | UNAVAILABLE | 서비스 이용 불가 | 모델 미로딩 |

**에러 상세 정보:**
```python
# 클라이언트에서 받는 에러 정보
try:
    response = stub.ExtractEntities(request)
except grpc.RpcError as e:
    print(f"gRPC Error: {e.code()}")
    print(f"Details: {e.details()}")
    # 예: "Text length exceeds maximum allowed (10000 characters)"
```

---

## 5. NER 모델

### 5.1 모델 정보
| 항목 | 내용 |
|------|------|
| **모델명** | `koorukuroo/korean_bert_ner` |
| **베이스** | KLUE BERT (klue/bert-base) |
| **태스크** | Token Classification (NER) |
| **성능** | Entity F1 88.43% |
| **태그 수** | 21개 (BIO 태깅: B-PER, I-PER, ...) |

### 5.2 개체 태그 (21개)

#### BIO 태깅 방식
- **B-XXX**: 개체의 시작 (Begin)
- **I-XXX**: 개체의 중간/끝 (Inside)
- **O**: 개체 아님 (Outside)

#### 개체 유형
| 태그 | 의미 | 예시 |
|------|------|------|
| **PER** | 인물 | 이재용, BTS, 윤석열 |
| **ORG** | 기관/기업 | 삼성전자, 카카오, 국회 |
| **LOC** | 지역 | 서울, 강남구, 미국 |
| **DAT** | 날짜 | 2025년 1월, 어제 |
| **TIM** | 시간 | 오후 3시, 아침 |
| **QT** | 수량 | 100만원, 3개 |
| **EV** | 이벤트 | 월드컵, 올림픽 |
| **AF** | 인공물 | 갤럭시, 아이폰 |
| **CV** | 문명 | 불교, 한국어 |
| **FD** | 학문분야 | 물리학, 경제학 |
| **TR** | 이론 | 상대성이론 |
| **AM** | 동물 | 호랑이, 코끼리 |
| **PT** | 식물 | 소나무, 장미 |
| **MT** | 물질 | 금, 석유 |
| **TM** | 용어 | AI, 블록체인 |

**예시 태깅:**
```
텍스트: "삼성전자 이재용 회장이 서울에서 신제품을 발표했다."
태그:   B-ORG I-ORG B-PER I-PER O O B-LOC O O O O O
개체:   [삼성전자(ORG)] [이재용(PER)] [서울(LOC)]
```

### 5.3 모델 로딩 전략

#### Singleton Pattern
```python
class NerModel:
    _instance = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            cls._instance._load_model()
        return cls._instance
```

**이유:**
- 모델은 서버 시작 시 한 번만 로딩
- 메모리 효율성 (약 500MB)
- 추론 속도 향상

### 5.4 추론 프로세스
```
1. 텍스트 입력
   ↓
2. Tokenization (BERT Tokenizer)
   - 서브워드 분리
   ↓
3. Model Forward Pass
   - BERT 인코딩
   - Token Classification Head
   ↓
4. Logits → Probabilities (Softmax)
   ↓
5. BIO 태그 디코딩
   - B-PER, I-PER → "이재용" (PER)
   ↓
6. 신뢰도 필터링 (>= 0.8)
   ↓
7. 결과 반환
```

---

## 6. 데이터 모델 (Protocol Buffers)

### 6.1 protos/ner.proto - 전체 정의

**파일 위치:** `ner-server/protos/ner.proto`

이 파일은 gRPC 서비스와 모든 메시지 타입을 정의합니다.

```protobuf
syntax = "proto3";

package ner;

// =====================================
// Service Definition
// =====================================

service NerService {
  // 단일 텍스트 NER
  rpc ExtractEntities(NerRequest) returns (NerResponse);

  // 배치 NER (최대 100개)
  rpc ExtractEntitiesBatch(NerBatchRequest) returns (NerBatchResponse);
}

// =====================================
// Request Messages
// =====================================

message NerRequest {
  string text = 1;                      // 분석할 텍스트 (최대 10,000자)
  float confidence_threshold = 2;        // 신뢰도 임계값 (기본값: 0.8, 범위: 0.0~1.0)
}

message NerBatchRequest {
  repeated string texts = 1;             // 분석할 텍스트 리스트 (최대 100개)
  float confidence_threshold = 2;        // 신뢰도 임계값 (기본값: 0.8)
}

// =====================================
// Response Messages
// =====================================

message NerResponse {
  repeated NerEntity entities = 1;       // 추출된 개체명 리스트
  int32 processing_time_ms = 2;          // 처리 시간 (밀리초)
}

message NerBatchResponse {
  repeated NerBatchResult results = 1;   // 각 텍스트별 결과
  int32 total_texts = 2;                 // 처리된 텍스트 개수
  int32 processing_time_ms = 3;          // 총 처리 시간 (밀리초)
}

// =====================================
// Data Types
// =====================================

message NerEntity {
  string word = 1;                       // 개체명
  string entity = 2;                     // 개체 유형 (PER, ORG, LOC 등)
  float confidence = 3;                  // 신뢰도 (0.0 ~ 1.0)
  int32 start = 4;                       // 시작 위치
  int32 end = 5;                         // 종료 위치
}

message NerBatchResult {
  string text = 1;                       // 입력 텍스트
  repeated NerEntity entities = 2;       // 추출된 개체명 리스트
}
```

### 6.2 Protocol Buffers 컴파일

**컴파일 명령어:**
```bash
# Python 코드 생성
python -m grpc_tools.protoc \
  -I./protos \
  --python_out=./app/generated \
  --grpc_python_out=./app/generated \
  ./protos/ner.proto
```

**생성되는 파일:**
- `app/generated/ner_pb2.py` - 메시지 클래스 (NerRequest, NerResponse 등)
- `app/generated/ner_pb2_grpc.py` - gRPC 서비스 스텁 및 Servicer

### 6.3 Python에서 사용하기

**서버 사이드 (Servicer):**
```python
import grpc
from app.generated import ner_pb2, ner_pb2_grpc

class NerServicer(ner_pb2_grpc.NerServiceServicer):
    def ExtractEntities(self, request, context):
        # request.text, request.confidence_threshold 사용
        entities = self._extract(request.text, request.confidence_threshold)

        return ner_pb2.NerResponse(
            entities=entities,
            processing_time_ms=45
        )
```

**클라이언트 사이드 (Java - indexing-system):**
```java
// Java에서 gRPC 호출
NerServiceGrpc.NerServiceBlockingStub stub = ...;
NerRequest request = NerRequest.newBuilder()
    .setText("삼성전자 이재용 회장이 서울에서 발표했다.")
    .setConfidenceThreshold(0.8f)
    .build();

NerResponse response = stub.extractEntities(request);
for (NerEntity entity : response.getEntitiesList()) {
    System.out.println(entity.getWord() + " -> " + entity.getEntity());
}
```

### 6.4 내부 데이터 모델 (Python)

Protocol Buffers는 외부 인터페이스용이고, 내부적으로는 Python 데이터클래스를 사용할 수 있습니다.

```python
from dataclasses import dataclass

@dataclass
class ModelPrediction:
    """NER 모델 내부 예측 결과 (BIO 태깅)"""
    token: str
    label: str          # BIO 태그 (B-PER, I-ORG 등)
    confidence: float
    start: int
    end: int
```

---

## 7. 성능 최적화

### 7.1 모델 최적화

| 방법 | 설명 | 예상 효과 |
|------|------|-----------|
| **싱글톤 패턴** | 모델 한 번만 로딩 | 메모리 절약, 빠른 응답 |
| **배치 처리** | 여러 텍스트 동시 처리 | 2~3배 처리량 향상 |
| **CPU 최적화** | torch.set_num_threads() | CPU 활용률 향상 |
| **GPU 지원** | CUDA 활용 (선택사항) | 5~10배 속도 향상 |

### 7.2 배치 크기 제한
```python
# 배치 요청 최대 100개
MAX_BATCH_SIZE = 100

# 텍스트 최대 길이 10,000자
MAX_TEXT_LENGTH = 10000
```

### 7.3 처리 성능 목표

| 환경 | 배치 크기 | 예상 처리량 | 응답 시간 |
|------|-----------|-------------|-----------|
| **CPU (개발)** | 1 | ~10 req/s | ~100ms |
| **CPU (운영)** | 32 | ~50 req/s | ~500ms |
| **GPU (T4)** | 32 | ~200 req/s | ~150ms |
| **GPU (A100)** | 64 | ~500 req/s | ~100ms |

### 7.4 메모리 사용량
- **모델 크기**: ~500MB (BERT-base)
- **런타임 메모리**: ~1GB (추론 시)
- **권장 최소 메모리**: 2GB

---

## 8. 에러 처리 (gRPC)

### 8.1 에러 분류

gRPC 환경에서는 HTTP 상태 코드 대신 gRPC 상태 코드를 사용합니다.

| 에러 유형 | gRPC Status Code | 처리 방법 |
|-----------|------------------|-----------|
| **Validation Error** | INVALID_ARGUMENT (3) | 입력값 검증 및 에러 메시지 반환 |
| **Model Not Loaded** | UNAVAILABLE (14) | 헬스 체크로 사전 감지 |
| **Inference Error** | INTERNAL (13) | 로그 + 재시도 권장 |
| **Timeout** | DEADLINE_EXCEEDED (4) | 배치 크기 줄이기 |
| **Resource Exhausted** | RESOURCE_EXHAUSTED (8) | 동시 요청 수 제한 |

### 8.2 gRPC 예외 처리 전략

**서버 사이드 (Servicer):**
```python
import grpc
from app.generated import ner_pb2, ner_pb2_grpc
from app.config import config
import logging

logger = logging.getLogger(__name__)

class NerServicer(ner_pb2_grpc.NerServiceServicer):
    def ExtractEntities(self, request, context):
        try:
            # 1. 입력 검증
            if not request.text or len(request.text) == 0:
                context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
                context.set_details("Text cannot be empty")
                return ner_pb2.NerResponse()

            if len(request.text) > config.max_text_length:
                context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
                context.set_details(
                    f"Text length exceeds maximum allowed ({config.max_text_length} characters)"
                )
                return ner_pb2.NerResponse()

            if not (0.0 <= request.confidence_threshold <= 1.0):
                context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
                context.set_details("Confidence threshold must be between 0.0 and 1.0")
                return ner_pb2.NerResponse()

            # 2. 모델 로딩 체크
            if not self.model.is_loaded():
                context.set_code(grpc.StatusCode.UNAVAILABLE)
                context.set_details("NER model is not loaded")
                return ner_pb2.NerResponse()

            # 3. NER 추론
            entities = self.model.predict(request.text, request.confidence_threshold)

            # 4. 성공 응답
            return ner_pb2.NerResponse(
                entities=entities,
                processing_time_ms=processing_time
            )

        except Exception as e:
            logger.error(f"NER extraction failed: {e}", exc_info=True)
            context.set_code(grpc.StatusCode.INTERNAL)
            context.set_details(f"Internal server error: {str(e)}")
            return ner_pb2.NerResponse()
```

**클라이언트 사이드 (Java - 에러 처리):**
```java
try {
    NerResponse response = stub.extractEntities(request);
    // 정상 처리
} catch (StatusRuntimeException e) {
    switch (e.getStatus().getCode()) {
        case INVALID_ARGUMENT:
            log.warn("Invalid request: {}", e.getStatus().getDescription());
            break;
        case UNAVAILABLE:
            log.error("NER service unavailable: {}", e.getStatus().getDescription());
            // 재시도 로직
            break;
        case INTERNAL:
            log.error("NER service internal error: {}", e.getStatus().getDescription());
            break;
        default:
            log.error("NER service error: {}", e.getStatus());
    }
}
```

### 8.3 로깅 전략

**로그 설정:**
```python
import logging
from app.config import config

# 로그 레벨 설정
logging.basicConfig(
    level=getattr(logging, config.log_level),
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)

logger = logging.getLogger(__name__)
```

**로그 내용:**
```python
# 요청 로그
logger.info(
    f"gRPC ExtractEntities: text_length={len(request.text)}, "
    f"threshold={request.confidence_threshold}"
)

# 성공 로그
logger.info(
    f"NER completed: entities_count={len(entities)}, "
    f"processing_time={processing_time}ms"
)

# 에러 로그
logger.error(f"Model inference failed: {error}", exc_info=True)

# 배치 처리 로그
logger.info(
    f"gRPC ExtractEntitiesBatch: batch_size={len(request.texts)}, "
    f"total_processing_time={processing_time}ms"
)
```

---

## 9. 배포 전략 (gRPC)

### 9.1 Docker 구성

#### Dockerfile
```dockerfile
FROM python:3.10-slim

WORKDIR /app

# 의존성 설치
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Protocol Buffers 컴파일 디렉토리 생성
RUN mkdir -p app/generated

# Protocol Buffers 파일 복사 및 컴파일
COPY protos/ ./protos/
RUN python -m grpc_tools.protoc \
    -I./protos \
    --python_out=./app/generated \
    --grpc_python_out=./app/generated \
    ./protos/ner.proto

# 모델 사전 다운로드 (선택사항 - 빌드 시간 증가)
# RUN python -c "from transformers import AutoModelForTokenClassification; \
#               AutoModelForTokenClassification.from_pretrained('koorukuroo/korean_bert_ner')"

# 앱 복사
COPY ./app ./app

# gRPC 포트 노출
EXPOSE 50051

# gRPC 서버 실행
CMD ["python", "-m", "app.server"]
```

#### docker-compose.yml 추가
```yaml
services:
  # 기존 서비스들...

  ner-server:
    build: ./ner-server
    container_name: ner-server
    ports:
      - "50051:50051"  # gRPC 포트
    environment:
      - MODEL_NAME=koorukuroo/korean_bert_ner
      - DEVICE=cpu
      - GRPC_PORT=50051
      - GRPC_HOST=0.0.0.0
      - MAX_WORKERS=10
      - LOG_LEVEL=INFO
    volumes:
      - ./ner-server/models:/root/.cache/huggingface  # 모델 캐시
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "grpc_health_probe", "-addr=:50051"]
      interval: 30s
      timeout: 10s
      retries: 3
    networks:
      - app-network

networks:
  app-network:
    driver: bridge
```

### 9.2 환경 변수

| 변수명 | 기본값 | 설명 |
|--------|--------|------|
| `MODEL_NAME` | `koorukuroo/korean_bert_ner` | HuggingFace 모델 ID |
| `DEVICE` | `cpu` | `cpu` 또는 `cuda` |
| `GRPC_PORT` | `50051` | gRPC 서버 포트 |
| `GRPC_HOST` | `0.0.0.0` | gRPC 서버 호스트 |
| `MAX_WORKERS` | `10` | gRPC 서버 워커 수 |
| `MAX_BATCH_SIZE` | `100` | 배치 최대 크기 |
| `CONFIDENCE_THRESHOLD` | `0.8` | 기본 신뢰도 임계값 |
| `LOG_LEVEL` | `INFO` | 로그 레벨 |
| `TORCH_NUM_THREADS` | `4` | PyTorch CPU 스레드 수 |

### 9.3 gRPC 헬스 체크

**방법 1: grpc_health_probe 사용 (권장)**
```bash
# grpc_health_probe 설치 (Docker 이미지에 포함)
wget -qO/bin/grpc_health_probe https://github.com/grpc-ecosystem/grpc-health-probe/releases/download/v0.4.19/grpc_health_probe-linux-amd64 && \
chmod +x /bin/grpc_health_probe

# 헬스 체크 실행
grpc_health_probe -addr=localhost:50051
```

**방법 2: grpcurl 사용**
```bash
# grpcurl로 서비스 확인
grpcurl -plaintext localhost:50051 list

# 예상 출력:
# grpc.health.v1.Health
# ner.NerService
```

**방법 3: Python 클라이언트**
```python
import grpc
from app.generated import ner_pb2, ner_pb2_grpc

def check_health():
    with grpc.insecure_channel('localhost:50051') as channel:
        stub = ner_pb2_grpc.NerServiceStub(channel)
        request = ner_pb2.NerRequest(
            text="테스트",
            confidence_threshold=0.8
        )
        try:
            response = stub.ExtractEntities(request)
            print("✓ NER Server is healthy")
            return True
        except grpc.RpcError as e:
            print(f"✗ NER Server error: {e.code()}")
            return False
```

### 9.4 배포 시나리오

**개발 환경:**
```bash
# Python 직접 실행
cd ner-server
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt

# Protocol Buffers 컴파일
python -m grpc_tools.protoc \
  -I./protos \
  --python_out=./app/generated \
  --grpc_python_out=./app/generated \
  ./protos/ner.proto

# 서버 실행
python -m app.server
```

**Docker Compose (통합 환경):**
```bash
# 전체 시스템 시작
docker-compose up -d

# NER 서버만 재시작
docker-compose restart ner-server

# 로그 확인
docker-compose logs -f ner-server
```

**Kubernetes (프로덕션):**
```yaml
apiVersion: v1
kind: Service
metadata:
  name: ner-server
spec:
  selector:
    app: ner-server
  ports:
    - protocol: TCP
      port: 50051
      targetPort: 50051
  type: ClusterIP
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ner-server
spec:
  replicas: 3
  selector:
    matchLabels:
      app: ner-server
  template:
    metadata:
      labels:
        app: ner-server
    spec:
      containers:
      - name: ner-server
        image: ner-server:latest
        ports:
        - containerPort: 50051
        env:
        - name: DEVICE
          value: "cpu"
        - name: LOG_LEVEL
          value: "INFO"
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
```

---

## 10. 구현 계획

### Phase 1: 기본 구조 및 의존성 ✅
- [x] 디렉토리 구조 생성
- [x] .gitignore, .dockerignore
- [x] README.md
- [x] requirements.txt 작성
- [x] app/config.py 구현

### Phase 2: Protocol Buffers 정의 (gRPC 인터페이스) ✅
- [x] protos/ner.proto 작성
  - [x] NerService 정의 (ExtractEntities, ExtractEntitiesBatch)
  - [x] NerRequest 메시지
  - [x] NerResponse 메시지
  - [x] NerEntity 메시지
  - [x] NerBatchRequest 메시지
  - [x] NerBatchResponse 메시지
  - [x] NerBatchResult 메시지
- [x] compile_protos.sh 스크립트 생성
- [x] app/generated/ 디렉토리 생성
- [x] README.md 업데이트 (컴파일 가이드 추가)

### Phase 3: NER 모델 ✅
- [x] app/models/ner_model.py
  - [x] NerModel 클래스 (Singleton 패턴)
  - [x] 모델 로딩 로직 (_load_model)
  - [x] predict() 메서드 (단일 텍스트)
  - [x] predict_batch() 메서드 (배치 처리)
  - [x] BIO 태그 디코딩 (HuggingFace pipeline aggregation_strategy)
  - [x] 신뢰도 필터링
  - [x] 에러 처리 및 로깅
- [x] app/models/__init__.py
- [x] test_model.py (모델 테스트 스크립트)

### Phase 4: gRPC 서비스 레이어 ✅
- [x] app/services/ner_servicer.py
  - [x] NerServicer 클래스 (ner_pb2_grpc.NerServiceServicer 상속)
  - [x] ExtractEntities() RPC 메서드 구현
  - [x] ExtractEntitiesBatch() RPC 메서드 구현
  - [x] gRPC 입력 검증 (context.set_code)
  - [x] gRPC 에러 처리
- [x] app/services/__init__.py

### Phase 5: gRPC 서버 레이어 ✅
- [x] app/server.py
  - [x] gRPC 서버 생성 및 설정
  - [x] ThreadPoolExecutor 설정 (max_workers)
  - [x] NerServicer 등록
  - [x] 서버 시작/종료 로직
  - [x] Graceful shutdown 구현
  - [x] 시그널 핸들러 (SIGINT, SIGTERM)
  - [x] 메시지 크기 제한 설정 (50MB)

### Phase 6: 테스트 ✅
- [x] tests/test_ner_model.py
  - [x] NerModel Singleton 테스트
  - [x] 모델 로딩 테스트
  - [x] predict() 메서드 테스트 (단일/배치)
  - [x] BIO 태그 디코딩 테스트
  - [x] 에러 처리 테스트
  - [x] 신뢰도 필터링 테스트
- [x] tests/test_ner_servicer.py
  - [x] ExtractEntities() gRPC 호출 테스트
  - [x] ExtractEntitiesBatch() gRPC 호출 테스트
  - [x] 입력 검증 테스트 (빈 텍스트, 길이 초과, 임계값 오류)
  - [x] gRPC 에러 처리 테스트
  - [x] 배치 처리 일관성 테스트
- [x] tests/conftest.py (공통 fixture)
- [x] pytest.ini (pytest 설정)

### Phase 7: Docker 및 배포 ✅
- [x] Dockerfile 작성 (gRPC 서버용)
  - [x] Python 3.10-slim 베이스 이미지
  - [x] 의존성 설치
  - [x] Protocol Buffers 자동 컴파일
  - [x] grpc_health_probe 설치
  - [x] 헬스 체크 설정
  - [x] 환경 변수 기본값 설정
- [x] docker-compose.yml 업데이트 (50051 포트)
  - [x] ner-server 서비스 추가
  - [x] 모델 캐시 볼륨 설정
  - [x] 헬스 체크 설정 (60초 시작 대기)
  - [x] 환경 변수 설정
- [x] .dockerignore 업데이트
- [x] README.md 업데이트 (Docker 실행 방법)

### Phase 8: 문서화
- [ ] gRPC 서비스 문서 (protos/ner.proto 주석)
- [ ] README 업데이트 (gRPC 사용법)
- [ ] Java 클라이언트 예제 코드
- [ ] 성능 벤치마크 결과

---

## 부록

### A. 참고 자료
- [HuggingFace - koorukuroo/korean_bert_ner](https://huggingface.co/koorukuroo/korean_bert_ner)
- [gRPC 공식 문서](https://grpc.io/docs/)
- [Protocol Buffers 공식 문서](https://protobuf.dev/)
- [Transformers 공식 문서](https://huggingface.co/docs/transformers)
- [KLUE Benchmark](https://github.com/KLUE-benchmark/KLUE)

### B. 개발 팁

#### 로컬 개발 (gRPC)
```bash
# 가상환경 생성
python -m venv venv
source venv/bin/activate

# 의존성 설치
pip install -r requirements.txt

# Protocol Buffers 컴파일
python -m grpc_tools.protoc \
  -I./protos \
  --python_out=./app/generated \
  --grpc_python_out=./app/generated \
  ./protos/ner.proto

# gRPC 서버 실행
python -m app.server
```

#### gRPC 테스트

**방법 1: Python 클라이언트**
```python
import grpc
from app.generated import ner_pb2, ner_pb2_grpc

# gRPC 채널 생성
channel = grpc.insecure_channel('localhost:50051')
stub = ner_pb2_grpc.NerServiceStub(channel)

# 단일 NER 요청
request = ner_pb2.NerRequest(
    text="삼성전자 이재용 회장이 서울에서 발표했다.",
    confidence_threshold=0.8
)
response = stub.ExtractEntities(request)

for entity in response.entities:
    print(f"{entity.word} -> {entity.entity} ({entity.confidence:.2f})")
```

**방법 2: grpcurl (CLI)**
```bash
# 서비스 목록 확인
grpcurl -plaintext localhost:50051 list

# 단일 NER 요청
grpcurl -plaintext -d '{
  "text": "삼성전자 이재용 회장이 서울에서 발표했다.",
  "confidence_threshold": 0.8
}' localhost:50051 ner.NerService/ExtractEntities

# 배치 NER 요청
grpcurl -plaintext -d '{
  "texts": ["삼성전자가 발표했다.", "현대차는 서울에 있다."],
  "confidence_threshold": 0.8
}' localhost:50051 ner.NerService/ExtractEntitiesBatch
```

---

## 변경 이력

| 버전 | 날짜 | 작성자 | 내용 |
|------|------|--------|------|
| 1.0 | 2025-01-28 | Claude | 최초 작성 |
