#!/usr/bin/env python
"""
NER 모델 테스트 스크립트

모델 로딩 및 기본 추론 테스트
"""

import sys
import logging
from app.models import get_ner_model

# 로깅 설정
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)

logger = logging.getLogger(__name__)


def test_model_loading():
    """모델 로딩 테스트"""
    print("=" * 60)
    print("NER Model Loading Test")
    print("=" * 60)

    try:
        # 모델 로딩
        model = get_ner_model()
        print(f"✓ Model loaded: {model.is_loaded()}")

        # 모델 정보 출력
        info = model.get_model_info()
        print("\nModel Info:")
        for key, value in info.items():
            print(f"  {key}: {value}")

        return model

    except Exception as e:
        print(f"✗ Model loading failed: {e}")
        sys.exit(1)


def test_single_prediction(model):
    """단일 텍스트 NER 테스트"""
    print("\n" + "=" * 60)
    print("Single Text NER Test")
    print("=" * 60)

    test_text = "삼성전자 이재용 회장이 서울에서 신제품을 발표했다."
    print(f"\nInput Text: {test_text}")

    try:
        entities = model.predict(test_text, confidence_threshold=0.8)

        print(f"\nExtracted Entities: {len(entities)}")
        for entity in entities:
            print(
                f"  - {entity['word']:<10} | "
                f"{entity['entity']:<5} | "
                f"confidence: {entity['confidence']:.3f} | "
                f"pos: ({entity['start']}, {entity['end']})"
            )

        print("\n✓ Single prediction test passed")

    except Exception as e:
        print(f"✗ Single prediction failed: {e}")
        sys.exit(1)


def test_batch_prediction(model):
    """배치 NER 테스트"""
    print("\n" + "=" * 60)
    print("Batch NER Test")
    print("=" * 60)

    test_texts = [
        "삼성전자가 신제품을 발표했다.",
        "현대자동차는 서울 강남구에 본사가 있다.",
        "BTS는 2025년 1월 서울에서 콘서트를 열었다."
    ]

    print(f"\nInput Texts: {len(test_texts)}")
    for i, text in enumerate(test_texts, 1):
        print(f"  {i}. {text}")

    try:
        results = model.predict_batch(test_texts, confidence_threshold=0.8)

        print(f"\nBatch Results:")
        for i, (text, entities) in enumerate(zip(test_texts, results), 1):
            print(f"\n  Text {i}: {text}")
            print(f"  Entities: {len(entities)}")
            for entity in entities:
                print(
                    f"    - {entity['word']:<10} | "
                    f"{entity['entity']:<5} | "
                    f"confidence: {entity['confidence']:.3f}"
                )

        print("\n✓ Batch prediction test passed")

    except Exception as e:
        print(f"✗ Batch prediction failed: {e}")
        sys.exit(1)


def test_singleton_pattern():
    """Singleton 패턴 테스트"""
    print("\n" + "=" * 60)
    print("Singleton Pattern Test")
    print("=" * 60)

    model1 = get_ner_model()
    model2 = get_ner_model()

    print(f"\nmodel1 id: {id(model1)}")
    print(f"model2 id: {id(model2)}")
    print(f"Same instance: {model1 is model2}")

    if model1 is model2:
        print("\n✓ Singleton pattern test passed")
    else:
        print("\n✗ Singleton pattern test failed")
        sys.exit(1)


def main():
    """메인 테스트 실행"""
    print("\n" + "🚀 " * 20)
    print("NER Model Test Suite")
    print("🚀 " * 20 + "\n")

    # 1. 모델 로딩 테스트
    model = test_model_loading()

    # 2. Singleton 패턴 테스트
    test_singleton_pattern()

    # 3. 단일 예측 테스트
    test_single_prediction(model)

    # 4. 배치 예측 테스트
    test_batch_prediction(model)

    # 최종 결과
    print("\n" + "✅ " * 20)
    print("All tests passed!")
    print("✅ " * 20 + "\n")


if __name__ == "__main__":
    main()