package com.innopia.bist.model;

import java.util.Map;
import java.util.function.Consumer;

// 모든 테스트 모듈이 구현해야 할 공통 인터페이스
public interface Test {
    // 수동 테스트 실행
    void runManualTest(Map<String, Object> params, Consumer<String> callback);
    // 자동 테스트 실행 (필요 시)
//    void runAutoTest(Consumer<String> callback);
}