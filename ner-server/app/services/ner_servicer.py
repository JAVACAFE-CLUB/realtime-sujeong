"""
NER gRPC Servicer

gRPC 서비스 구현체 (NerService의 실제 동작 로직)
"""

import logging
import time
from typing import List
import grpc

from app.generated import ner_pb2, ner_pb2_grpc
from app.models import get_ner_model
from app.config import config

logger = logging.getLogger(__name__)


class NerServicer(ner_pb2_grpc.NerServiceServicer):
    """
    gRPC NerService 구현체

    Protocol Buffers에 정의된 NerService를 실제로 구현하는 클래스입니다.
    모든 RPC 메서드는 여기서 구현됩니다.
    """

    def __init__(self):
        """서비스 초기화 및 모델 로딩"""
        logger.info("Initializing NerServicer...")
        self.model = get_ner_model()

        if not self.model.is_loaded():
            raise RuntimeError("Failed to initialize NER model")

        logger.info("NerServicer initialized successfully")

    def ExtractEntities(
        self,
        request: ner_pb2.NerRequest,
        context: grpc.ServicerContext
    ) -> ner_pb2.NerResponse:
        """
        단일 텍스트에서 개체명 추출 (gRPC 메서드)

        Args:
            request: NerRequest (text, confidence_threshold)
            context: gRPC 컨텍스트 (에러 처리용)

        Returns:
            NerResponse (entities, processing_time_ms)
        """
        start_time = time.time()

        try:
            # 1. 입력 검증
            if not request.text or len(request.text.strip()) == 0:
                context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
                context.set_details("Text cannot be empty")
                logger.warning("Received empty text in ExtractEntities")
                return ner_pb2.NerResponse()

            # 텍스트 길이 체크
            if len(request.text) > config.max_text_length:
                context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
                context.set_details(
                    f"Text length ({len(request.text)}) exceeds "
                    f"maximum ({config.max_text_length})"
                )
                logger.warning(
                    f"Text length {len(request.text)} exceeds maximum "
                    f"{config.max_text_length}"
                )
                return ner_pb2.NerResponse()

            # 2. confidence_threshold 설정 (0이면 기본값 사용)
            threshold = (
                request.confidence_threshold
                if request.confidence_threshold > 0
                else config.default_confidence_threshold
            )

            # threshold 범위 검증 (0.0 ~ 1.0)
            if not (0.0 <= threshold <= 1.0):
                context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
                context.set_details(
                    f"confidence_threshold must be between 0.0 and 1.0, "
                    f"got {threshold}"
                )
                logger.warning(f"Invalid confidence threshold: {threshold}")
                return ner_pb2.NerResponse()

            # 3. NER 모델 추론
            logger.debug(
                f"Processing text (length={len(request.text)}, "
                f"threshold={threshold})"
            )

            entities = self.model.predict(
                text=request.text,
                confidence_threshold=threshold
            )

            # 4. 결과를 Protocol Buffer 메시지로 변환
            pb_entities = []
            for entity in entities:
                pb_entity = ner_pb2.NerEntity(
                    word=entity["word"],
                    entity=entity["entity"],
                    confidence=entity["confidence"],
                    start=entity["start"],
                    end=entity["end"]
                )
                pb_entities.append(pb_entity)

            # 5. 처리 시간 계산
            processing_time_ms = int((time.time() - start_time) * 1000)

            # 6. 응답 생성
            response = ner_pb2.NerResponse(
                entities=pb_entities,
                processing_time_ms=processing_time_ms
            )

            logger.info(
                f"ExtractEntities completed: {len(pb_entities)} entities, "
                f"{processing_time_ms}ms"
            )

            return response

        except Exception as e:
            # 예외 발생 시 gRPC 에러 설정
            context.set_code(grpc.StatusCode.INTERNAL)
            context.set_details(f"Internal error: {str(e)}")
            logger.error(
                f"ExtractEntities failed: {e}",
                exc_info=True
            )
            return ner_pb2.NerResponse()

    def ExtractEntitiesBatch(
        self,
        request: ner_pb2.NerBatchRequest,
        context: grpc.ServicerContext
    ) -> ner_pb2.NerBatchResponse:
        """
        배치 텍스트에서 개체명 추출 (gRPC 메서드)

        Args:
            request: NerBatchRequest (texts, confidence_threshold)
            context: gRPC 컨텍스트 (에러 처리용)

        Returns:
            NerBatchResponse (results, total_texts, processing_time_ms)
        """
        start_time = time.time()

        try:
            # 1. 입력 검증
            if not request.texts or len(request.texts) == 0:
                context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
                context.set_details("Texts list cannot be empty")
                logger.warning("Received empty texts list in ExtractEntitiesBatch")
                return ner_pb2.NerBatchResponse()

            # 배치 크기 체크
            if len(request.texts) > config.max_batch_size:
                context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
                context.set_details(
                    f"Batch size ({len(request.texts)}) exceeds "
                    f"maximum ({config.max_batch_size})"
                )
                logger.warning(
                    f"Batch size {len(request.texts)} exceeds maximum "
                    f"{config.max_batch_size}"
                )
                return ner_pb2.NerBatchResponse()

            # 2. confidence_threshold 설정
            threshold = (
                request.confidence_threshold
                if request.confidence_threshold > 0
                else config.default_confidence_threshold
            )

            # threshold 범위 검증
            if not (0.0 <= threshold <= 1.0):
                context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
                context.set_details(
                    f"confidence_threshold must be between 0.0 and 1.0, "
                    f"got {threshold}"
                )
                logger.warning(f"Invalid confidence threshold: {threshold}")
                return ner_pb2.NerBatchResponse()

            # 3. 배치 NER 추론
            logger.debug(
                f"Processing batch (size={len(request.texts)}, "
                f"threshold={threshold})"
            )

            batch_entities = self.model.predict_batch(
                texts=list(request.texts),
                confidence_threshold=threshold
            )

            # 4. 결과를 Protocol Buffer 메시지로 변환
            pb_results = []
            for i, (text, entities) in enumerate(zip(request.texts, batch_entities)):
                # 각 텍스트별 개체명 변환
                pb_entities = []
                for entity in entities:
                    pb_entity = ner_pb2.NerEntity(
                        word=entity["word"],
                        entity=entity["entity"],
                        confidence=entity["confidence"],
                        start=entity["start"],
                        end=entity["end"]
                    )
                    pb_entities.append(pb_entity)

                # 배치 결과 생성
                pb_result = ner_pb2.NerBatchResult(
                    text=text,
                    entities=pb_entities
                )
                pb_results.append(pb_result)

            # 5. 처리 시간 계산
            processing_time_ms = int((time.time() - start_time) * 1000)

            # 6. 응답 생성
            response = ner_pb2.NerBatchResponse(
                results=pb_results,
                total_texts=len(request.texts),
                processing_time_ms=processing_time_ms
            )

            total_entities = sum(len(result.entities) for result in pb_results)
            logger.info(
                f"ExtractEntitiesBatch completed: {len(request.texts)} texts, "
                f"{total_entities} total entities, {processing_time_ms}ms"
            )

            return response

        except Exception as e:
            # 예외 발생 시 gRPC 에러 설정
            context.set_code(grpc.StatusCode.INTERNAL)
            context.set_details(f"Internal error: {str(e)}")
            logger.error(
                f"ExtractEntitiesBatch failed: {e}",
                exc_info=True
            )
            return ner_pb2.NerBatchResponse()