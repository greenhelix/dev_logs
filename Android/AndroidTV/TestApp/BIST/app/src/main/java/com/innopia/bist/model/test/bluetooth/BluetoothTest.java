package com.innopia.bist.model.test.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;

import com.innopia.bist.model.Test;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class BluetoothTest implements Test {
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private boolean testSppConnection(Context context, BluetoothDevice device) {
        // 권한 확인은 ViewModel 또는 View에서 처리되었음을 가정
        try (BluetoothSocket socket = device.createRfcommSocketToServiceRecord(SPP_UUID)) {
            socket.connect();
            return true;
        } catch (IOException | SecurityException e) {
            // SecurityException은 BLUETOOTH_CONNECT 권한이 없을 때 발생 가능
            return false;
        }
    }

    @Override
    public void runManualTest(Map<String, Object> params, Consumer<String> callback) {
        // ViewModel에서 전달한 파라미터 추출
        Context context = (Context) params.get("context");
        BluetoothDevice device = (BluetoothDevice) params.get("device");

        if (device == null) {
            callback.accept("테스트할 기기가 선택되지 않았습니다.");
            return;
        }

        // 백그라운드 스레드에서 테스트 실행
        new Thread(() -> {
            StringBuilder resultLog = new StringBuilder();
            resultLog.append("--- 수동 테스트 결과 ---\n");
            resultLog.append("테스트 기기: ").append(device.getName()).append("\n");

            // 1. SPP 연결 테스트
            boolean sppResult = testSppConnection(context, device);
            resultLog.append("SPP 연결 테스트: ").append(sppResult ? "성공" : "실패").append("\n");

            // TODO: 필요한 다른 테스트 추가

            // 최종 결과를 콜백으로 ViewModel에 전달
            callback.accept(resultLog.toString());
        }).start();
    }

    @Override
    public void runAutoTest(Map<String, Object> params, Consumer<String> callback) {

    }
}
