package com.innopia.bist.ver2.util;

import android.view.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * 비밀 키 조합 감지기
 * UP - DOWN - UP - DOWN - LEFT - RIGHT 순서로 입력하면 OSD 활성화
 */
public class SecretCodeDetector {

    private static final int[] SECRET_CODE = {
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT
    };

    private static final long MAX_TIME_BETWEEN_KEYS = 3000; // 3초

    private List<KeyInput> keyHistory = new ArrayList<>();
    private SecretCodeListener listener;

    public interface SecretCodeListener {
        void onSecretCodeDetected();
    }

    public SecretCodeDetector(SecretCodeListener listener) {
        this.listener = listener;
    }

    /**
     * 키 입력 처리
     */
    public void onKeyPressed(int keyCode) {
        long currentTime = System.currentTimeMillis();

        // 오래된 입력 제거
        removeOldInputs(currentTime);

        // 새 입력 추가
        keyHistory.add(new KeyInput(keyCode, currentTime));

        // 비밀 코드 확인
        if (checkSecretCode()) {
            keyHistory.clear();
            if (listener != null) {
                listener.onSecretCodeDetected();
            }
        }

        // 최대 길이 유지
        if (keyHistory.size() > SECRET_CODE.length) {
            keyHistory.remove(0);
        }
    }

    /**
     * 시간 초과된 입력 제거
     */
    private void removeOldInputs(long currentTime) {
        keyHistory.removeIf(input ->
                currentTime - input.timestamp > MAX_TIME_BETWEEN_KEYS);
    }

    /**
     * 비밀 코드 확인
     */
    private boolean checkSecretCode() {
        if (keyHistory.size() != SECRET_CODE.length) {
            return false;
        }

        for (int i = 0; i < SECRET_CODE.length; i++) {
            if (keyHistory.get(i).keyCode != SECRET_CODE[i]) {
                return false;
            }
        }

        return true;
    }

    /**
     * 히스토리 초기화
     */
    public void reset() {
        keyHistory.clear();
    }

    /**
     * 키 입력 정보
     */
    private static class KeyInput {
        int keyCode;
        long timestamp;

        KeyInput(int keyCode, long timestamp) {
            this.keyCode = keyCode;
            this.timestamp = timestamp;
        }
    }
}
