package com.innopia.bist.tests;

import java.util.function.Consumer;

public interface AutoTest {
    void runTest(Consumer<TestResult> onResult);
}
