package com.innopia.bist.util;

import android.os.Bundle;

public class TestConfig {
	private final TestType type;
	private final Bundle config;

	public TestConfig(TestType type, Bundle config) {
		this.type = type;
		this.config = config;
	}

	public TestConfig(TestType type) {
		this.type = type;
		this.config = null;
	}

	public TestType getType() {
		return type;
	}

	public Bundle getConfig() {
		return config;
	}
}
