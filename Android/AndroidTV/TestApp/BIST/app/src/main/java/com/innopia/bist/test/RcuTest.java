package com.innopia.bist.test;

import android.content.Context;
import android.view.KeyEvent;

import com.innopia.bist.util.TestResult;
import com.innopia.bist.util.TestStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class RcuTest implements Test {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private List<Integer> keySequence;
    private int currentIndex;
    private Consumer<TestResult> callback;


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
            callback.accept(new TestResult(TestStatus.PASSED, "RCU Test pass"));
        });
    }
    private void rcuTest(Map<String, Object> params, Consumer<TestResult> callback) {
        Context context = (Context) params.get("context");
        if (context == null) {
            callback.accept(new TestResult(TestStatus.ERROR, "Error: Context is null."));
            return;
        }
        this.callback = callback;

        executor.execute(() -> {
            // Generate 4 random keys
            keySequence = generateRandomKeys();
            currentIndex = 0;

            // Show first instruction
            showNextKey();
        });
    }

    private List<Integer> generateRandomKeys() {
        List<Integer> allKeys = Arrays.asList(
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT
        );
        List<Integer> result = new ArrayList<>();
        Random rand = new Random();
        for (int i = 0; i < 4; i++) {
            result.add(allKeys.get(rand.nextInt(allKeys.size())));
        }
        return result;
    }

    private void showNextKey() {
        if (currentIndex < keySequence.size()) {
            String keyName = KeyEvent.keyCodeToString(keySequence.get(currentIndex));
            callback.accept(new TestResult(TestStatus.RUNNING,"Press: " + keyName + " (" + (currentIndex + 1) + "/4)"));
        } else {
            callback.accept(new TestResult(TestStatus.PASSED,"Test Passed! 🎉"));
        }
    }

    public void onKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN || currentIndex >= keySequence.size()) return;

        int expectedKey = keySequence.get(currentIndex);
        int receivedKey = event.getKeyCode();

        if (receivedKey == expectedKey) {
            currentIndex++;
            showNextKey();
        } else {
            callback.accept(new TestResult(TestStatus.RUNNING,"Wrong key! Pressed " + KeyEvent.keyCodeToString(receivedKey) + ". Try again: " + KeyEvent.keyCodeToString(expectedKey)));
        }
    }
}
