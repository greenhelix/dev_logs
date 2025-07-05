package com.ik.innopia.hubist.side.test.wifi;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.ik.innopia.hubist.databinding.FragmentWifiTestBinding;

import java.util.ArrayList;
import java.util.List;

public class WifiTest {
    private static final String TAG = "BIST"; // 로그 태그

    private final WifiManager mWifiManager;
    public Context mContext;
    public List<ScanResult> wifiList = new ArrayList<>();

    // 로그 메시지를 MainActivity로 전달하기 위한 인터페이스 정의
    public interface OnLogMessageListener {
        void onLogMessage(String message);
    }

    // 인터페이스 인스턴스 변수
    private com.ik.innopia.hubist.WifiTest.OnLogMessageListener mLogMessageListener;

    // 생성자
    public WifiTest(Context context) {
        mContext = context;
        mWifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        // WifiInfo wifiInfo = mWifiManager.getConnectionInfo(); // 이 줄은 현재 사용되지 않으므로 필요에 따라 제거하거나 사용하십시오.
    }

    /**
     * 로그 메시지 리스너를 설정하는 메서드
     * MainActivity에서 이 메서드를 호출하여 로그를 받을 리스너를 등록합니다.
     * @param listener 로그 메시지를 받을 리스너
     */
    public void setOnLogMessageListener(com.ik.innopia.hubist.WifiTest.OnLogMessageListener listener) {
        this.mLogMessageListener = listener;
    }

    /**
     * 로그 메시지를 리스너를 통해 전달하는 헬퍼 메서드
     * 리스너가 설정되어 있으면 메시지를 전달하고, 그렇지 않으면 내부 로그로만 출력합니다.
     * @param message 전달할 로그 메시지
     */
    private void sendLogMessage(String message) {
        // "WifiTestLog" 문구를 포함하여 MainActivity로 전달
        String fullMessage = "WifiTestLog: " + message;
        if (mLogMessageListener != null) {
            mLogMessageListener.onLogMessage(fullMessage);
        }
        // 내부 디버깅을 위해 Logcat에 출력
        Log.d(TAG, fullMessage);
    }

    /**
     * Wi-Fi 권한이 있는지 확인합니다.
     * @return 권한이 있으면 true, 없으면 false
     */
    public boolean checkWifiPermission() {
        boolean hasPermission = ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!hasPermission) {
            sendLogMessage("Wi-Fi 권한이 없습니다. ACCESS_FINE_LOCATION 필요.");
        }
        return hasPermission;
    }

    /**
     * Wi-Fi 스캔을 시작합니다.
     * 권한이 없으면 스캔을 시작하지 않습니다.
     */
    public void startWifiScan() {
        sendLogMessage("Wi-Fi 스캔 시작 요청.");
        if(checkWifiPermission()){
            wifiList = mWifiManager.getScanResults();
            sendLogMessage("Wi-Fi 스캔 결과 수: " + wifiList.size());
            checkWifiScanList(true);  // Wi-Fi 목록을 로그로 확인 (true: 켜기, false: 끄기)
        } else {
            sendLogMessage("Wi-Fi 스캔을 시작할 수 없습니다: 권한 없음.");
        }
    }

    /**
     * Wi-Fi 스캔 목록을 로그로 확인합니다.
     * @param on_off true면 로그를 출력하고, false면 출력하지 않습니다.
     */
    public void checkWifiScanList(boolean on_off){
        if(on_off) {
            sendLogMessage("Wi-Fi 목록 =======================");
            if (wifiList.isEmpty()) {
                sendLogMessage("스캔된 Wi-Fi 네트워크가 없습니다.");
            } else {
                for (int i = 0; i < wifiList.size(); i++) {
                    sendLogMessage((i + 1) + " : " + wifiList.get(i).SSID);
                }
            }
            sendLogMessage("Wi-Fi 목록 끝 =======================");
        }
    }

//    직접 설정의 wifi 설정 화면 가져오는 인텐트
//    Intent intent = new Intent(Settings.Panel.ACTION_WIFI);
//    startActivity(intent);

    public static class WifiTestFragment extends Fragment {

        private FragmentWifiTestBinding binding;

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            binding = FragmentWifiTestBinding.inflate(inflater, container, false);
            return binding.getRoot();
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            binding.wifiScanButton.setOnClickListener(v -> {
                // 여기서 사이드 패널(DialogFragment)을 띄웁니다.
                WifiScanListDialogFragment dialogFragment = new WifiScanListDialogFragment();
                dialogFragment.show(getParentFragmentManager(), "wifi_scan_list");
            });

            binding.wifiActionButton.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Wi-Fi Action Test Clicked", Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            binding = null;
        }
    }
}
