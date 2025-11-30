"""
NER Servicer 테스트

gRPC NerServicer의 RPC 메서드들을 테스트합니다.
"""

import pytest
import grpc
from grpc_testing import server_from_dictionary, strict_real_time

from app.generated import ner_pb2, ner_pb2_grpc
from app.services import NerServicer


class TestNerServicerSetup:
    """Servicer 초기화 테스트"""

    def test_servicer_initialization(self):
        """Servicer가 정상적으로 초기화되는지 확인"""
        servicer = NerServicer()

        assert servicer is not None, "Servicer 초기화 실패"
        assert hasattr(servicer, 'model'), "model 속성 없음"
        assert servicer.model.is_loaded(), "모델이 로딩되지 않음"


class TestExtractEntities:
    """ExtractEntities RPC 메서드 테스트"""

    @pytest.fixture
    def servicer(self):
        """테스트용 Servicer fixture"""
        return NerServicer()

    @pytest.fixture
    def context(self):
        """Mock gRPC context"""
        class MockContext:
            def __init__(self):
                self._code = grpc.StatusCode.OK
                self._details = ""

            def set_code(self, code):
                self._code = code

            def set_details(self, details):
                self._details = details

            def code(self):
                return self._code

            def details(self):
                return self._details

        return MockContext()

    def test_extract_entities_success(self, servicer, context):
        """정상적인 NER 요청"""
        request = ner_pb2.NerRequest(
            text="삼성전자 이재용 회장이 서울에서 신제품을 발표했다.",
            confidence_threshold=0.8
        )

        response = servicer.ExtractEntities(request, context)

        # 응답이 정상적으로 반환되는지 확인
        assert isinstance(response, ner_pb2.NerResponse), "응답 타입 오류"
        assert context.code() == grpc.StatusCode.OK, "gRPC 상태 코드 오류"

        # 개체명이 추출되었는지 확인
        assert len(response.entities) > 0, "개체명이 추출되지 않음"

        # 처리 시간이 기록되었는지 확인
        assert response.processing_time_ms > 0, "처리 시간이 기록되지 않음"

        # 각 개체명 필드 확인
        for entity in response.entities:
            assert entity.word, "word 필드가 비어있음"
            assert entity.entity, "entity 필드가 비어있음"
            assert entity.confidence >= 0.8, "신뢰도가 임계값 미만"
            assert entity.start >= 0, "start 위치 오류"
            assert entity.end > entity.start, "end 위치 오류"

    def test_extract_entities_empty_text(self, servicer, context):
        """빈 텍스트 요청"""
        request = ner_pb2.NerRequest(
            text="",
            confidence_threshold=0.8
        )

        response = servicer.ExtractEntities(request, context)

        # INVALID_ARGUMENT 에러가 설정되어야 함
        assert context.code() == grpc.StatusCode.INVALID_ARGUMENT, \
            "빈 텍스트에 대한 에러 코드 미설정"
        assert "empty" in context.details().lower(), \
            "에러 메시지에 'empty' 포함 안됨"

    def test_extract_entities_whitespace_only(self, servicer, context):
        """공백만 있는 텍스트 요청"""
        request = ner_pb2.NerRequest(
            text="   ",
            confidence_threshold=0.8
        )

        response = servicer.ExtractEntities(request, context)

        # INVALID_ARGUMENT 에러가 설정되어야 함
        assert context.code() == grpc.StatusCode.INVALID_ARGUMENT, \
            "공백 텍스트에 대한 에러 코드 미설정"

    def test_extract_entities_text_too_long(self, servicer, context):
        """텍스트 길이 초과"""
        from app.config import config

        # 최대 길이를 초과하는 텍스트
        long_text = "A" * (config.max_text_length + 1)

        request = ner_pb2.NerRequest(
            text=long_text,
            confidence_threshold=0.8
        )

        response = servicer.ExtractEntities(request, context)

        # INVALID_ARGUMENT 에러가 설정되어야 함
        assert context.code() == grpc.StatusCode.INVALID_ARGUMENT, \
            "긴 텍스트에 대한 에러 코드 미설정"
        assert "exceeds" in context.details().lower(), \
            "에러 메시지에 'exceeds' 포함 안됨"

    def test_extract_entities_invalid_threshold_high(self, servicer, context):
        """잘못된 신뢰도 임계값 (1.0 초과)"""
        request = ner_pb2.NerRequest(
            text="삼성전자 이재용 회장이 서울에서 발표했다.",
            confidence_threshold=1.5
        )

        response = servicer.ExtractEntities(request, context)

        # INVALID_ARGUMENT 에러가 설정되어야 함
        assert context.code() == grpc.StatusCode.INVALID_ARGUMENT, \
            "잘못된 임계값에 대한 에러 코드 미설정"
        assert "confidence_threshold" in context.details().lower(), \
            "에러 메시지에 'confidence_threshold' 포함 안됨"

    def test_extract_entities_invalid_threshold_low(self, servicer, context):
        """잘못된 신뢰도 임계값 (0.0 미만)"""
        request = ner_pb2.NerRequest(
            text="삼성전자 이재용 회장이 서울에서 발표했다.",
            confidence_threshold=-0.1
        )

        response = servicer.ExtractEntities(request, context)

        # INVALID_ARGUMENT 에러가 설정되어야 함
        assert context.code() == grpc.StatusCode.INVALID_ARGUMENT, \
            "음수 임계값에 대한 에러 코드 미설정"

    def test_extract_entities_default_threshold(self, servicer, context):
        """기본 임계값 사용 (0 입력 시)"""
        request = ner_pb2.NerRequest(
            text="삼성전자 이재용 회장이 서울에서 발표했다.",
            confidence_threshold=0.0  # 0이면 기본값 사용
        )

        response = servicer.ExtractEntities(request, context)

        # 정상 처리되어야 함
        assert context.code() == grpc.StatusCode.OK, "기본 임계값 처리 오류"

    def test_extract_entities_various_languages(self, servicer, context):
        """다양한 언어 혼합 텍스트"""
        request = ner_pb2.NerRequest(
            text="Samsung Electronics CEO 이재용이 Seoul에서 발표했다.",
            confidence_threshold=0.8
        )

        response = servicer.ExtractEntities(request, context)

        # 정상 처리되어야 함
        assert context.code() == grpc.StatusCode.OK, "다국어 텍스트 처리 오류"


class TestExtractEntitiesBatch:
    """ExtractEntitiesBatch RPC 메서드 테스트"""

    @pytest.fixture
    def servicer(self):
        """테스트용 Servicer fixture"""
        return NerServicer()

    @pytest.fixture
    def context(self):
        """Mock gRPC context"""
        class MockContext:
            def __init__(self):
                self._code = grpc.StatusCode.OK
                self._details = ""

            def set_code(self, code):
                self._code = code

            def set_details(self, details):
                self._details = details

            def code(self):
                return self._code

            def details(self):
                return self._details

        return MockContext()

    def test_extract_entities_batch_success(self, servicer, context):
        """정상적인 배치 NER 요청"""
        request = ner_pb2.NerBatchRequest(
            texts=[
                "삼성전자가 신제품을 발표했다.",
                "현대자동차는 서울 강남구에 본사가 있다.",
                "BTS는 2025년 1월 콘서트를 열었다."
            ],
            confidence_threshold=0.8
        )

        response = servicer.ExtractEntitiesBatch(request, context)

        # 응답이 정상적으로 반환되는지 확인
        assert isinstance(response, ner_pb2.NerBatchResponse), "응답 타입 오류"
        assert context.code() == grpc.StatusCode.OK, "gRPC 상태 코드 오류"

        # 결과 개수 확인
        assert len(response.results) == 3, "결과 개수 불일치"
        assert response.total_texts == 3, "total_texts 불일치"

        # 처리 시간 확인
        assert response.processing_time_ms > 0, "처리 시간이 기록되지 않음"

        # 각 결과 확인
        for i, result in enumerate(response.results):
            assert result.text == request.texts[i], f"{i}번째 텍스트 불일치"
            assert isinstance(result.entities, list) or hasattr(result, 'entities'), \
                f"{i}번째 결과의 entities 필드 오류"

    def test_extract_entities_batch_empty_list(self, servicer, context):
        """빈 리스트 요청"""
        request = ner_pb2.NerBatchRequest(
            texts=[],
            confidence_threshold=0.8
        )

        response = servicer.ExtractEntitiesBatch(request, context)

        # INVALID_ARGUMENT 에러가 설정되어야 함
        assert context.code() == grpc.StatusCode.INVALID_ARGUMENT, \
            "빈 리스트에 대한 에러 코드 미설정"
        assert "empty" in context.details().lower(), \
            "에러 메시지에 'empty' 포함 안됨"

    def test_extract_entities_batch_too_large(self, servicer, context):
        """배치 크기 초과"""
        from app.config import config

        # 최대 배치 크기를 초과하는 요청
        large_batch = ["텍스트"] * (config.max_batch_size + 1)

        request = ner_pb2.NerBatchRequest(
            texts=large_batch,
            confidence_threshold=0.8
        )

        response = servicer.ExtractEntitiesBatch(request, context)

        # INVALID_ARGUMENT 에러가 설정되어야 함
        assert context.code() == grpc.StatusCode.INVALID_ARGUMENT, \
            "큰 배치에 대한 에러 코드 미설정"
        assert "exceeds" in context.details().lower(), \
            "에러 메시지에 'exceeds' 포함 안됨"

    def test_extract_entities_batch_single_text(self, servicer, context):
        """단일 텍스트 배치"""
        request = ner_pb2.NerBatchRequest(
            texts=["삼성전자 이재용 회장이 서울에서 발표했다."],
            confidence_threshold=0.8
        )

        response = servicer.ExtractEntitiesBatch(request, context)

        # 정상 처리되어야 함
        assert context.code() == grpc.StatusCode.OK, "단일 배치 처리 오류"
        assert len(response.results) == 1, "결과 개수 오류"
        assert response.total_texts == 1, "total_texts 오류"

    def test_extract_entities_batch_mixed_content(self, servicer, context):
        """다양한 내용의 배치"""
        request = ner_pb2.NerBatchRequest(
            texts=[
                "삼성전자 이재용 회장이 서울에서 발표했다.",  # 개체명 있음
                "오늘 날씨가 좋다.",  # 개체명 없을 가능성
                "",  # 빈 문자열은 서버에서 필터링되지 않음 (각 텍스트는 개별 처리)
            ],
            confidence_threshold=0.8
        )

        response = servicer.ExtractEntitiesBatch(request, context)

        # 배치 요청은 성공해야 함
        # (개별 텍스트 에러는 배치 레벨에서 처리하지 않음)
        assert isinstance(response, ner_pb2.NerBatchResponse), "응답 타입 오류"

    def test_extract_entities_batch_invalid_threshold(self, servicer, context):
        """잘못된 신뢰도 임계값"""
        request = ner_pb2.NerBatchRequest(
            texts=["삼성전자가 발표했다.", "현대차는 서울에 있다."],
            confidence_threshold=2.0  # 잘못된 임계값
        )

        response = servicer.ExtractEntitiesBatch(request, context)

        # INVALID_ARGUMENT 에러가 설정되어야 함
        assert context.code() == grpc.StatusCode.INVALID_ARGUMENT, \
            "잘못된 임계값에 대한 에러 코드 미설정"

    def test_extract_entities_batch_consistency(self, servicer, context):
        """배치 처리와 개별 처리 일관성"""
        text = "삼성전자 이재용 회장이 서울에서 발표했다."

        # 개별 처리
        single_request = ner_pb2.NerRequest(
            text=text,
            confidence_threshold=0.8
        )
        single_response = servicer.ExtractEntities(single_request, context)

        # context 초기화
        context = type(context)()

        # 배치 처리
        batch_request = ner_pb2.NerBatchRequest(
            texts=[text],
            confidence_threshold=0.8
        )
        batch_response = servicer.ExtractEntitiesBatch(batch_request, context)

        # 결과 개수가 동일해야 함
        assert len(single_response.entities) == len(batch_response.results[0].entities), \
            "개별 처리와 배치 처리 결과 불일치"


class TestNerServicerErrorHandling:
    """Servicer 에러 처리 테스트"""

    @pytest.fixture
    def servicer(self):
        """테스트용 Servicer fixture"""
        return NerServicer()

    @pytest.fixture
    def context(self):
        """Mock gRPC context"""
        class MockContext:
            def __init__(self):
                self._code = grpc.StatusCode.OK
                self._details = ""

            def set_code(self, code):
                self._code = code

            def set_details(self, details):
                self._details = details

            def code(self):
                return self._code

            def details(self):
                return self._details

        return MockContext()

    def test_error_status_codes(self, servicer, context):
        """다양한 에러 상황에서 올바른 gRPC 상태 코드 반환"""
        # 빈 텍스트 -> INVALID_ARGUMENT
        request = ner_pb2.NerRequest(text="", confidence_threshold=0.8)
        servicer.ExtractEntities(request, context)
        assert context.code() == grpc.StatusCode.INVALID_ARGUMENT

        # Context 초기화
        context = type(context)()

        # 긴 텍스트 -> INVALID_ARGUMENT
        from app.config import config
        long_text = "A" * (config.max_text_length + 1)
        request = ner_pb2.NerRequest(text=long_text, confidence_threshold=0.8)
        servicer.ExtractEntities(request, context)
        assert context.code() == grpc.StatusCode.INVALID_ARGUMENT

    def test_error_details_message(self, servicer, context):
        """에러 상세 메시지가 설정되는지 확인"""
        request = ner_pb2.NerRequest(text="", confidence_threshold=0.8)
        servicer.ExtractEntities(request, context)

        # 에러 메시지가 비어있지 않아야 함
        assert context.details(), "에러 상세 메시지가 설정되지 않음"
        assert len(context.details()) > 0, "에러 메시지가 비어있음"
