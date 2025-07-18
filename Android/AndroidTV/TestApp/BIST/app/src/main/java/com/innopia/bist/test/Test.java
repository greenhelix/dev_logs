package com.innopia.bist.test;

import java.util.Map;
import java.util.function.Consumer;

public interface Test {
    void runManualTest(Map<String, Object> params, Consumer<String> callback);

//    void runAutoTest(Map<String, Object> params, Consumer<String> callback);
}
