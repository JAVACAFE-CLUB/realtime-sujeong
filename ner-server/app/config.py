"""
NER Server Configuration

환경 변수를 통해 서버 설정을 관리합니다.
"""

import os
from dataclasses import dataclass
from typing import Literal


@dataclass
class Config:
    """NER Server 설정 클래스"""

    # Model Configuration
    model_name: str = os.getenv("MODEL_NAME", "koorukuroo/korean_bert_ner")
    device: Literal["cpu", "cuda"] = os.getenv("DEVICE", "cpu")

    # NER Configuration
    max_text_length: int = int(os.getenv("MAX_TEXT_LENGTH", "10000"))
    max_batch_size: int = int(os.getenv("MAX_BATCH_SIZE", "100"))
    default_confidence_threshold: float = float(os.getenv("CONFIDENCE_THRESHOLD", "0.8"))

    # Server Configuration
    grpc_port: int = int(os.getenv("GRPC_PORT", "50051"))
    grpc_host: str = os.getenv("GRPC_HOST", "0.0.0.0")
    max_workers: int = int(os.getenv("MAX_WORKERS", "10"))

    # Logging Configuration
    log_level: str = os.getenv("LOG_LEVEL", "INFO")

    # Performance Configuration
    torch_num_threads: int = int(os.getenv("TORCH_NUM_THREADS", "4"))

    def __post_init__(self):
        """설정 유효성 검사"""
        if self.device not in ["cpu", "cuda"]:
            raise ValueError(f"Invalid device: {self.device}. Must be 'cpu' or 'cuda'")

        if self.max_text_length <= 0:
            raise ValueError(f"max_text_length must be positive: {self.max_text_length}")

        if self.max_batch_size <= 0 or self.max_batch_size > 1000:
            raise ValueError(f"max_batch_size must be between 1 and 1000: {self.max_batch_size}")

        if not 0.0 <= self.default_confidence_threshold <= 1.0:
            raise ValueError(f"confidence_threshold must be between 0.0 and 1.0: {self.default_confidence_threshold}")

        if self.grpc_port <= 0 or self.grpc_port > 65535:
            raise ValueError(f"Invalid port number: {self.grpc_port}")

    def display_config(self) -> str:
        """설정 정보를 문자열로 반환"""
        return f"""
NER Server Configuration:
========================
Model: {self.model_name}
Device: {self.device}
gRPC Port: {self.grpc_port}
Max Text Length: {self.max_text_length}
Max Batch Size: {self.max_batch_size}
Default Confidence Threshold: {self.default_confidence_threshold}
Max Workers: {self.max_workers}
Log Level: {self.log_level}
PyTorch Threads: {self.torch_num_threads}
========================
"""


# Global config instance
config = Config()
