"""
gRPC Server

NER gRPC 서버 실행 및 관리
"""

import logging
import signal
import sys
from concurrent import futures
import grpc

from app.generated import ner_pb2_grpc
from app.services import NerServicer
from app.config import config

logger = logging.getLogger(__name__)


class NerServer:
    """
    gRPC NER 서버 클래스

    ThreadPoolExecutor를 사용하여 여러 클라이언트 요청을 동시에 처리합니다.
    """

    def __init__(self):
        """서버 초기화"""
        self.server = None
        self.servicer = None

    def start(self):
        """
        gRPC 서버 시작

        1. ThreadPoolExecutor 생성 (max_workers)
        2. NerServicer 등록
        3. 포트 바인딩
        4. 서버 시작
        """
        try:
            logger.info("Starting NER gRPC Server...")

            # 1. gRPC 서버 생성 (ThreadPoolExecutor)
            self.server = grpc.server(
                futures.ThreadPoolExecutor(max_workers=config.max_workers),
                options=[
                    ('grpc.max_send_message_length', 50 * 1024 * 1024),  # 50MB
                    ('grpc.max_receive_message_length', 50 * 1024 * 1024),  # 50MB
                ]
            )

            # 2. NerServicer 등록
            logger.info("Initializing NerServicer...")
            self.servicer = NerServicer()
            ner_pb2_grpc.add_NerServiceServicer_to_server(
                self.servicer,
                self.server
            )

            # 3. 포트 바인딩
            server_address = f"{config.grpc_host}:{config.grpc_port}"
            self.server.add_insecure_port(server_address)

            # 4. 서버 시작
            self.server.start()

            logger.info(
                f"✓ NER gRPC Server started successfully on {server_address}"
            )
            logger.info(
                f"  - Max workers: {config.max_workers}"
            )
            logger.info(
                f"  - Model: {config.model_name}"
            )
            logger.info(
                f"  - Device: {config.device}"
            )

            # 5. 서버 대기 (블로킹)
            logger.info("Server is ready to accept requests. Press Ctrl+C to stop.")
            self.server.wait_for_termination()

        except Exception as e:
            logger.error(f"Failed to start gRPC server: {e}", exc_info=True)
            self.stop()
            sys.exit(1)

    def stop(self):
        """
        gRPC 서버 종료 (Graceful Shutdown)

        1. 새 요청 거부
        2. 진행 중인 요청 완료 대기 (최대 30초)
        3. 서버 종료
        """
        if self.server:
            logger.info("Shutting down gRPC server...")

            # Graceful shutdown (30초 대기)
            self.server.stop(grace=30)

            logger.info("✓ gRPC server stopped")


def setup_signal_handlers(server: NerServer):
    """
    시그널 핸들러 설정 (SIGINT, SIGTERM)

    Ctrl+C (SIGINT) 또는 Docker stop (SIGTERM) 시 Graceful Shutdown
    """
    def signal_handler(sig, frame):
        logger.info(f"Received signal {sig}, initiating shutdown...")
        server.stop()
        sys.exit(0)

    signal.signal(signal.SIGINT, signal_handler)
    signal.signal(signal.SIGTERM, signal_handler)


def main():
    """
    메인 함수

    1. 로깅 설정
    2. 서버 생성
    3. 시그널 핸들러 등록
    4. 서버 시작
    """
    # 1. 로깅 설정
    logging.basicConfig(
        level=getattr(logging, config.log_level),
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )

    logger.info("=" * 60)
    logger.info("NER gRPC Server")
    logger.info("=" * 60)
    logger.info(f"Model: {config.model_name}")
    logger.info(f"Device: {config.device}")
    logger.info(f"Host: {config.grpc_host}")
    logger.info(f"Port: {config.grpc_port}")
    logger.info(f"Workers: {config.max_workers}")
    logger.info(f"Log Level: {config.log_level}")
    logger.info("=" * 60)

    # 2. 서버 생성
    server = NerServer()

    # 3. 시그널 핸들러 등록
    setup_signal_handlers(server)

    # 4. 서버 시작 (블로킹)
    try:
        server.start()
    except KeyboardInterrupt:
        logger.info("Server interrupted by user")
        server.stop()
    except Exception as e:
        logger.error(f"Server error: {e}", exc_info=True)
        server.stop()
        sys.exit(1)


if __name__ == "__main__":
    main()
