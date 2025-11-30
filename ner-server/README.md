# NER Server (gRPC)

한국어 개체명 인식(Named Entity Recognition) gRPC 서버

## 모델 정보

- **모델**: `koorukuroo/korean_bert_ner`
- **성능**: Entity F1 88.43%
- **태그**: KLUE 표준 (PER, ORG, LOC, DAT, TIM 등 21개)
- **통신**: gRPC (HTTP/2)

## 디렉토리 구조

```
ner-server/
├── protos/
│   └── ner.proto              # gRPC 인터페이스 정의
├── app/
│   ├── __init__.py
│   ├── config.py              # 설정 관리
│   ├── generated/             # 컴파일된 protobuf 파일
│   ├── models/                # NER 모델 레이어 (Phase 3)
│   ├── services/              # gRPC 서비스 레이어 (Phase 4)
│   └── server.py              # gRPC 서버 (Phase 5)
├── tests/                     # 테스트 코드 (Phase 6)
├── requirements.txt           # Python 의존성
├── .env.example               # 환경 변수 예시
├── compile_protos.sh          # protobuf 컴파일 스크립트
├── Dockerfile                 # Docker 이미지 (Phase 7)
├── DESIGN.md                  # 설계 문서
└── README.md                  # 이 파일
```

## 설치 및 실행

### 1. 의존성 설치

```bash
pip install -r requirements.txt
```

### 2. 환경 변수 설정

```bash
cp .env.example .env
# .env 파일을 편집하여 설정 변경
```

### 3. Protocol Buffers 컴파일

```bash
# 자동 컴파일 스크립트 실행
./compile_protos.sh

# 또는 수동 컴파일
python -m grpc_tools.protoc \
    -I./protos \
    --python_out=./app/generated \
    --grpc_python_out=./app/generated \
    ./protos/ner.proto
```

컴파일 후 생성되는 파일:
- `app/generated/ner_pb2.py` - Protocol Buffers 메시지 정의
- `app/generated/ner_pb2_grpc.py` - gRPC 서비스 스텁

### 4. 서버 실행 (Phase 5 이후)

```bash
python -m app.server
```

서버는 기본적으로 `0.0.0.0:50051`에서 실행됩니다.

## Protocol Buffers 인터페이스

### Service Definition

```protobuf
service NerService {
  // 단일 텍스트 개체명 추출
  rpc ExtractEntities(NerRequest) returns (NerResponse);

  // 배치 텍스트 개체명 추출 (최대 100개)
  rpc ExtractEntitiesBatch(NerBatchRequest) returns (NerBatchResponse);
}
```

### Message Types

#### NerRequest
```protobuf
message NerRequest {
  string text = 1;                  // 분석할 텍스트
  float confidence_threshold = 2;   // 신뢰도 임계값 (기본: 0.8)
}
```

#### NerResponse
```protobuf
message NerResponse {
  repeated NerEntity entities = 1;  // 추출된 개체명 리스트
  int32 processing_time_ms = 2;     // 처리 시간 (ms)
}
```

#### NerEntity
```protobuf
message NerEntity {
  string word = 1;                  // 개체명
  string entity = 2;                // 개체 유형 (PER, ORG, LOC, etc.)
  float confidence = 3;             // 신뢰도 (0.0 ~ 1.0)
  int32 start = 4;                  // 시작 위치
  int32 end = 5;                    // 종료 위치
}
```

## 환경 변수

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `MODEL_NAME` | `koorukuroo/korean_bert_ner` | NER 모델 이름 |
| `DEVICE` | `cpu` | 실행 디바이스 (cpu/cuda) |
| `MAX_TEXT_LENGTH` | `10000` | 최대 텍스트 길이 |
| `MAX_BATCH_SIZE` | `100` | 배치 처리 최대 크기 |
| `CONFIDENCE_THRESHOLD` | `0.8` | 기본 신뢰도 임계값 |
| `GRPC_PORT` | `50051` | gRPC 서버 포트 |
| `GRPC_HOST` | `0.0.0.0` | gRPC 서버 호스트 |
| `MAX_WORKERS` | `10` | gRPC 워커 스레드 수 |
| `LOG_LEVEL` | `INFO` | 로그 레벨 |
| `TORCH_NUM_THREADS` | `4` | PyTorch 스레드 수 |

## 개발 상태

- [x] Phase 1: 기본 구조 및 의존성 (requirements.txt, config.py) ✅
- [x] Phase 2: Protocol Buffers 정의 (protos/ner.proto) ✅
- [x] Phase 3: NER 모델 레이어 (models/ner_model.py) ✅
- [x] Phase 4: gRPC 서비스 레이어 (services/ner_servicer.py) ✅
- [x] Phase 5: gRPC 서버 (app/server.py) ✅
- [ ] Phase 6: 테스트 코드
- [ ] Phase 7: Docker 설정

## Entity Tags (KLUE 표준)

| 태그 | 의미 | 예시 |
|------|------|------|
| PER | 인물 | 이재용, 윤석열, BTS |
| ORG | 기관 | 삼성전자, 카카오, 국회 |
| LOC | 지역 | 서울, 강남구, 미국 |
| DAT | 날짜 | 2024년 1월, 어제 |
| TIM | 시간 | 오후 3시, 아침 |
| QT | 수량 | 100만원, 3개 |
| AF | 인공물 | 갤럭시, 아이폰 |
| ... | ... | 총 21개 태그 |

## 성능

| 태그 | F1 Score |
|------|----------|
| PER (인물) | 94% |
| ORG (기관) | 88% |
| LOC (지역) | 74% |
| DAT (날짜) | 96% |
| TIM (시간) | 86% |

## 참고 자료

- [gRPC Python Documentation](https://grpc.io/docs/languages/python/)
- [Protocol Buffers](https://developers.google.com/protocol-buffers)
- [koorukuroo/korean_bert_ner](https://huggingface.co/koorukuroo/korean_bert_ner)
- [KLUE Benchmark](https://github.com/KLUE-benchmark/KLUE)