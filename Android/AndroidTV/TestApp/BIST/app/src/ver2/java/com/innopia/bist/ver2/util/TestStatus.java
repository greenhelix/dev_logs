package com.innopia.bist.ver2.util;

/**
 * 테스트 상태를 나타내는 Enum
 */
public enum TestStatus {
    IDLE,           // 테스트 대기 중
    RUNNING,        // 테스트 실행 중
    PAUSED,         // 테스트 일시정지
    COMPLETED,      // 테스트 완료
    ERROR,          // 테스트 에러
    CANCELLED       // 테스트 취소됨
}