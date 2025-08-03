# Kafka & Elasticsearch 기반 Spring Boot 프로젝트

이 프로젝트는 **Apache Kafka**를 통해 데이터를 비동기 처리하고, **Elasticsearch**를 이용해 실시간 검색 기능을 구현하고자 합니다.
백엔드는 **Spring Boot 3.5.4** 및 **Java 24**을 사용하여 구축되었습니다.

---

## 📌 주요 기술 스택

| 기술 | 설명 |
|------|------|
| Java 24 | Java 언어 |
| Spring Boot 3.5.4 | 최신 안정판, Java 24 완전 호환 |
| Apache Kafka | 메시지 큐 시스템, 실시간 데이터 처리 |
| Spring for Apache Kafka | Kafka와 통합을 위한 Spring 지원 |
| Elasticsearch 8.x | 분산 검색 및 분석 엔진 |
| Spring Data Elasticsearch | Elasticsearch와의 간편한 통합 |
| Testcontainers | Kafka/Elasticsearch 테스트 컨테이너 |
| Micrometer + Actuator | 시스템 관측 및 Prometheus 연동 가능 |
| Lombok, Validation 등 | 편의성 및 유효성 검증 도구 |

---

## ✅ 주요 기능

- Kafka Producer/Consumer 기반 비동기 메시지 처리
- Elasticsearch를 통한 실시간 검색 기능
- RESTful API 구성 (Spring Web)
- Spring Boot Actuator를 활용한 시스템 상태 모니터링
- Testcontainers를 활용한 Kafka, Elasticsearch 통합 테스트 환경

---

## 🏗️ 프로젝트 초기 설정 (start.spring.io)

- **Spring Boot**: `3.5.4`
- **Java**: `24`
- **Dependencies**:
  - Spring Web
  - Spring for Apache Kafka
  - Spring Data Elasticsearch
  - Spring Boot Actuator
  - Validation
  - Lombok
  - Spring Configuration Processor
  - Testcontainers

---

## ⚙️ Elasticsearch & Kafka 버전 권장

| 구성 요소 | 권장 버전 |
|-----------|------------|
| Kafka Broker | 3.x 이상 |
| Elasticsearch | 8.18.x |
| Spring Data Elasticsearch | 5.5.x (Spring Boot BOM으로 자동 관리됨) |

---
## 📮 문의 / 피드백
> 이 프로젝트는 테스트 목적 또는 마이크로서비스 아키텍처 학습용으로 제작되었습니다.
> 추가 기능/피드백은 언제든지 환영합니다!
