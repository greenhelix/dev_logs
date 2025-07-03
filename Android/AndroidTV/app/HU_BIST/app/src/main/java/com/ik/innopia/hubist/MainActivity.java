package com.ik.innopia.hubist; // Your package name

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends Activity implements WifiTest.OnLogMessageListener {
    private static final String TAG = "BIST"; // 로그 태그

    private static final int REQUEST_CODE_WIFI_PERMISSION = 1001; // 권한 요청 코드

    private TextView tvWifiLog;
    private ScrollView scrollWifi;
    private WifiTest wifiTest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI 요소 초기화
        tvWifiLog = findViewById(R.id.tv_wifi);
        scrollWifi = findViewById(R.id.scroll_wifi);
        Button btnWifiTest = findViewById(R.id.button1);
        Button btnBluetoothTest = findViewById(R.id.button2);

        // WifiTest 클래스 인스턴스 생성 및 리스너 등록
        wifiTest = new WifiTest(this);
        wifiTest.setOnLogMessageListener(this); // MainActivity가 WifiTest의 로그를 수신하도록 설정

        btnWifiTest.setOnClickListener(v -> {
            appendLog("WiFi TEST 버튼 클릭됨. 스캔 시작...");
            checkAndRequestWifiPermissions();
        });

        // 초기 로그 메시지 설정 (선택 사항)
        tvWifiLog.setText("Wi-Fi 테스트 로그가 여기에 표시됩니다...\n");
    }

    // WifiTest.OnLogMessageListener 인터페이스 구현
    @Override
    public void onLogMessage(final String message) {
        // UI 스레드에서 TextView를 업데이트합니다.
        runOnUiThread(() -> {
            appendLog(message);
        });
    }

    /**
     * 로그 메시지를 TextView에 추가하고 ScrollView를 맨 아래로 스크롤합니다.
     * @param message TextView에 추가할 메시지
     */
    private void appendLog(String message) {
        tvWifiLog.append(message + "\n");
        // ScrollView를 항상 맨 아래로 스크롤하여 최신 로그가 보이도록 합니다.
        scrollWifi.post(() -> scrollWifi.fullScroll(ScrollView.FOCUS_DOWN));
    }

    /**
     * Wi-Fi 스캔에 필요한 권한을 확인하고, 없으면 요청합니다.
     */
    private void checkAndRequestWifiPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // 권한이 이미 있다면 Wi-Fi 스캔 시작
            wifiTest.startWifiScan();
        } else {
            // 권한이 없다면 사용자에게 요청
            appendLog("Wi-Fi 스캔 권한 (ACCESS_FINE_LOCATION)이 필요합니다. 요청 중...");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_WIFI_PERMISSION);
        }
    }

    /**
     * 권한 요청 결과를 처리합니다.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_WIFI_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한이 부여되면 Wi-Fi 스캔 시작
                appendLog("Wi-Fi 스캔 권한이 부여되었습니다. 스캔을 시작합니다.");
                wifiTest.startWifiScan();
            } else {
                // 권한이 거부되면 사용자에게 알림
                appendLog("Wi-Fi 스캔 권한이 거부되었습니다. 스캔을 할 수 없습니다.");
            }
        }
    }
}
