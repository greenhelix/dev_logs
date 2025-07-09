package com.ik.innopia.hubist.side.test.wifi;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.ik.innopia.hubist.databinding.FragmentWifiScanListBinding;

import java.util.Arrays;
import java.util.List;

public class WifiScanListDialogFragment extends DialogFragment {

    private FragmentWifiScanListBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentWifiScanListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecyclerView();
        startWifiScanSimulation();
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = getDialog().getWindow();
        if (window != null) {
            // 사이드 패널처럼 보이게 하는 핵심 코드
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.END; // 화면 오른쪽에 붙이기
            params.width = getResources().getDisplayMetrics().widthPixels / 2; // 화면 너비의 절반 크기
            params.height = WindowManager.LayoutParams.MATCH_PARENT; // 높이는 꽉 채우기
            window.setAttributes(params);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // 기본 배경 제거
        }
    }

    private void setupRecyclerView() {
        binding.wifiListRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // 어댑터는 아래 5단계에서 생성
        // binding.wifiListRecyclerView.setAdapter(new WifiListAdapter(...));
    }

    private void startWifiScanSimulation() {
        binding.scanProgressBar.setVisibility(View.VISIBLE);
        binding.wifiListRecyclerView.setVisibility(View.GONE);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (binding == null) return; // 다이얼로그가 닫혔을 경우를 대비

            List<String> fakeWifiList = Arrays.asList(
                    "MyHome_WiFi_5G", "Office_Guest", "Cafe_WiFi",
                    "Neighbor_AP", "Public_Free_WiFi", "Android_Hotspot"
            );

            binding.scanProgressBar.setVisibility(View.GONE);
            binding.wifiListRecyclerView.setVisibility(View.VISIBLE);

            // TODO: 실제 어댑터를 생성하여 연결
            // binding.wifiListRecyclerView.setAdapter(new WifiListAdapter(fakeWifiList));

        }, 1500); // 1.5초 딜레이
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
