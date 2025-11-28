#!/bin/bash
# Protocol Buffers 컴파일 스크립트

echo "Compiling Protocol Buffers..."

# generated 디렉토리 생성
mkdir -p app/generated

# Protocol Buffers 컴파일
python -m grpc_tools.protoc \
    -I./protos \
    --python_out=./app/generated \
    --grpc_python_out=./app/generated \
    ./protos/ner.proto

# 컴파일 확인
if [ $? -eq 0 ]; then
    echo "✅ Protocol Buffers compiled successfully!"
    echo "Generated files:"
    ls -la app/generated/
else
    echo "❌ Failed to compile Protocol Buffers"
    exit 1
fi