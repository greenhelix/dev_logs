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
import com.innopia.bist.viewmodel.MainViewModel;
import com.innopia.bist.viewmodel.MemoryTestViewModel;

public class MemoryTestFragment extends Fragment {

	private MemoryTestViewModel memoryTestViewModel;
	private MainViewModel mainViewModel;
	private TextView tvMemoryInfo;

	public static MemoryTestFragment newInstance() {
		return new MemoryTestFragment();
	}

	public static class MemoryTestViewModelFactory implements ViewModelProvider.Factory {
		private final Application application;
		private final MainViewModel mainViewModel;

		public MemoryTestViewModelFactory(@NonNull Application application, @NonNull MainViewModel mainViewModel) {
			this.application = application;
			this.mainViewModel = mainViewModel;
		}

		@NonNull
		@Override
		@SuppressWarnings("unchecked")
		public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
			if (modelClass.isAssignableFrom(MemoryTestViewModel.class)) {
				return (T) new MemoryTestViewModel(application, mainViewModel);
			}
			throw new IllegalArgumentException("Unknown ViewModel class");
		}
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
		MemoryTestViewModelFactory factory = new MemoryTestViewModelFactory(requireActivity().getApplication(), mainViewModel);
		memoryTestViewModel = new ViewModelProvider(this, factory).get(MemoryTestViewModel.class);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_memory_test, container, false);
		tvMemoryInfo = rootView.findViewById(R.id.text_memory_info);
		Button btnMemoryManTest = rootView.findViewById(R.id.btn_memory_manual_test);
		btnMemoryManTest.setOnClickListener(v -> {
			mainViewModel.appendLog(getTag(), "Memory Test Start");
			memoryTestViewModel.startTest();
		});

		memoryTestViewModel.testResultLiveData.observe(getViewLifecycleOwner(), result -> {
			tvMemoryInfo.setText(result.getMessage());
			mainViewModel.appendLog(getTag(), "Memory Result \n"+ result);
		});

		return rootView;
	}
}