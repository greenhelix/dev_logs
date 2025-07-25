package com.innopia.bist.test;

import android.content.Context;

import com.innopia.bist.util.TestResult;
import com.innopia.bist.util.TestStatus;

import java.io.File;
import java.util.Map;
import java.util.function.Consumer;

public class RcuTest implements Test {

    @Override
    public void runManualTest(Map<String, Object> params, Consumer<TestResult> callback) {
        executeTest(params, callback);
    }

    @Override
    public void runAutoTest(Map<String, Object> params, Consumer<TestResult> callback) {
        executeTest(params, callback);
    }

    private void executeTest(Map<String, Object> params, Consumer<TestResult> callback) {

    }
}
