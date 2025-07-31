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
import com.innopia.bist.util.TestStatus;
import com.innopia.bist.util.TestType;
import com.innopia.bist.viewmodel.RcuTestViewModel;
import com.innopia.bist.viewmodel.MainViewModel;


public class RcuTestFragment extends Fragment {

	private static final String TAG = "RcuTestFragment";

	private RcuTestViewModel rcuTestViewModel;
	private MainViewModel mainViewModel;
	private View rootView;
	private TextView tvRcuTest;
	private Button btnRcuTest;

	public static RcuTestFragment newInstance() {
		return new RcuTestFragment();
	}

	public static class RcuTestViewModelFactory implements ViewModelProvider.Factory {

		private final Application application;
		private final MainViewModel mainViewModel;

		public RcuTestViewModelFactory(@NonNull Application application, @NonNull MainViewModel mainViewModel) {
			this.application = application;
			this.mainViewModel = mainViewModel;
		}

		@NonNull
		@Override
		@SuppressWarnings("unchecked")
		public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
			if (modelClass.isAssignableFrom(RcuTestViewModel.class)) {
				return (T) new RcuTestViewModel(application, mainViewModel);
			}
			throw new IllegalArgumentException("Unknown ViewModel class");
		}
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
		RcuTestViewModelFactory factory = new RcuTestViewModelFactory(requireActivity().getApplication(), mainViewModel);
		rcuTestViewModel = new ViewModelProvider(this, factory).get(RcuTestViewModel.class);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.fragment_rcu_test, container, false);
		tvRcuTest = rootView.findViewById(R.id.text_rcu_test);
//		btnRcuTest = rootView.findViewById(R.id.btn_rcu_test);
//		btnRcuTest.setOnClickListener(v -> {
//			btnRcuTest.setVisibility(View.GONE);
//			rcuTestViewModel.startTest();
//			setupKeyListener();
//			rootView.requestFocus();
//		});
		rcuTestViewModel.startTest();
		mainViewModel.updateTestResult(TestType.RCU, TestStatus.RUNNING);
		setupKeyListener();
		rootView.requestFocus();

		// Observe the LiveData from the ViewModel to update the UI.
		rcuTestViewModel.testResultLiveData.observe(getViewLifecycleOwner(), result -> {
			if (result != null) {
				tvRcuTest.setText(result.getMessage());
			}
		});

		rcuTestViewModel.testCompletedEvent.observe(getViewLifecycleOwner(), aVoid -> {
//			btnRcuTest.setVisibility(View.VISIBLE);
//			btnRcuTest.requestFocus();
			rootView.setFocusable(false);

		});

		return rootView;
	}

	private void setupKeyListener() {
		rootView.setFocusableInTouchMode(true);
		rootView.setOnKeyListener((v, keyCode, event) -> {
			rcuTestViewModel.onKeyEvent(event);
			return true;
		});
	}

	@Override
	public void onResume() {
		super.onResume();
		mainViewModel.setLogAutoScrollEnabled(false);
//		btnRcuTest.requestFocus();
	}

	@Override
	public void onPause() {
		super.onPause();
		mainViewModel.setLogAutoScrollEnabled(true);
	}
}
