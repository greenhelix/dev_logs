package com.innopia.bist.test;

import java.util.Map;
import java.util.function.Consumer;

// 영상 Test 추상화 및 확장 지점
public class VideoTest implements Test {

    @Override
    public void runManualTest(Map params, Consumer callback) {
        // 이 예시에서는 실제 영상 재생은 Fragment/VideoView에서 처리하며,
        // 이 메서드는 signed(키확인), 특수테스트 등 추후 확장시 구현
        // (예: DRM 체크, 영상 integrity 확인 등)
        callback.accept("Manual video test is not implemented in this version.");
    }

    // public void checkDRMVideo(...) { ... } 등 추후 확장
}
