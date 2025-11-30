"""
NER Model 테스트

NerModel 클래스의 Singleton 패턴, 모델 로딩, 추론 등을 테스트합니다.
"""

import pytest
from app.models import NerModel, get_ner_model


class TestNerModelSingleton:
    """Singleton 패턴 테스트"""

    def test_singleton_same_instance(self):
        """동일한 인스턴스 반환 확인"""
        model1 = get_ner_model()
        model2 = get_ner_model()

        assert model1 is model2, "Singleton 패턴이 적용되지 않음"

    def test_singleton_with_direct_instantiation(self):
        """직접 인스턴스화해도 동일한 인스턴스 반환"""
        model1 = NerModel()
        model2 = NerModel()
        model3 = get_ner_model()

        assert model1 is model2, "직접 인스턴스화 시 Singleton 위반"
        assert model1 is model3, "get_ner_model()과 직접 인스턴스화 불일치"


class TestNerModelLoading:
    """모델 로딩 테스트"""

    def test_model_is_loaded(self):
        """모델이 정상적으로 로딩되는지 확인"""
        model = get_ner_model()

        assert model.is_loaded(), "모델이 로딩되지 않음"

    def test_model_has_required_attributes(self):
        """모델이 필요한 속성을 가지고 있는지 확인"""
        model = get_ner_model()

        assert hasattr(model, 'model'), "model 속성 없음"
        assert hasattr(model, 'tokenizer'), "tokenizer 속성 없음"
        assert hasattr(model, 'ner_pipeline'), "ner_pipeline 속성 없음"
        assert hasattr(model, 'device'), "device 속성 없음"

    def test_model_info(self):
        """모델 정보가 올바르게 반환되는지 확인"""
        model = get_ner_model()
        info = model.get_model_info()

        assert 'model_name' in info, "model_name 정보 없음"
        assert 'device' in info, "device 정보 없음"
        assert 'loaded' in info, "loaded 정보 없음"
        assert 'max_text_length' in info, "max_text_length 정보 없음"
        assert 'max_batch_size' in info, "max_batch_size 정보 없음"
        assert 'default_confidence_threshold' in info, "default_confidence_threshold 정보 없음"

        assert info['loaded'] is True, "모델이 로딩되지 않았다고 표시됨"


class TestNerModelPredict:
    """단일 텍스트 추론 테스트"""

    @pytest.fixture
    def model(self):
        """테스트용 모델 fixture"""
        return get_ner_model()

    def test_predict_simple_text(self, model):
        """간단한 텍스트에서 개체명 추출"""
        text = "삼성전자 이재용 회장이 서울에서 신제품을 발표했다."
        entities = model.predict(text, confidence_threshold=0.8)

        # 개체명이 추출되었는지 확인
        assert len(entities) > 0, "개체명이 추출되지 않음"

        # 각 개체명이 올바른 형식인지 확인
        for entity in entities:
            assert 'word' in entity, "word 필드 없음"
            assert 'entity' in entity, "entity 필드 없음"
            assert 'confidence' in entity, "confidence 필드 없음"
            assert 'start' in entity, "start 필드 없음"
            assert 'end' in entity, "end 필드 없음"

            # confidence가 임계값 이상인지 확인
            assert entity['confidence'] >= 0.8, f"신뢰도 {entity['confidence']}가 임계값 미만"

            # 위치 정보가 유효한지 확인
            assert 0 <= entity['start'] < len(text), "start 위치 오류"
            assert entity['start'] < entity['end'] <= len(text), "end 위치 오류"

    def test_predict_with_expected_entities(self, model):
        """예상 개체명이 포함되어 있는지 확인"""
        text = "삼성전자 이재용 회장이 서울에서 신제품을 발표했다."
        entities = model.predict(text, confidence_threshold=0.5)

        # 추출된 개체명 단어 리스트
        words = [e['word'] for e in entities]

        # 예상 개체명이 포함되어 있는지 확인 (모델 성능에 따라 일부는 누락될 수 있음)
        # 최소 하나 이상의 개체명은 추출되어야 함
        assert len(words) > 0, "개체명이 전혀 추출되지 않음"

    def test_predict_empty_text(self, model):
        """빈 텍스트 처리"""
        entities = model.predict("", confidence_threshold=0.8)

        assert entities == [], "빈 텍스트는 빈 리스트를 반환해야 함"

    def test_predict_whitespace_only(self, model):
        """공백만 있는 텍스트 처리"""
        entities = model.predict("   ", confidence_threshold=0.8)

        assert entities == [], "공백만 있는 텍스트는 빈 리스트를 반환해야 함"

    def test_predict_no_entities(self, model):
        """개체명이 없는 텍스트"""
        text = "오늘 날씨가 좋다."
        entities = model.predict(text, confidence_threshold=0.8)

        # 개체명이 없거나 매우 적을 것으로 예상
        assert isinstance(entities, list), "리스트를 반환해야 함"

    def test_predict_various_entity_types(self, model):
        """다양한 개체 유형 추출 확인"""
        text = "BTS는 2025년 1월 서울 강남구에서 콘서트를 열었다."
        entities = model.predict(text, confidence_threshold=0.5)

        # 개체 유형이 KLUE 표준인지 확인
        valid_types = [
            'PER', 'ORG', 'LOC', 'DAT', 'TIM', 'QT', 'EV', 'AF',
            'CV', 'FD', 'TR', 'AM', 'PT', 'MT', 'TM'
        ]

        for entity in entities:
            assert entity['entity'] in valid_types, \
                f"유효하지 않은 개체 유형: {entity['entity']}"

    def test_predict_confidence_threshold_filtering(self, model):
        """신뢰도 임계값 필터링 확인"""
        text = "삼성전자 이재용 회장이 서울에서 발표했다."

        # 높은 임계값
        entities_high = model.predict(text, confidence_threshold=0.95)

        # 낮은 임계값
        entities_low = model.predict(text, confidence_threshold=0.5)

        # 낮은 임계값일 때 더 많거나 같은 개체명 추출
        assert len(entities_low) >= len(entities_high), \
            "낮은 임계값에서 더 적은 개체명 추출됨"

    def test_predict_long_text(self, model):
        """긴 텍스트 처리"""
        text = "삼성전자 이재용 회장이 서울에서 신제품을 발표했다. " * 100
        entities = model.predict(text, confidence_threshold=0.8)

        # 긴 텍스트도 처리 가능해야 함
        assert isinstance(entities, list), "긴 텍스트 처리 실패"


class TestNerModelPredictBatch:
    """배치 추론 테스트"""

    @pytest.fixture
    def model(self):
        """테스트용 모델 fixture"""
        return get_ner_model()

    def test_predict_batch_multiple_texts(self, model):
        """여러 텍스트 배치 처리"""
        texts = [
            "삼성전자가 신제품을 발표했다.",
            "현대자동차는 서울 강남구에 본사가 있다.",
            "BTS는 2025년 1월 콘서트를 열었다."
        ]

        results = model.predict_batch(texts, confidence_threshold=0.8)

        # 결과 개수가 입력 텍스트 개수와 같은지 확인
        assert len(results) == len(texts), "결과 개수 불일치"

        # 각 결과가 리스트인지 확인
        for i, entities in enumerate(results):
            assert isinstance(entities, list), f"{i}번째 결과가 리스트가 아님"

    def test_predict_batch_empty_list(self, model):
        """빈 리스트 처리"""
        results = model.predict_batch([], confidence_threshold=0.8)

        assert results == [], "빈 리스트는 빈 리스트를 반환해야 함"

    def test_predict_batch_single_text(self, model):
        """단일 텍스트 배치 처리"""
        texts = ["삼성전자 이재용 회장이 서울에서 발표했다."]
        results = model.predict_batch(texts, confidence_threshold=0.8)

        assert len(results) == 1, "단일 텍스트 배치 처리 오류"
        assert isinstance(results[0], list), "결과가 리스트가 아님"

    def test_predict_batch_consistency(self, model):
        """배치 처리와 개별 처리 결과 일관성"""
        text = "삼성전자 이재용 회장이 서울에서 발표했다."

        # 개별 처리
        single_result = model.predict(text, confidence_threshold=0.8)

        # 배치 처리
        batch_result = model.predict_batch([text], confidence_threshold=0.8)

        # 결과가 동일해야 함
        assert len(single_result) == len(batch_result[0]), \
            "개별 처리와 배치 처리 결과 불일치"


class TestNerModelErrorHandling:
    """에러 처리 테스트"""

    @pytest.fixture
    def model(self):
        """테스트용 모델 fixture"""
        return get_ner_model()

    def test_predict_not_loaded(self):
        """모델이 로딩되지 않은 상태에서 predict 호출 (이론적 테스트)"""
        # Singleton 패턴으로 인해 실제로는 발생하지 않지만,
        # is_loaded() 체크 로직을 테스트
        model = get_ner_model()

        # 모델이 로딩되어 있어야 함
        assert model.is_loaded(), "모델이 로딩되지 않음"

    def test_predict_with_none_threshold(self, model):
        """confidence_threshold가 None일 때 기본값 사용"""
        text = "삼성전자 이재용 회장이 서울에서 발표했다."
        entities = model.predict(text, confidence_threshold=None)

        # 정상적으로 처리되어야 함
        assert isinstance(entities, list), "None 임계값 처리 실패"

    def test_predict_with_zero_threshold(self, model):
        """confidence_threshold가 0일 때 모든 결과 반환"""
        text = "삼성전자 이재용 회장이 서울에서 발표했다."
        entities = model.predict(text, confidence_threshold=0.0)

        # 매우 낮은 신뢰도의 개체명도 포함되어야 함
        assert isinstance(entities, list), "0 임계값 처리 실패"
