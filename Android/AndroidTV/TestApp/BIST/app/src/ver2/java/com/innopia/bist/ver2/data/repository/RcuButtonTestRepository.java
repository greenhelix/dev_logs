package com.innopia.bist.ver2.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;

import com.innopia.bist.ver2.util.TestStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RCU (Remote Control Unit) 버튼 테스트 Repository
 */
public class RcuButtonTestRepository implements Test {

    private static final String TAG = "RcuButtonTestRepository";
    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TestStatus currentStatus = TestStatus.IDLE;
    private boolean isTestRunning = false;

    // 버튼 입력 기록
    private List<ButtonEvent> buttonHistory = new ArrayList<>();
    private Map<String, Integer> buttonCountMap = new HashMap<>();

    public RcuButtonTestRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * RCU 버튼 테스트 시작
     */
    public void startButtonTest(RcuButtonTestCallback callback) {
        currentStatus = TestStatus.RUNNING;
        isTestRunning = true;

        buttonHistory.clear();
        buttonCountMap.clear();

        handler.post(() -> callback.onTestStarted("Press any remote button..."));
    }

    /**
     * 버튼 입력 기록
     */
    public void recordButtonPress(int keyCode, long responseTime) {
        if (!isTestRunning) return;

        String buttonName = getButtonName(keyCode);

        ButtonEvent event = new ButtonEvent();
        event.buttonName = buttonName;
        event.keyCode = keyCode;
        event.responseTime = responseTime;
        event.timestamp = System.currentTimeMillis();

        buttonHistory.add(event);

        // 버튼별 카운트 증가
        Integer count = buttonCountMap.get(buttonName);
        if (count == null) {
            count = 0;
        }
        buttonCountMap.put(buttonName, count + 1);

        // 콜백 호출
        RcuButtonTestResult result = new RcuButtonTestResult();
        result.buttonHistory = new ArrayList<>(buttonHistory);
        result.buttonCountMap = new HashMap<>(buttonCountMap);
        result.calculateAverageResponseTime();

        handler.post(() -> {
            if (isTestRunning && currentCallback != null) {
                currentCallback.onButtonPressed(event);
                currentCallback.onTestResultUpdated(result);
            }
        });
    }

    private RcuButtonTestCallback currentCallback;

    public void setCallback(RcuButtonTestCallback callback) {
        this.currentCallback = callback;
    }

    /**
     * 키 코드를 버튼 이름으로 변환
     */
    private String getButtonName(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP: return "UP";
            case KeyEvent.KEYCODE_DPAD_DOWN: return "DOWN";
            case KeyEvent.KEYCODE_DPAD_LEFT: return "LEFT";
            case KeyEvent.KEYCODE_DPAD_RIGHT: return "RIGHT";
            case KeyEvent.KEYCODE_DPAD_CENTER: return "OK";
            case KeyEvent.KEYCODE_ENTER: return "OK";
            case KeyEvent.KEYCODE_BACK: return "BACK";
            case KeyEvent.KEYCODE_HOME: return "HOME";
            case KeyEvent.KEYCODE_MENU: return "MENU";
            case KeyEvent.KEYCODE_VOLUME_UP: return "VOL_UP";
            case KeyEvent.KEYCODE_VOLUME_DOWN: return "VOL_DOWN";
            case KeyEvent.KEYCODE_VOLUME_MUTE: return "MUTE";
            case KeyEvent.KEYCODE_CHANNEL_UP: return "CH_UP";
            case KeyEvent.KEYCODE_CHANNEL_DOWN: return "CH_DOWN";
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE: return "PLAY_PAUSE";
            case KeyEvent.KEYCODE_MEDIA_PLAY: return "PLAY";
            case KeyEvent.KEYCODE_MEDIA_PAUSE: return "PAUSE";
            case KeyEvent.KEYCODE_MEDIA_STOP: return "STOP";
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD: return "FF";
            case KeyEvent.KEYCODE_MEDIA_REWIND: return "REW";
            case KeyEvent.KEYCODE_0: return "0";
            case KeyEvent.KEYCODE_1: return "1";
            case KeyEvent.KEYCODE_2: return "2";
            case KeyEvent.KEYCODE_3: return "3";
            case KeyEvent.KEYCODE_4: return "4";
            case KeyEvent.KEYCODE_5: return "5";
            case KeyEvent.KEYCODE_6: return "6";
            case KeyEvent.KEYCODE_7: return "7";
            case KeyEvent.KEYCODE_8: return "8";
            case KeyEvent.KEYCODE_9: return "9";
            default: return "KEY_" + keyCode;
        }
    }

    @Override
    public void startTest(TestCallback callback) {
        currentStatus = TestStatus.RUNNING;
        callback.onTestStarted();
    }

    @Override
    public void stopTest() {
        currentStatus = TestStatus.IDLE;
        isTestRunning = false;
    }

    @Override
    public void pauseTest() {
        currentStatus = TestStatus.PAUSED;
        isTestRunning = false;
    }

    @Override
    public void resumeTest() {
        currentStatus = TestStatus.RUNNING;
        isTestRunning = true;
    }

    @Override
    public TestStatus getTestStatus() {
        return currentStatus;
    }

    public interface RcuButtonTestCallback {
        void onTestStarted(String message);
        void onButtonPressed(ButtonEvent event);
        void onTestResultUpdated(RcuButtonTestResult result);
        void onError(String error);
    }

    public static class ButtonEvent {
        public String buttonName;
        public int keyCode;
        public long responseTime; // ms
        public long timestamp;
    }

    public static class RcuButtonTestResult {
        public List<ButtonEvent> buttonHistory = new ArrayList<>();
        public Map<String, Integer> buttonCountMap = new HashMap<>();
        public double averageResponseTime; // ms

        public void calculateAverageResponseTime() {
            if (buttonHistory.isEmpty()) {
                averageResponseTime = 0;
                return;
            }

            long totalResponseTime = 0;
            for (ButtonEvent event : buttonHistory) {
                totalResponseTime += event.responseTime;
            }

            averageResponseTime = (double) totalResponseTime / buttonHistory.size();
        }
    }
}
