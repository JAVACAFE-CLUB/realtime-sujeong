# NER Server Dockerfile
# Python 3.10 기반 gRPC 서버

FROM python:3.10-slim

# 작업 디렉토리 설정
WORKDIR /app

# 시스템 패키지 업데이트 및 필요한 도구 설치
RUN apt-get update && apt-get install -y \
    wget \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Python 의존성 파일 복사 및 설치
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Protocol Buffers 컴파일을 위한 디렉토리 생성
RUN mkdir -p app/generated

# Protocol Buffers 정의 파일 복사
COPY protos/ ./protos/

# Protocol Buffers 컴파일
RUN python -m grpc_tools.protoc \
    -I./protos \
    --python_out=./app/generated \
    --grpc_python_out=./app/generated \
    ./protos/ner.proto

# __init__.py 파일 생성 (generated 패키지)
RUN touch app/generated/__init__.py

# 애플리케이션 코드 복사
COPY ./app ./app
COPY .env.example .env

# 모델 캐시 디렉토리 생성
RUN mkdir -p /root/.cache/huggingface

# gRPC 포트 노출
EXPOSE 50051

# 헬스 체크 (선택사항)
# grpc_health_probe 다운로드
RUN wget -qO /bin/grpc_health_probe \
    https://github.com/grpc-ecosystem/grpc-health-probe/releases/download/v0.4.19/grpc_health_probe-linux-amd64 \
    && chmod +x /bin/grpc_health_probe

# 헬스 체크 설정
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD grpc_health_probe -addr=:50051 || exit 1

# 환경 변수 기본값 설정
ENV MODEL_NAME=koorukuroo/korean_bert_ner \
    DEVICE=cpu \
    GRPC_PORT=50051 \
    GRPC_HOST=0.0.0.0 \
    MAX_WORKERS=10 \
    LOG_LEVEL=INFO \
    TORCH_NUM_THREADS=4

# gRPC 서버 실행
CMD ["python", "-m", "app.server"]