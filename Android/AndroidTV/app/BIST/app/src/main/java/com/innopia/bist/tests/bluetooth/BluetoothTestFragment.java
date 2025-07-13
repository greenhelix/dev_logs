package com.innopia.bist.tests.bluetooth;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Fragment;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.innopia.bist.MainActivity;
import com.innopia.bist.tests.wifi.WifiTest;
import com.innopia.bist.util.FocusNavigationHandler;
import com.innopia.bist.util.ILogger;
import com.innopia.bist.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

public class BluetoothTestFragment extends Fragment implements FocusNavigationHandler {

    private static final String TAG = "BIST_BT_FRAGMENT";
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 101;
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothTest bluetoothTest;
    private ILogger mLogger;
    private TextView tvBluetoothInfo;
    private Button btnBluetoothScan;
    private Button btnBluetoothTest;

    private BluetoothAdapter bluetoothAdapter;
    // 사용자가 선택한, 테스트 대상이 되는 기기 (상태 유지를 위해 멤버 변수로 관리)
    private BluetoothDevice mSelectedDevice;
    // 현재 시스템에 연결된 기기 목록을 임시 저장하는 캐시
    private final List<BluetoothDevice> connectedDevicesCache = new ArrayList<>();

    public static BluetoothTestFragment newInstance() {
        return new BluetoothTestFragment();
    }

    @Override
    public int getTargetFocusId(int direction) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (getActivity() instanceof MainActivity && ((MainActivity) getActivity()).isFocusFeatureEnabled()) {
                if (direction == KeyEvent.KEYCODE_DPAD_UP) {
                    return R.id.text_bluetooth_info;
                } else if (direction == KeyEvent.KEYCODE_DPAD_DOWN) {
                    return R.id.btn_bluetooth_scan;
                }
            }
        }
        return 0;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                this.mLogger = activity.logUtil;
                this.bluetoothTest = activity.getBluetoothTest();
            }else{
                mLogger.log("*** TIRAMISU not supported");
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_bluetooth_test, container, false);
        mLogger.log("onCreateView called.");

        tvBluetoothInfo = rootView.findViewById(R.id.text_bluetooth_info);
        btnBluetoothScan = rootView.findViewById(R.id.btn_bluetooth_scan);
        btnBluetoothTest = rootView.findViewById(R.id.btn_bluetooth_test);

        if (bluetoothAdapter == null) {
            tvBluetoothInfo.setText("Bluetooth is not supported on this device.");
            btnBluetoothScan.setEnabled(false);
            btnBluetoothTest.setEnabled(false);
            return rootView;
        }

        btnBluetoothScan.setOnClickListener(v -> {
            mLogger.log("Scan button clicked.");
            if (!checkAndRequestPermissions()) {
                return;
            }
            // 개선된 스캔 로직 호출
            showConnectedDevicesOrScan();
        });

        btnBluetoothTest.setOnClickListener(v -> {
            if (mSelectedDevice != null) {
                mLogger.log("Test button clicked for device: " + mSelectedDevice.getName());
                runAllBluetoothTests(mSelectedDevice);
            } else {
                Toast.makeText(getActivity(), "Please select a device to test.", Toast.LENGTH_SHORT).show();
                mLogger.log("Test button clicked but no device selected.");
            }
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mLogger.log("Fragment resumed. Updating connection status.");
        if (checkAndRequestPermissions()) {
            // 상태 유지 로직이 포함된 연결 상태 업데이트
            updateConnectionStatus();
        }
    }

    /**
     * 요청 1: 연결된 기기가 있으면 다이얼로그를 먼저 보여주고, 없으면 스캔 화면으로 이동
     */
    private void showConnectedDevicesOrScan() {
        // 항상 최신 연결 목록을 가져오기 위해 캐시를 업데이트한 후 다이얼로그를 보여줌
        updateConnectedDevicesCache(() -> {
            if (connectedDevicesCache.isEmpty()) {
                mLogger.log("No connected devices found. Opening Add Accessory screen.");
                openAddAccessoryScreen();
            } else {
                mLogger.log("Connected devices found. Showing selection dialog.");
                showDeviceSelectionDialog();
            }
        });
    }

    /**
     * 현재 연결된 장치 목록을 보여주는 다이얼로그
     */
    private void showDeviceSelectionDialog() {
        if (getActivity() == null) return;

        List<String> deviceNames = new ArrayList<>();
        for (BluetoothDevice device : connectedDevicesCache) {
            if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return;
            deviceNames.add(device.getName());
        }
        deviceNames.add("다른 기기 검색 (Scan for other devices)...");

        new AlertDialog.Builder(getActivity())
                .setTitle("테스트할 기기 선택")
                .setItems(deviceNames.toArray(new String[0]), (dialog, which) -> {
                    if (which == deviceNames.size() - 1) {
                        // "다른 기기 검색" 선택
                        openAddAccessoryScreen();
                    } else {
                        // 기존 연결된 기기 선택
                        mSelectedDevice = connectedDevicesCache.get(which);
                        mLogger.log("Device selected from dialog: " + mSelectedDevice.getName());
                        updateUiWithDeviceInfo(mSelectedDevice);
                    }
                })
                .show();
    }

    private void openAddAccessoryScreen() {
        mLogger.log("Opening Add Accessory screen.");
        Toast.makeText(getActivity(), "액세서리 추가 화면을 엽니다...", Toast.LENGTH_SHORT).show();
        try {
            Intent intent = new Intent();
            intent.setClassName("com.android.tv.settings", "com.android.tv.settings.accessories.AddAccessoryActivity");
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            mLogger.log("Failed to open AddAccessoryActivity. Falling back to main settings.");
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    /**
     * 요청 2: 여러 테스트를 순차적으로 실행하는 메인 테스트 함수
     */
    private void runAllBluetoothTests(BluetoothDevice device) {
        // 테스트 시작 전 UI 초기화
        updateUiWithDeviceInfo(device); // 테스트 결과를 덮어쓰기 위해 기본 정보로 리셋
        tvBluetoothInfo.append("\n\n--- Running Tests ---");
        btnBluetoothTest.setEnabled(false);

        new Thread(() -> {
            // 테스트 1: SPP 연결 테스트
            final boolean sppResult = testSppConnection(device);
            mLogger.log("Device : "+ device.getName());
            updateTestResultOnUI("\nSPP Connection Test: " + (sppResult ? "SUCCESS" : "FAILED"));

            // 테스트 2: RSSI (신호 강도) 확인
            // 실제 Classic BT RSSI는 검색 시에만 얻을 수 있어, 여기서는 테스트 구조만 보여줌
            updateTestResultOnUI("\nRSSI Check: Unavailable");

            // 테스트 3: 오디오 장치인 경우 사운드 테스트
            if (isAudioDevice(device)) {
                final String soundTestResult = testSoundPlayback(device);
                updateTestResultOnUI("\nSound Playback Test: " + soundTestResult);
            }

            // 모든 테스트 완료 후 버튼 다시 활성화
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> btnBluetoothTest.setEnabled(true));
            }
        }).start();
    }

    private boolean testSppConnection(BluetoothDevice device) {
        try (BluetoothSocket socket = device.createRfcommSocketToServiceRecord(SPP_UUID)) {
            if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
            socket.connect();
            mLogger.log("SPP socket connection successful.");
            return true;
        } catch (IOException e) {
            mLogger.log("SPP socket connection failed: " + e.getMessage());
            return false;
        }
    }

    private String testSoundPlayback(BluetoothDevice device) {
        // 실제 사운드 재생은 AudioManager를 통해 오디오 포커스를 요청하고 스트림을 라우팅해야 함
        // 여기서는 테스트가 가능한 구조만 보여주기 위해 시뮬레이션된 결과를 반환
        mLogger.log("Simulating sound playback test.");
        return "Passed (Simulated)";
    }

    /**
     * 요청 3: 프래그먼트 상태 유지를 포함한 연결 상태 업데이트
     */
    private void updateConnectionStatus() {
        if (!isAdded()) return; // 프래그먼트가 액티비티에 붙어있지 않으면 중단

        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            tvBluetoothInfo.setText("Permission needed to check status.");
            updateBluetoothIcon(false);
            return;
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            tvBluetoothInfo.setText("Bluetooth is OFF.");
            btnBluetoothTest.setEnabled(false);
            mSelectedDevice = null;
            updateBluetoothIcon(false);
            return;
        }

        // 연결된 기기 목록을 먼저 업데이트
        updateConnectedDevicesCache(() -> {
            // 이전에 선택한 기기가 있고, 그 기기가 여전히 연결 목록에 있는지 확인
            if (mSelectedDevice != null && connectedDevicesCache.contains(mSelectedDevice)) {
                mLogger.log("Previously selected device is still connected. Restoring state.");
                updateUiWithDeviceInfo(mSelectedDevice);
            } else {
                // 이전에 선택한 기기가 없거나, 연결이 끊겼다면 캐시의 첫번째 기기를 선택
                if (!connectedDevicesCache.isEmpty()) {
                    mLogger.log("No previous device state. Selecting the first connected device.");
                    mSelectedDevice = connectedDevicesCache.get(0);
                    updateUiWithDeviceInfo(mSelectedDevice);
                } else {
                    // 연결된 기기가 아무것도 없음
                    mLogger.log("No connected devices found.");
                    mSelectedDevice = null;
                    updateUiWithDeviceInfo(null);
                }
            }
        });
    }

    private void updateConnectedDevicesCache(Runnable onCompleted) {
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            if (onCompleted != null) onCompleted.run();
            return;
        }

        connectedDevicesCache.clear();
        final CountDownLatch latch = new CountDownLatch(2); // A2DP, HEADSET 2개 프로필 확인

        BluetoothProfile.ServiceListener listener = new BluetoothProfile.ServiceListener() {
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                List<BluetoothDevice> devices = proxy.getConnectedDevices();
                for (BluetoothDevice device : devices) {
                    if (!connectedDevicesCache.contains(device)) {
                        connectedDevicesCache.add(device);
                    }
                }
                bluetoothAdapter.closeProfileProxy(profile, proxy);
                latch.countDown();
            }
            public void onServiceDisconnected(int profile) {
                latch.countDown();
            }
        };

        bluetoothAdapter.getProfileProxy(getActivity(), listener, BluetoothProfile.A2DP);
        bluetoothAdapter.getProfileProxy(getActivity(), listener, BluetoothProfile.HEADSET);

        // 백그라운드에서 대기 후 완료 콜백 실행
        new Thread(() -> {
            try {
                latch.await(); // 두 프로필 조회가 끝날 때까지 대기
                if (getActivity() != null) {
                    getActivity().runOnUiThread(onCompleted);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }


    // --- Helper & UI Methods ---

    private void updateUiWithDeviceInfo(@Nullable BluetoothDevice device) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            if (device != null) {
                if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return;
                StringBuilder info = new StringBuilder();
                info.append("Connected Device:\n");
                info.append("Name: ").append(device.getName()).append("\n");
                info.append("Address: ").append(device.getAddress()).append("\n");
                info.append("Bond State: ").append(getBondStateString(device.getBondState())).append("\n");
                info.append("Device Class: ").append(getDeviceClassString(device.getBluetoothClass()));

                tvBluetoothInfo.setText(info.toString());
                btnBluetoothTest.setEnabled(true);
                updateBluetoothIcon(true);
            } else {
                tvBluetoothInfo.setText("No Bluetooth device is currently connected.\nPlease use 'Scan' to connect a device.");
                btnBluetoothTest.setEnabled(false);
                updateBluetoothIcon(false);
            }
        });
    }

    private void updateTestResultOnUI(String result) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> tvBluetoothInfo.append(result));
        }
    }

    private void updateBluetoothIcon(boolean isConnected) {
        if (getActivity() instanceof MainActivity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ((MainActivity) getActivity()).updateStatusIcon(((MainActivity) getActivity()).ivBtStatus, isConnected, R.drawable.ic_bt_on, R.drawable.ic_bt_off);
            }
        }
    }

    private String getBondStateString(int bondState) {
        switch (bondState) {
            case BluetoothDevice.BOND_BONDED: return "Paired";
            case BluetoothDevice.BOND_BONDING: return "Pairing...";
            case BluetoothDevice.BOND_NONE: return "Not Paired";
            default: return "Unknown";
        }
    }

    private boolean isAudioDevice(BluetoothDevice device) {
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return false;
        BluetoothClass btClass = device.getBluetoothClass();
        if (btClass == null) return false;
        switch (btClass.getDeviceClass()) {
            case BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES:
            case BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE:
            case BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET:
            case BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER:
                return true;
            default:
                return false;
        }
    }

    private String getDeviceClassString(BluetoothClass btClass) {
        if (btClass == null) return "Unknown";
        if (isAudioDevice(mSelectedDevice)) return "Audio (Headset/Speaker)";

        switch (btClass.getDeviceClass()) {
            case BluetoothClass.Device.COMPUTER_LAPTOP: return "Computer";
            case BluetoothClass.Device.PHONE_SMART: return "Phone";
            case BluetoothClass.Device.PERIPHERAL_KEYBOARD:
            case BluetoothClass.Device.PERIPHERAL_POINTING: return "Input Device (Keyboard/Mouse)";
            default: return "Other (" + Integer.toHexString(btClass.getDeviceClass()) + ")";
        }
    }

    private boolean checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_PERMISSIONS);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mLogger.log("BLUETOOTH_CONNECT permission granted.");
            updateConnectionStatus();
        } else {
            mLogger.log("Permission denied by user.");
            Toast.makeText(getActivity(), "Bluetooth permission is required.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (btnBluetoothScan != null) {
            btnBluetoothScan.requestFocus();
        }
    }
}
