package com.innopia.bist.view;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.innopia.bist.R;
import com.innopia.bist.viewmodel.BluetoothTestViewModel;
import com.innopia.bist.viewmodel.MainViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class BluetoothTestFragment extends Fragment {
    private BluetoothTestViewModel bluetoothTestViewModel;
    private MainViewModel mainViewModel;
    private TextView tvBluetoothInfo;
    private BluetoothManager bluetoothManager;
    private BluetoothDevice mSelectedDevice;
    private final List<BluetoothDevice> connectedDevicesCache = new ArrayList<>();
    private BluetoothAdapter bluetoothAdapter;

    public static BluetoothTestFragment newInstance() {
        return new BluetoothTestFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        BluetoothTestViewModelFactory factory = new BluetoothTestViewModelFactory(requireActivity().getApplication(), mainViewModel);
        bluetoothTestViewModel = new ViewModelProvider(this, factory).get(BluetoothTestViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_bluetooth_test, container, false);

        tvBluetoothInfo = rootView.findViewById(R.id.text_bluetooth_info);
        Button btnBluetoothScan = rootView.findViewById(R.id.btn_bluetooth_scan);
        Button btnBluetoothTest = rootView.findViewById(R.id.btn_bluetooth_test);

        btnBluetoothScan.setOnClickListener(v -> {
            mainViewModel.appendLog("BluetoothTestFragment", "Scan button clicked. This part is not Worked");
            showConnectedDevicesOrScan();
        });

        btnBluetoothTest.setOnClickListener(v -> {
            tvBluetoothInfo.setText("Running Bluetooth Test...");
            bluetoothTestViewModel.startManualTest();
        });

        // ViewModel의 LiveData를 관찰하여 UI 업데이트
        bluetoothTestViewModel.testResultLiveData.observe(getViewLifecycleOwner(), result -> {
            tvBluetoothInfo.setText(result);
            boolean isConnected = result != null && !result.contains("not connected");
        });

        return rootView;
    }

    public static class BluetoothTestViewModelFactory implements ViewModelProvider.Factory {
        private final Application application;
        private final MainViewModel mainViewModel;

        public BluetoothTestViewModelFactory(@NonNull Application application, @NonNull MainViewModel mainViewModel){
            this.application = application;
            this.mainViewModel = mainViewModel;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if(modelClass.isAssignableFrom(BluetoothTestViewModel.class)) {
                return (T) new BluetoothTestViewModel(application, mainViewModel);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }

    private void showConnectedDevicesOrScan() {
        // 항상 최신 연결 목록을 가져오기 위해 캐시를 업데이트한 후 다이얼로그를 보여줌
        updateConnectedDevicesCache(() -> {
            if (connectedDevicesCache.isEmpty()) {
                mainViewModel.appendLog(getTag(),"No connected devices found. Opening Add Accessory screen.");
                openAddAccessoryScreen();
            } else {
                mainViewModel.appendLog(getTag(),"Connected devices found. Showing selection dialog.");
                showDeviceSelectionDialog();
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

    private void updateConnectionStatus() {
        if (!isAdded()) return; // 프래그먼트가 액티비티에 붙어있지 않으면 중단

        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            tvBluetoothInfo.setText("Permission needed to check status.");
            return;
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            tvBluetoothInfo.setText("Bluetooth is OFF.");
            mSelectedDevice = null;
            return;
        }

        // 연결된 기기 목록을 먼저 업데이트
        updateConnectedDevicesCache(() -> {
            // 이전에 선택한 기기가 있고, 그 기기가 여전히 연결 목록에 있는지 확인
            if (mSelectedDevice != null && connectedDevicesCache.contains(mSelectedDevice)) {
//                mLogger.log("Previously selected device is still connected. Restoring state.");
//                updateUiWithDeviceInfo(mSelectedDevice);
            } else {
                // 이전에 선택한 기기가 없거나, 연결이 끊겼다면 캐시의 첫번째 기기를 선택
                if (!connectedDevicesCache.isEmpty()) {
//                    mLogger.log("No previous device state. Selecting the first connected device.");
                    mSelectedDevice = connectedDevicesCache.get(0);
//                    updateUiWithDeviceInfo(mSelectedDevice);
                } else {
                    // 연결된 기기가 아무것도 없음
//                    mLogger.log("No connected devices found.");
                    mSelectedDevice = null;
//                    updateUiWithDeviceInfo(null);
                }
            }
        });
    }

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
//                        mLogger.log("Device selected from dialog: " + mSelectedDevice.getName());
//                        updateUiWithDeviceInfo(mSelectedDevice);
                    }
                })
                .show();
    }

    private void openAddAccessoryScreen() {
        mainViewModel.appendLog(getTag(), "Opening Add Accessory screen.");
        Toast.makeText(getActivity(), "액세서리 추가 화면을 엽니다...", Toast.LENGTH_SHORT).show();
        try {
            Intent intent = new Intent();
            intent.setClassName("com.android.tv.settings", "com.android.tv.settings.accessories.AddAccessoryActivity");
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            mainViewModel.appendLog(getTag(),"Failed to open AddAccessoryActivity. Falling back to main settings.");
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }
}
