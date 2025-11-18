package com.innopia.bist.ver2.util;

/**
 * 테스트 결과를 담는 클래스
 */
public class TestResult {

    private final TestStatus status;
    private final String message;
    private final Object data;
    private final long timestamp;
    private final boolean success;

    public TestResult(TestStatus status, String message, Object data) {
        this.status = status;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
        this.success = (status == TestStatus.COMPLETED);
    }

    public TestResult(TestStatus status, Object data) {
        this(status, "", data);
    }

    public TestStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isSuccess() {
        return success;
    }

    @Override
    public String toString() {
        return "TestResult{" +
                "status=" + status +
                ", message='" + message + '\'' +
                ", success=" + success +
                ", timestamp=" + timestamp +
                '}';
    }
}
