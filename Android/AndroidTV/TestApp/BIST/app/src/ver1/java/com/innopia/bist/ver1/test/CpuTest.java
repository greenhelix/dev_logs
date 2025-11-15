package com.innopia.bist.ver1.test;

import android.content.Context;
import android.util.Log;

import com.innopia.bist.ver1.util.TestResult;
import com.innopia.bist.ver1.util.TestStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class CpuTest implements Test {

	private static final String TAG = "CpuTest";

	private final String CMD_DUMP_CPUINFO = "cat /proc/stat";

	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	@Override
	public void runManualTest(Map<String, Object> params, Consumer<TestResult> callback) {
		cpuTest(params, callback);
	}

	@Override
	public void runAutoTest(Map<String, Object> params, Consumer<TestResult> callback) {
		cpuTest(params, callback);
	}

	private void cpuTest(Map<String, Object> params, Consumer<TestResult> callback) {
		executor.execute(() -> {
			Context context = (Context) params.get("context");
			if (context == null) {
				callback.accept(new TestResult(TestStatus.ERROR, "Error: Context is null"));
				return;
			}

			int cpuSpeed = getCpuSpeed();
			int cpuUsage = getCpuUsage();
			if (cpuSpeed > 0 && cpuUsage > 0) {
				String result = "CPU Speed: " + cpuSpeed + " MHz\n" +
								"CPU Usage: " + cpuUsage + " %";
				callback.accept(new TestResult(TestStatus.PASSED, result));
			} else {
				String result = "Cannot get CPU info";
				callback.accept(new TestResult(TestStatus.FAILED, result));
			}
		});
	}

	private int getCpuSpeed() {
		int cpuSpeed = 0;
		try {
			Process process = Runtime.getRuntime().exec("cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq");
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String speed = reader.readLine();
			if (speed != null) {
				float speedValue = Float.parseFloat(speed) / 1000.0f;
				cpuSpeed = (int) speedValue;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return cpuSpeed;
	}

	private String[] execCommand(String cmd) {
		Runtime runtime = Runtime.getRuntime();

		List<String> readData = new ArrayList<String>();

		try {
			Process process = runtime.exec(cmd);
			process.waitFor();
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = "";
			while ((line = reader.readLine()) != null) {
				readData.add(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		String[] result = new String[readData.size()];
		readData.toArray(result);

		return result;
	}

	private int getCpuUsage() {
		int cpuUsage = 0;
		try {
			String[] cmdRetVal = execCommand(CMD_DUMP_CPUINFO);
			String cpuInfo = cmdRetVal[0];
			cpuInfo = cpuInfo.replaceAll("\\s+", " ");
			String[] infos1 = cpuInfo.split(" ");

			TimeUnit.SECONDS.sleep(1);

			cmdRetVal = execCommand(CMD_DUMP_CPUINFO);
			cpuInfo = cmdRetVal[0];
			cpuInfo = cpuInfo.replaceAll("\\s+", " ");
			String[] infos2 = cpuInfo.split(" ");


			int delta_total = 0;
			for (int i = 1; i < infos2.length; i++) {
				delta_total += Integer.parseInt(infos2[i]) - Integer.parseInt(infos1[i]);
			}
			cpuUsage = (delta_total - (Integer.parseInt(infos2[4]) - Integer.parseInt(infos1[4]))) * 100 / delta_total ;
			Log.d(TAG, "cpuUsage = " + cpuUsage);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return cpuUsage;
	}

}
