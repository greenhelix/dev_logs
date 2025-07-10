package com.innopia.bist.tests;

public class TestResult {
    public final boolean isSuccess;
    public final String message;

    public TestResult(boolean isSuccess, String message){
        this.isSuccess = isSuccess;
        this.message = message;
    }
}
