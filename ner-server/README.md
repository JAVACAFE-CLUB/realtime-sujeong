# NER Server

## 개요
한국어 개체명 인식(Named Entity Recognition) 서버

## 기술 스택
- Python 3.10+
- FastAPI (REST API)
- Transformers (HuggingFace)
- PyTorch
- 모델: `koorukuroo/korean_bert_ner`

## 디렉토리 구조
```
ner-server/
├── app/
│   ├── __init__.py
│   ├── main.py              # FastAPI 애플리케이션
│   ├── config.py            # 설정
│   ├── models/
│   │   ├── __init__.py
│   │   └── ner_model.py     # NER 모델 로딩/추론
│   ├── services/
│   │   ├── __init__.py
│   │   └── ner_service.py   # NER 비즈니스 로직
│   └── schemas/
│       ├── __init__.py
│       ├── request.py        # 요청 스키마
│       └── response.py       # 응답 스키마
├── tests/
│   ├── __init__.py
│   └── test_ner.py
├── requirements.txt
├── Dockerfile
└── README.md
```

## API 엔드포인트
- `POST /api/ner` - 단일 텍스트 NER
- `POST /api/ner/batch` - 배치 NER
- `GET /health` - 헬스 체크

## 로컬 실행
```bash
# 가상환경 생성
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# 의존성 설치
pip install -r requirements.txt

# 서버 실행
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

## Docker 실행
```bash
docker build -t ner-server .
docker run -p 8000:8000 ner-server
```

## 테스트
```bash
pytest tests/
```