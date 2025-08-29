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
import com.innopia.bist.viewmodel.ButtonTestViewModel;
import com.innopia.bist.viewmodel.MainViewModel;

public class ButtonTestFragment extends Fragment {

	private ButtonTestViewModel buttonTestViewModel;
	private MainViewModel mainViewModel;
	private View rootView;
	private TextView tvButton;

	public static ButtonTestFragment newInstance() {
		return new ButtonTestFragment();
	}

	public static class ButtonTestViewModelFactory implements ViewModelProvider.Factory {
		private final Application application;
		private final MainViewModel mainViewModel;

		public ButtonTestViewModelFactory(@NonNull Application application, @NonNull MainViewModel mainViewModel) {
			this.application = application;
			this.mainViewModel = mainViewModel;
		}

		@NonNull
		@Override
		@SuppressWarnings("unchecked")
		public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
			if (modelClass.isAssignableFrom(ButtonTestViewModel.class)) {
				return (T) new ButtonTestViewModel(application, mainViewModel);
			}
			throw new IllegalArgumentException("Unknown ViewModel class");
		}
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
		ButtonTestViewModelFactory factory = new ButtonTestViewModelFactory(requireActivity().getApplication(), mainViewModel);
		buttonTestViewModel = new ViewModelProvider(this, factory).get(ButtonTestViewModel.class);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.fragment_button_test, container, false);
		tvButton = rootView.findViewById(R.id.text_button);

		buttonTestViewModel.testResultLiveData.observe(getViewLifecycleOwner(), result -> {
			if (result != null) {
				tvButton.setText(result.getMessage());
			}
		});

		buttonTestViewModel.testCompletedEvent.observe(getViewLifecycleOwner(), aVoid -> {
			rootView.setFocusable(false);
		});

		buttonTestViewModel.startTest();
		rootView.requestFocus();
		rootView.setFocusable(true);
		rootView.setOnKeyListener((v, keyCode, event) -> {
			buttonTestViewModel.onKeyEvent(event);
			return true;
		});
		rootView.post(() -> rootView.requestFocus());

		return rootView;
	}
}
