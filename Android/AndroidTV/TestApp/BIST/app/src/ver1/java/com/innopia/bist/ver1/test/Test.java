package com.innopia.bist.ver1.test;

import com.innopia.bist.ver1.util.TestResult;

import java.util.Map;
import java.util.function.Consumer;

public interface Test {
    void runManualTest(Map<String, Object> params, Consumer<TestResult> callback);
    void runAutoTest(Map<String, Object> params, Consumer<TestResult> callback);
}
