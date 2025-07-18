package com.innopia.bist.test;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class HdmiTest implements Test {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final String TAG = "HdmiTest";

    @Override
    public void runManualTest(Map<String, Object> params, Consumer<String> callback) {

    }



//    @Override
//    public void runAutoTest(Map<String, Object> params, Consumer<String> callback) {
//
//    }
}
