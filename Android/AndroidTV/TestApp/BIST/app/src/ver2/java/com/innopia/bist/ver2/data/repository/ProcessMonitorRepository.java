package com.innopia.bist.ver2.data.repository;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.innopia.bist.ver2.util.TestStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 프로세스 모니터링 Repository
 */
public class ProcessMonitorRepository implements Test {

    private static final String TAG = "ProcessMonitorRepository";
    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TestStatus currentStatus = TestStatus.IDLE;
    private boolean isMonitoring = false;

    // 샘플 데이터 사용 여부
    private boolean useSampleData = true;

    public ProcessMonitorRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public void setUseSampleData(boolean useSampleData) {
        this.useSampleData = useSampleData;
    }

    /**
     * 프로세스 모니터링 시작
     */
    public void startMonitoring(ProcessMonitorCallback callback) {
        currentStatus = TestStatus.RUNNING;
        isMonitoring = true;

        new Thread(() -> {
            try {
                handler.post(() -> callback.onMonitoringStarted());

                while (isMonitoring) {
                    ProcessMonitorResult result;

                    if (useSampleData) {
                        result = getSampleProcessData();
                    } else {
                        result = getRealProcessData();
                    }

                    handler.post(() -> callback.onProcessDataUpdated(result));

                    Thread.sleep(1000); // 1초마다 업데이트
                }

                currentStatus = TestStatus.IDLE;
                handler.post(() -> callback.onMonitoringStopped());

            } catch (Exception e) {
                Log.e(TAG, "Process monitoring error", e);
                currentStatus = TestStatus.ERROR;
                handler.post(() -> callback.onError("Monitoring failed: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * 실제 프로세스 데이터 가져오기
     */
    private ProcessMonitorResult getRealProcessData() {
        ProcessMonitorResult result = new ProcessMonitorResult();

        try {
            ActivityManager activityManager = (ActivityManager)
                    context.getSystemService(Context.ACTIVITY_SERVICE);

            if (activityManager != null) {
                // 실행 중인 프로세스 정보 (Android 5.0 이상에서 제한됨)
                List<ActivityManager.RunningAppProcessInfo> processes =
                        activityManager.getRunningAppProcesses();

                if (processes != null && !processes.isEmpty()) {
                    List<ProcessInfo> processInfoList = new ArrayList<>();

                    for (ActivityManager.RunningAppProcessInfo processInfo : processes) {
                        ProcessInfo info = new ProcessInfo();
                        info.processName = processInfo.processName;
                        info.pid = processInfo.pid;

                        // 메모리 사용량 가져오기
                        Debug.MemoryInfo[] memoryInfo = activityManager.getProcessMemoryInfo(
                                new int[]{processInfo.pid});

                        if (memoryInfo != null && memoryInfo.length > 0) {
                            info.memoryUsage = memoryInfo[0].getTotalPss() / 1024; // MB
                        }

                        processInfoList.add(info);
                    }

                    // 메모리 사용량 기준으로 정렬
                    Collections.sort(processInfoList, new Comparator<ProcessInfo>() {
                        @Override
                        public int compare(ProcessInfo o1, ProcessInfo o2) {
                            return Long.compare(o2.memoryUsage, o1.memoryUsage);
                        }
                    });

                    // 상위 5개만 선택
                    result.topProcesses = processInfoList.subList(0,
                            Math.min(5, processInfoList.size()));
                }

                // 전체 메모리 정보
                ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
                activityManager.getMemoryInfo(memInfo);

                result.totalMemory = memInfo.totalMem / (1024 * 1024);
                result.availableMemory = memInfo.availMem / (1024 * 1024);
                result.usedMemory = result.totalMemory - result.availableMemory;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error getting real process data", e);
        }

        return result;
    }

    /**
     * 샘플 프로세스 데이터
     */
    private ProcessMonitorResult getSampleProcessData() {
        ProcessMonitorResult result = new ProcessMonitorResult();

        result.topProcesses = new ArrayList<>();

        ProcessInfo p1 = new ProcessInfo();
        p1.processName = "com.android.systemui";
        p1.pid = 1234;
        p1.memoryUsage = 256;
        result.topProcesses.add(p1);

        ProcessInfo p2 = new ProcessInfo();
        p2.processName = "com.google.android.gms";
        p2.pid = 2345;
        p2.memoryUsage = 189;
        result.topProcesses.add(p2);

        ProcessInfo p3 = new ProcessInfo();
        p3.processName = "com.android.launcher";
        p3.pid = 3456;
        p3.memoryUsage = 145;
        result.topProcesses.add(p3);

        ProcessInfo p4 = new ProcessInfo();
        p4.processName = "com.android.phone";
        p4.pid = 4567;
        p4.memoryUsage = 98;
        result.topProcesses.add(p4);

        ProcessInfo p5 = new ProcessInfo();
        p5.processName = "com.android.settings";
        p5.pid = 5678;
        p5.memoryUsage = 67;
        result.topProcesses.add(p5);

        result.totalMemory = 4096; // 4GB
        result.usedMemory = 2548;
        result.availableMemory = 1548;

        return result;
    }

    @Override
    public void startTest(TestCallback callback) {
        currentStatus = TestStatus.RUNNING;
        callback.onTestStarted();
    }

    @Override
    public void stopTest() {
        currentStatus = TestStatus.IDLE;
        isMonitoring = false;
    }

    @Override
    public void pauseTest() {
        currentStatus = TestStatus.PAUSED;
        isMonitoring = false;
    }

    @Override
    public void resumeTest() {
        currentStatus = TestStatus.RUNNING;
        isMonitoring = true;
    }

    @Override
    public TestStatus getTestStatus() {
        return currentStatus;
    }

    public interface ProcessMonitorCallback {
        void onMonitoringStarted();
        void onProcessDataUpdated(ProcessMonitorResult result);
        void onMonitoringStopped();
        void onError(String error);
    }

    public static class ProcessMonitorResult {
        public List<ProcessInfo> topProcesses = new ArrayList<>();
        public long totalMemory; // MB
        public long usedMemory; // MB
        public long availableMemory; // MB
    }

    public static class ProcessInfo {
        public String processName;
        public int pid;
        public long memoryUsage; // MB
    }
}
