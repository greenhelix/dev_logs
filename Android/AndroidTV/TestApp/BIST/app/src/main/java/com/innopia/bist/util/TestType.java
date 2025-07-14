package com.innopia.bist.util;

/**
 * 애플리케이션에서 수행되는 테스트의 종류를 정의하는 열거형 클래스입니다.
 * MainViewModel에서 각 테스트의 상태를 관리하는 데 사용됩니다.
 */
public enum TestType {
    WIFI,
    BLUETOOTH,
    ETHERNET
    // 새로운 테스트 추가 시 여기에 항목을 추가
}
