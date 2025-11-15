package com.innopia.bist.ver1.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.innopia.bist.ver1.viewmodel.HdmiTestViewModel;
import com.innopia.bist.ver1.viewmodel.MainViewModel;

/**
 * A Fragment that displays the UI for the HDMI test.
 * It allows the user to start the test and shows the results.
 */
public class HdmiTestFragment extends Fragment {

	private HdmiTestViewModel hdmiTestViewModel;
	private MainViewModel mainViewModel;
	private TextView tvHdmiInfo;

	public static HdmiTestFragment newInstance() {
		return new HdmiTestFragment();
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

		ViewModelProvider.Factory factory = new ViewModelProvider.Factory() {
			@NonNull
			@Override
			public <T extends androidx.lifecycle.ViewModel> T create(@NonNull Class<T> modelClass) {
				if (modelClass.isAssignableFrom(HdmiTestViewModel.class)) {
					return (T) new HdmiTestViewModel(requireActivity().getApplication(), mainViewModel);
				}
				throw new IllegalArgumentException("Unknown ViewModel class");
			}
		};
		hdmiTestViewModel = new ViewModelProvider(this, factory).get(HdmiTestViewModel.class);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_hdmi_test, container, false);
		tvHdmiInfo = rootView.findViewById(R.id.text_hdmi_info);

		hdmiTestViewModel.testResultLiveData.observe(getViewLifecycleOwner(), result -> {
			tvHdmiInfo.setText(result.getMessage());
			mainViewModel.appendLog(getTag(), "HDMI Result \n" + result);
		});

		mainViewModel.appendLog(getTag(), "HDMI Test Start");
		hdmiTestViewModel.startTest();

		return rootView;
	}
}
