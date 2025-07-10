package com.innopia.bist.tests;

import java.util.function.Consumer;

public interface ITest {
    void runTest(Consumer<TestResult> onResult);
}
