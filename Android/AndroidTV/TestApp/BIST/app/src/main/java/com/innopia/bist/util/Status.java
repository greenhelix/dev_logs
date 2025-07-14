package com.innopia.bist.util;

/**
 * 각 테스트의 현재 상태를 나타내는 열거형 클래스입니다.
 * UI의 상태 아이콘(ON/OFF)을 업데이트하는 데 사용됩니다.
 */
public enum Status {
    ON,     // 테스트 성공 또는 연결됨
    OFF,    // 테스트 실패 또는 연결되지 않음
    PENDING // 테스트 진행 중 또는 확인 중 (필요 시 사용)
}
