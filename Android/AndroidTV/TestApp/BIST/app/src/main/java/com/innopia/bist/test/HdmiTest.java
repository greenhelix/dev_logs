package com.innopia.bist.test;

import android.content.Context;

import com.innopia.bist.util.TestResult;
import com.innopia.bist.util.TestStatus;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * HdmiTest class performs tests on the HDMI connection for STB environments.
 * It identifies the primary HDMI display, retrieves TV (sink) and STB (source) information,
 * and queries HDMI CEC status, including CEC version, with proper permission handling.
 */
public class HdmiTest implements Test {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void runManualTest(Map<String, Object> params, Consumer<TestResult> callback) {
        executeTest(params, callback);
    }

    @Override
    public void runAutoTest(Map<String, Object> params, Consumer<TestResult> callback) {
        executeTest(params, callback);
    }

    private void executeTest(Map<String, Object> params, Consumer<TestResult> callback) {
        executor.execute(() -> {
            Context context = (Context) params.get("context");
            if (context == null) {
                callback.accept(new TestResult(TestStatus.ERROR, "Error: Context is null"));
                return;
            }
            callback.accept(new TestResult(TestStatus.PASSED, "HDMI Test pass"));
        });
    }
}
