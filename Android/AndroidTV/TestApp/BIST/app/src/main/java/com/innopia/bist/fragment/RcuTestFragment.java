package com.innopia.bist.fragment;

import android.app.Application;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
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
import com.innopia.bist.util.TestResult;
import com.innopia.bist.util.TestStatus;
import com.innopia.bist.util.TestType;
import com.innopia.bist.viewmodel.RcuTestViewModel;
import com.innopia.bist.viewmodel.MainViewModel;
import com.innopia.bist.viewmodel.RcuViewModelFactory;


public class RcuTestFragment extends Fragment {

	private static final String TAG = "BIST_RcuTestFragment";

	private RcuTestViewModel rcuTestViewModel;
	private MainViewModel mainViewModel;
	private View rootView;
	private TextView tvRcuTest;
	private Button btnRcuTest;

	public static RcuTestFragment newInstance() {
		return new RcuTestFragment();
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
		RcuViewModelFactory factory = new RcuViewModelFactory(requireActivity().getApplication(), mainViewModel);
		rcuTestViewModel = new ViewModelProvider(requireActivity(), factory).get(RcuTestViewModel.class);
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
		Log.d(TAG, "start Test RCU");
		rcuTestViewModel.startTest();
		mainViewModel.updateTestResult(TestType.RCU, TestStatus.RUNNING);
		setupKeyListener();
		rootView.requestFocus();

		// Observe the LiveData from the ViewModel to update the UI.
		rcuTestViewModel.testResultLiveData.observe(getViewLifecycleOwner(), result -> {
			if (result != null) {
				tvRcuTest.setText(result.getMessage());
				Log.d(TAG, "result Test RCU"+result.getMessage());
			}
		});

//		rcuTestViewModel.testCompletedEvent.observe(getViewLifecycleOwner(), aVoid -> {
////			btnRcuTest.setVisibility(View.VISIBLE);
////			btnRcuTest.requestFocus();
//			rootView.setFocusable(false);
//		});

		return rootView;
	}

	private void setupKeyListener() {
		rootView.setFocusableInTouchMode(true);
		rootView.setOnKeyListener((v, keyCode, event) -> {
			TestResult currentResult = rcuTestViewModel.testResultLiveData.getValue();
			if (currentResult != null && currentResult.getStatus() == TestStatus.PASSED) {
				return false;
			}
			if (event.getAction() == KeyEvent.ACTION_DOWN) {
				rcuTestViewModel.onKeyEvent(event);
			}
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
