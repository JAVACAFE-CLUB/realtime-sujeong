"""
gRPC Service Layer

이 모듈은 gRPC 서비스 구현을 담당합니다.
"""

from .ner_servicer import NerServicer

__all__ = ["NerServicer"]