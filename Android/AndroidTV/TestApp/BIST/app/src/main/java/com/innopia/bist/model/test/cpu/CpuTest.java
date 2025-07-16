package com.innopia.bist.model.test.cpu;

import com.innopia.bist.model.Test;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class CpuTest implements Test {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void runManualTest(Map<String, Object> params, Consumer<String> callback) {
        executor.execute(() -> {
            String temp = checkCpuTemperature();
            String speed = checkCpuSpeed();
            String result = "== CPU Test Result ==\n" + temp + "\n" + speed;
            callback.accept(result);
        });
    }

    @Override
    public void runAutoTest(Map<String, Object> params, Consumer<String> callback) {
        // 자동 테스트: 백그라운드에서 주기적으로 성능을 체크하거나, 특정 부하 테스트 후 결과를 반환
        executor.execute(() -> {
            // 예시: 간단한 연산을 10초간 수행하여 CPU 부하를 유발
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < 10000) {
                // 부하 생성 작업
            }
            String result = "CPU Stress Test Completed. \n" + checkCpuSpeed();
            callback.accept(result);
        });
    }

    // CPU 온도 확인
    private String checkCpuTemperature() {
        try {
            // thermal_zone 번호는 디바이스마다 다를 수 있음 (0~9 시도)
            Process process = Runtime.getRuntime().exec("cat /sys/class/thermal/thermal_zone0/temp");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String temp = reader.readLine();
            if (temp != null) {
                float tempValue = Float.parseFloat(temp) / 1000.0f; // 1000을 나눠 섭씨로 변환
                return String.format("CPU Temperature: %.2f °C", tempValue);
            }
        } catch (Exception e) {
            return "CPU Temperature: Not Available";
        }
        return "CPU Temperature: Not Available";
    }

    // CPU 속도(클럭) 확인
    private String checkCpuSpeed() {
        try {
            // cpu0의 현재 클럭 확인
            Process process = Runtime.getRuntime().exec("cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String speed = reader.readLine();
            if (speed != null) {
                float speedValue = Float.parseFloat(speed) / 1000.0f; // 1000을 나눠 MHz로 변환
                return String.format("CPU Speed: %.0f MHz", speedValue);
            }
        } catch (Exception e) {
            return "CPU Speed: Not Available or ";
        }
        return "CPU Speed: Not Available";
    }
}
