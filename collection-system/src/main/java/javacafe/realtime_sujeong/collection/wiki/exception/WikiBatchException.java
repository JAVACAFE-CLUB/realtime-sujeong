package javacafe.realtime_sujeong.collection.wiki.exception;

/**
 * Wiki 배치 작업 관련 예외
 */
public class WikiBatchException extends RuntimeException {

    public WikiBatchException(String message) {
        super(message);
    }

    public WikiBatchException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 배치가 이미 실행 중일 때
     */
    public static class AlreadyRunningException extends WikiBatchException {
        public AlreadyRunningException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * 배치 재시작 실패
     */
    public static class RestartFailedException extends WikiBatchException {
        public RestartFailedException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * 이미 완료된 배치
     */
    public static class AlreadyCompletedException extends WikiBatchException {
        public AlreadyCompletedException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * 잘못된 파라미터
     */
    public static class InvalidParametersException extends WikiBatchException {
        public InvalidParametersException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * 배치 실행 실패
     */
    public static class ExecutionFailedException extends WikiBatchException {
        public ExecutionFailedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}