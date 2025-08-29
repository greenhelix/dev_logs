package com.innopia.bist.test;

import com.innopia.bist.util.TestResult;

import java.util.Map;
import java.util.function.Consumer;

public interface Test {
    void runManualTest(Map<String, Object> params, Consumer<TestResult> callback);
    void runAutoTest(Map<String, Object> params, Consumer<TestResult> callback);
}
