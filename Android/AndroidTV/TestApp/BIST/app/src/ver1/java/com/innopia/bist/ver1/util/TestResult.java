package com.innopia.bist.ver1.util;

public class TestResult {
	private final TestStatus status;
	private final String message;

	public TestResult(TestStatus status, String message) {
		this.status = status;
		this.message = message;
	}

	public TestStatus getStatus() { return status; }
	public String getMessage() { return message; }
}
