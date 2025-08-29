package com.innopia.bist.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LogRepository {

	private static volatile LogRepository instance;
	private final List<String> logs = new ArrayList<>();

	private LogRepository() {}

	public static LogRepository getInstance() {
		if (instance == null) {
			synchronized (LogRepository.class) {
				if (instance == null) {
					instance = new LogRepository();
				}
			}
		}
		return instance;
	}

	public void addLog(String log) {
		logs.add(log);
	}

	public List<String> getLogs() {
		return Collections.unmodifiableList(logs);
	}

	public void clearLogs() {
		logs.clear();
	}

	public void saveToFile() {
	}
}
