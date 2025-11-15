package com.innopia.bist.ver1.util;

public enum TestStatus {
	// Default state, before any test is run
	PENDING,
	// The test has passed successfully
	PASSED,
	// The test has failed and cannot be retried automatically
	FAILED,
	// The test has failed but can be retried by the user or system
	RETEST,
	// The test is currently in progress
	RUNNING,
	// The test is paused, waiting for a user action (e.g., plug in a cable)
	WAITING_FOR_USER,
	// The test is currently in error
	ERROR
}
