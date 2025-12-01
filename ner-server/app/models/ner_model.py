"""
NER Model Layer

Singleton 패턴을 사용한 한국어 NER 모델 로딩 및 추론
"""

import logging
import time
from typing import List, Tuple, Optional
import torch
from transformers import (
    AutoTokenizer,
    AutoModelForTokenClassification,
    pipeline
)
from app.config import config

logger = logging.getLogger(__name__)


class NerModel:
    """
    Singleton 패턴을 사용한 NER 모델 클래스

    모델은 서버 시작 시 한 번만 로딩되며, 이후 모든 요청에서 재사용됩니다.
    """

    _instance: Optional['NerModel'] = None
    _initialized: bool = False

    def __new__(cls):
        """Singleton 인스턴스 생성"""
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance

    def __init__(self):
        """
        모델 초기화 (Singleton이므로 한 번만 실행)
        """
        if not self._initialized:
            self._load_model()
            NerModel._initialized = True

    def _load_model(self):
        """모델 및 토크나이저 로딩"""
        try:
            logger.info(f"Loading NER model: {config.model_name}")
            start_time = time.time()

            # PyTorch 스레드 수 설정
            torch.set_num_threads(config.torch_num_threads)

            # 디바이스 설정
            self.device = torch.device(
                "cuda" if config.device == "cuda" and torch.cuda.is_available()
                else "cpu"
            )
            logger.info(f"Using device: {self.device}")

            # 토크나이저 로딩
            self.tokenizer = AutoTokenizer.from_pretrained(config.model_name)

            # 모델 로딩
            self.model = AutoModelForTokenClassification.from_pretrained(
                config.model_name
            ).to(self.device)

            # 평가 모드로 설정 (추론용)
            self.model.eval()

            # HuggingFace Pipeline 생성 (편의성)
            self.ner_pipeline = pipeline(
                "ner",
                model=self.model,
                tokenizer=self.tokenizer,
                device=0 if self.device.type == "cuda" else -1,
                aggregation_strategy="simple"  # 같은 개체를 하나로 묶음
            )

            load_time = time.time() - start_time
            logger.info(
                f"Model loaded successfully in {load_time:.2f}s "
                f"(device: {self.device})"
            )

            self._loaded = True

        except Exception as e:
            logger.error(f"Failed to load NER model: {e}", exc_info=True)
            self._loaded = False
            raise

    def is_loaded(self) -> bool:
        """모델 로딩 상태 확인"""
        return getattr(self, '_loaded', False)

    def predict(
        self,
        text: str,
        confidence_threshold: Optional[float] = None
    ) -> List[dict]:
        """
        단일 텍스트에서 개체명 추출

        Args:
            text: 분석할 텍스트
            confidence_threshold: 신뢰도 임계값 (기본값: config.default_confidence_threshold)

        Returns:
            추출된 개체명 리스트
            [
                {
                    "word": "삼성전자",
                    "entity": "ORG",
                    "confidence": 0.95,
                    "start": 0,
                    "end": 4
                },
                ...
            ]
        """
        if not self.is_loaded():
            raise RuntimeError("Model is not loaded")

        if not text or len(text.strip()) == 0:
            return []

        # 토크나이징 후 최대 길이(512) 체크 및 자르기
        # BERT 모델의 최대 토큰 수는 512이므로 truncation=True로 자동 처리
        max_tokens = 510  # [CLS]와 [SEP] 토큰을 위해 510으로 설정

        # 토크나이징 (truncation 적용)
        encoded = self.tokenizer.encode(
            text,
            truncation=True,
            max_length=max_tokens,
            add_special_tokens=False  # 수동으로 처리
        )

        if len(encoded) >= max_tokens:
            # 토큰 수가 최대치에 도달하면 디코딩하여 잘린 텍스트로 변환
            text = self.tokenizer.decode(encoded, skip_special_tokens=True)
            logger.warning(
                f"Text truncated to {max_tokens} tokens (original had {len(encoded)} tokens)"
            )

        # 신뢰도 임계값 설정
        threshold = confidence_threshold if confidence_threshold is not None \
                    else config.default_confidence_threshold

        try:
            # NER 추론 (pipeline은 truncation 파라미터를 받지 않음)
            with torch.no_grad():
                results = self.ner_pipeline(text)

            # 결과 필터링 및 변환
            entities = []
            for result in results:
                if result['score'] >= threshold:
                    # entity_group에서 "B-" 접두사 제거
                    entity_type = result['entity_group'].replace('B-', '').replace('I-', '')

                    entities.append({
                        "word": result['word'],
                        "entity": entity_type,
                        "confidence": float(result['score']),
                        "start": int(result['start']),
                        "end": int(result['end'])
                    })

            logger.debug(
                f"Extracted {len(entities)} entities from text "
                f"(length: {len(text)}, threshold: {threshold})"
            )

            return entities

        except Exception as e:
            logger.error(f"NER prediction failed: {e}", exc_info=True)
            raise

    def predict_batch(
        self,
        texts: List[str],
        confidence_threshold: Optional[float] = None
    ) -> List[List[dict]]:
        """
        배치 텍스트에서 개체명 추출

        Args:
            texts: 분석할 텍스트 리스트
            confidence_threshold: 신뢰도 임계값

        Returns:
            각 텍스트별 개체명 리스트
            [
                [{word: "삼성전자", entity: "ORG", ...}],
                [{word: "현대차", entity: "ORG", ...}],
                ...
            ]
        """
        if not self.is_loaded():
            raise RuntimeError("Model is not loaded")

        if not texts or len(texts) == 0:
            return []

        # 배치 크기 체크
        if len(texts) > config.max_batch_size:
            logger.warning(
                f"Batch size ({len(texts)}) exceeds maximum "
                f"({config.max_batch_size}), processing first {config.max_batch_size}"
            )
            texts = texts[:config.max_batch_size]

        try:
            # 각 텍스트 개별 처리
            # (HuggingFace pipeline의 배치 처리는 메모리 효율성이 떨어질 수 있음)
            results = []
            for text in texts:
                entities = self.predict(text, confidence_threshold)
                results.append(entities)

            logger.debug(f"Processed batch of {len(texts)} texts")

            return results

        except Exception as e:
            logger.error(f"Batch NER prediction failed: {e}", exc_info=True)
            raise

    def get_model_info(self) -> dict:
        """모델 정보 반환"""
        return {
            "model_name": config.model_name,
            "device": str(self.device) if hasattr(self, 'device') else "unknown",
            "loaded": self.is_loaded(),
            "max_text_length": config.max_text_length,
            "max_batch_size": config.max_batch_size,
            "default_confidence_threshold": config.default_confidence_threshold
        }


# Singleton 인스턴스 생성 (서버 시작 시 한 번만)
def get_ner_model() -> NerModel:
    """NER 모델 인스턴스 가져오기"""
    return NerModel()
