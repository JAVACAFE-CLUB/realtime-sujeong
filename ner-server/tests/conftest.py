"""
pytest 공통 설정 및 fixture

모든 테스트에서 공통으로 사용하는 fixture를 정의합니다.
"""

import pytest
import sys
from pathlib import Path

# 프로젝트 루트를 Python path에 추가
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root))


@pytest.fixture(scope="session")
def project_root_path():
    """프로젝트 루트 경로 반환"""
    return project_root


@pytest.fixture(scope="session")
def ner_model():
    """
    세션 레벨 NER 모델 fixture

    모든 테스트에서 동일한 모델 인스턴스를 공유하여
    모델 로딩 시간을 절약합니다.
    """
    from app.models import get_ner_model
    return get_ner_model()


@pytest.fixture
def sample_texts():
    """테스트용 샘플 텍스트"""
    return {
        "with_entities": "삼성전자 이재용 회장이 서울에서 신제품을 발표했다.",
        "simple": "삼성전자가 신제품을 발표했다.",
        "multiple_entities": "현대자동차는 서울 강남구에 본사가 있다.",
        "date_time": "BTS는 2025년 1월 서울에서 콘서트를 열었다.",
        "no_entities": "오늘 날씨가 좋다.",
        "empty": "",
        "whitespace": "   ",
        "long": "삼성전자 이재용 회장이 서울에서 신제품을 발표했다. " * 100,
    }


@pytest.fixture
def sample_batch_texts():
    """테스트용 배치 텍스트"""
    return [
        "삼성전자가 신제품을 발표했다.",
        "현대자동차는 서울 강남구에 본사가 있다.",
        "BTS는 2025년 1월 콘서트를 열었다."
    ]
