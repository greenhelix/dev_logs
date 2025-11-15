package com.innopia.bist.ver1.fragment;

import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.innopia.bist.ver1.util.Status;
import com.innopia.bist.ver1.util.TestType;
import com.innopia.bist.ver1.viewmodel.MainViewModel;
import com.innopia.bist.ver1.viewmodel.WifiTestViewModel;

public class WifiTestFragment extends Fragment {

	private WifiTestViewModel wifiTestViewModel;
	private MainViewModel mainViewModel;
	private TextView tvWifiInfo;

	public static WifiTestFragment newInstance() {
		return new WifiTestFragment();
	}

	public static class WifiTestViewModelFactory implements ViewModelProvider.Factory {
		private final Application application;
		private final MainViewModel mainViewModel;

		public WifiTestViewModelFactory(@NonNull Application application, @NonNull MainViewModel mainViewModel) {
			this.application = application;
			this.mainViewModel = mainViewModel;
		}

		@NonNull
		@Override
		@SuppressWarnings("unchecked")
		public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
			if (modelClass.isAssignableFrom(WifiTestViewModel.class)) {
				return (T) new WifiTestViewModel(application, mainViewModel);
			}
			throw new IllegalArgumentException("Unknown ViewModel class");
		}
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
		WifiTestViewModelFactory factory = new WifiTestViewModelFactory(
				requireActivity().getApplication(),
				mainViewModel
		);
		wifiTestViewModel = new ViewModelProvider(this, factory).get(WifiTestViewModel.class);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_wifi_test, container, false);

		tvWifiInfo = rootView.findViewById(R.id.text_wifi_info);
		Button btnWifiScan = rootView.findViewById(R.id.btn_wifi_scan);
		Button btnWifiTest = rootView.findViewById(R.id.btn_wifi_test);

		btnWifiScan.setOnClickListener(v -> {
			mainViewModel.appendLog("WifiTestFragment", "Scan button clicked. Opening system Wi-Fi settings...");
			startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
		});

		btnWifiTest.setOnClickListener(v -> {
			tvWifiInfo.setText("Running Wi-Fi Test...");
			wifiTestViewModel.startTest();
		});

		wifiTestViewModel.testResultLiveData.observe(getViewLifecycleOwner(), result -> {
			mainViewModel.appendLog(getTag(), result.getMessage());
			tvWifiInfo.setText(result.getMessage());
			boolean isConnected = result != null && result.getMessage().contains("Connected (Internet OK)");
			mainViewModel.updateHardwareStatus(TestType.WIFI, isConnected ? Status.ON : Status.OFF);
		});

		tvWifiInfo.setText("Running Wi-Fi Test...");
		if (!mainViewModel.isAutoTestRunning.getValue()) {
			wifiTestViewModel.startTest();
		}
		return rootView;
	}

	@Override
	public void onResume() {
		super.onResume();
	}
}
