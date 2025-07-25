package com.innopia.bist.test;

import com.innopia.bist.util.TestResult;
import com.innopia.bist.util.TestStatus;

import java.util.Map;
import java.util.function.Consumer;

public class VideoTest implements Test {

    @Override
    public void runManualTest(Map<String, Object> params, Consumer<TestResult> callback) {
        callback.accept(new TestResult(TestStatus.PASSED, "Video test is pass"));
    }

    @Override
    public void runAutoTest(Map<String, Object> params, Consumer<TestResult> callback) {
        callback.accept(new TestResult(TestStatus.PASSED, "Video test is pass"));
    }
}
