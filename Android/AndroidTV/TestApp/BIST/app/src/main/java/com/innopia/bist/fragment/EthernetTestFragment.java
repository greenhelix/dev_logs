package com.innopia.bist.fragment;

import android.app.Application;
import android.os.Bundle;
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

import com.innopia.bist.R;
import com.innopia.bist.util.Status;
import com.innopia.bist.util.TestType;
import com.innopia.bist.viewmodel.EthernetTestViewModel;
import com.innopia.bist.viewmodel.MainViewModel;

public class EthernetTestFragment extends Fragment {

	private EthernetTestViewModel ethernetTestViewModel;
	private MainViewModel mainViewModel;
	private TextView tvEthernetInfo;

	public static EthernetTestFragment newInstance() {
		return new EthernetTestFragment();
	}

	public static class EthernetTestViewModelFactory implements ViewModelProvider.Factory {

		private final Application application;
		private final MainViewModel mainViewModel;

		public EthernetTestViewModelFactory(@NonNull Application application, @NonNull MainViewModel mainViewModel) {
			this.application = application;
			this.mainViewModel = mainViewModel;
		}

		@NonNull
		@Override
		@SuppressWarnings("unchecked")
		public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
			if (modelClass.isAssignableFrom(EthernetTestViewModel.class)) {
				return (T) new EthernetTestViewModel(application, mainViewModel);
			}
			throw new IllegalArgumentException("Unknown ViewModel class");
		}
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
		EthernetTestViewModelFactory factory = new EthernetTestViewModelFactory(
				requireActivity().getApplication(),
				mainViewModel
		);
		ethernetTestViewModel = new ViewModelProvider(this, factory).get(EthernetTestViewModel.class);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_ethernet_test, container, false);
		tvEthernetInfo = rootView.findViewById(R.id.text_ethernet_info);

		ethernetTestViewModel.testResultLiveData.observe(getViewLifecycleOwner(), result -> {
			tvEthernetInfo.setText(result.getMessage());
			boolean isConnected = result != null && result.getMessage().contains("Connected (Internet OK)");
			mainViewModel.updateHardwareStatus(TestType.ETHERNET, isConnected ? Status.ON : Status.OFF);
		});
		tvEthernetInfo.setText("Running Ethernet Test...");
		if (!mainViewModel.isAutoTestRunning.getValue()) {
			ethernetTestViewModel.startTest();
		}
		return rootView;
	}

	@Override
	public void onResume() {
		super.onResume();
	}
}
