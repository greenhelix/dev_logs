package com.innopia.bist.util;

import android.app.ActivityManager;
import android.content.Context;
import java.util.List;

public class ServiceUtils {
    /**
     * 지정된 클래스 이름을 가진 서비스가 실행 중인지 확인합니다.
     * @param context Context 객체
     * @param serviceClassName 확인할 서비스의 전체 클래스 이름 (예: "com.innopia.bistservice.BISTService")
     * @return 서비스가 실행 중이면 true, 아니면 false
     */
    public static boolean isServiceRunning(Context context, String serviceClassName) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) {
            return false;
        }
        List<ActivityManager.RunningServiceInfo> runningServices = manager.getRunningServices(Integer.MAX_VALUE);
        if (runningServices == null) {
            return false;
        }
        for (ActivityManager.RunningServiceInfo service : runningServices) {
            if (serviceClassName.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
