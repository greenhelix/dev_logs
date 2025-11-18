package com.innopia.bist.ver2.data.repository;

import com.innopia.bist.ver2.util.TestResult;
import com.innopia.bist.ver2.util.TestStatus;

/**
 * 모든 테스트의 기본 인터페이스
 */
public interface Test {

    /**
     * 테스트 시작
     */
    void startTest(TestCallback callback);

    /**
     * 테스트 중지
     */
    void stopTest();

    /**
     * 테스트 일시정지
     */
    void pauseTest();

    /**
     * 테스트 재개
     */
    void resumeTest();

    /**
     * 현재 테스트 상태 가져오기
     */
    TestStatus getTestStatus();

    /**
     * 테스트 결과 콜백 인터페이스
     */
    interface TestCallback {
        void onTestStarted();
        void onTestProgress(int progress, String message);
        void onTestCompleted(TestResult result);
        void onTestError(String error);
    }
}
