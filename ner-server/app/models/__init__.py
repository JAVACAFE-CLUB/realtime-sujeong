"""
NER Model Layer

이 모듈은 한국어 NER 모델 로딩 및 추론을 담당합니다.
"""

from .ner_model import NerModel, get_ner_model

__all__ = ["NerModel", "get_ner_model"]
