package com.ik.innopia.hubist.side.test.wifi;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.ik.innopia.hubist.databinding.FragmentWifiTestBinding;

public class WifiTestFragment extends Fragment {

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
