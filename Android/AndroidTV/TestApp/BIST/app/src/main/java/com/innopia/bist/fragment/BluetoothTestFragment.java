package com.innopia.bist.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.innopia.bist.R;
import com.innopia.bist.util.Status;
import com.innopia.bist.util.TestType;
import com.innopia.bist.viewmodel.BluetoothTestViewModel;
import com.innopia.bist.viewmodel.MainViewModel;
import java.util.List;
import java.util.stream.Collectors;
import android.bluetooth.BluetoothDevice; // Import BluetoothDevice

/**
 * Fragment for displaying and running Bluetooth tests.
 * It follows the MVVM pattern, interacting with BluetoothTestViewModel.
 */
public class BluetoothTestFragment extends Fragment {

	private static final String TAG = "BIST_BluetoothTestFragment";
	private BluetoothTestViewModel bluetoothTestViewModel;
	private MainViewModel mainViewModel;

	// TextViews are now separated for different purposes.
	private TextView tvBluetoothInfo; // For detailed device info
	private TextView tvSelectedDevice; // For the name of the device selected for the test
	private TextView tvTestResult; // ADDED: For displaying the connection test result

	public static BluetoothTestFragment newInstance() {
		return new BluetoothTestFragment();
	}

	// Launcher for requesting the BLUETOOTH_CONNECT permission.
	private final ActivityResultLauncher<String> requestPermissionLauncher =
			registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
				if (isGranted) {
					String msg = "BLUETOOTH_CONNECT permission granted.";
					mainViewModel.appendLog(getTag(), msg);
					Log.d(TAG, msg);
					bluetoothTestViewModel.onScanClicked();
				} else {
					Toast.makeText(getContext(), "Bluetooth Connect permission is required to scan for devices.", Toast.LENGTH_SHORT).show();
				}
			});

	/**
	 * Custom ViewModelFactory to pass MainViewModel instance into BluetoothTestViewModel.
	 */
	public static class BluetoothTestViewModelFactory implements ViewModelProvider.Factory {
		private final Application application;
		private final MainViewModel mainViewModel;

		public BluetoothTestViewModelFactory(@NonNull Application application, @NonNull MainViewModel mainViewModel) {
			this.application = application;
			this.mainViewModel = mainViewModel;
		}

		@NonNull
		@Override
		@SuppressWarnings("unchecked")
		public <T extends androidx.lifecycle.ViewModel> T create(@NonNull Class<T> modelClass) {
			if (modelClass.isAssignableFrom(BluetoothTestViewModel.class)) {
				return (T) new BluetoothTestViewModel(application, mainViewModel);
			}
			throw new IllegalArgumentException("Unknown ViewModel class");
		}
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Obtain the shared MainViewModel from the activity.
		mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
		// Use the custom factory to create the BluetoothTestViewModel instance.
		BluetoothTestViewModelFactory factory = new BluetoothTestViewModelFactory(requireActivity().getApplication(), mainViewModel);
		bluetoothTestViewModel = new ViewModelProvider(this, factory).get(BluetoothTestViewModel.class);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		// Inflate the layout for this fragment.
		View rootView = inflater.inflate(R.layout.fragment_bluetooth_test, container, false);

		// Initialize all three TextViews.
		tvBluetoothInfo = rootView.findViewById(R.id.text_bluetooth_info);
		tvSelectedDevice = rootView.findViewById(R.id.text_selected_device);
		tvTestResult = rootView.findViewById(R.id.text_bluetooth_test_result); // ADDED

		// Setup buttons and their click listeners.
		Button btnBluetoothScan = rootView.findViewById(R.id.btn_bluetooth_scan);
		Button btnBluetoothTest = rootView.findViewById(R.id.btn_bluetooth_test);
		btnBluetoothScan.setOnClickListener(v -> checkPermissionAndScan());
		btnBluetoothTest.setOnClickListener(v -> bluetoothTestViewModel.startTest());

		observeViewModel();
		return rootView;
	}

	@SuppressLint("MissingPermission")
	private void observeViewModel() {

		bluetoothTestViewModel.deviceInfo.observe(getViewLifecycleOwner(), info -> tvBluetoothInfo.setText(info));

		bluetoothTestViewModel.selectedDevice.observe(getViewLifecycleOwner(), device -> {
			if (device != null) {
				tvSelectedDevice.setText("Selected for Test: " + device.getName());
			} else {
				tvSelectedDevice.setText("Selected for Test: None");
			}
		});

		bluetoothTestViewModel.testResultLiveData.observe(getViewLifecycleOwner(), result -> tvTestResult.setText(result.getMessage()));

		bluetoothTestViewModel.devicesForDialog.observe(getViewLifecycleOwner(), devices -> {
			if (devices != null && !devices.isEmpty()) {
				showDeviceSelectionDialog(devices);
				bluetoothTestViewModel.onDialogShown(); // Reset the trigger.
			}
		});

		bluetoothTestViewModel.navigateToSettings.observe(getViewLifecycleOwner(), shouldNavigate -> {
			if (shouldNavigate) {
				openAddAccessoryScreen();
				bluetoothTestViewModel.onNavigatedToSettings(); // Reset the trigger.
			}
		});

		bluetoothTestViewModel.testResultLiveData.observe(getViewLifecycleOwner(), result ->{
			mainViewModel.appendLog(getTag(), result.getMessage());
			boolean isConnected = result != null && !result.getMessage().contains("not connected");
			mainViewModel.updateHardwareStatus(TestType.BLUETOOTH, isConnected? Status.ON : Status.OFF);
		});
	}

	private void checkPermissionAndScan() {
		if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
			bluetoothTestViewModel.onScanClicked();
		} else {
			requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
		}
	}

	@SuppressLint("MissingPermission")
	private void showDeviceSelectionDialog(List<BluetoothDevice> devices) {
		if (!isAdded()) return; // Ensure fragment is still attached to the activity.

		// Get a list of device names for the dialog.
		List<String> deviceNames = devices.stream()
				.map(BluetoothDevice::getName)
				.collect(Collectors.toList());
		deviceNames.add("Scan for other devices..."); // Option to go to settings.

		LayoutInflater inflater = LayoutInflater.from(getContext());
		View dialogView = inflater.inflate(R.layout.dialog_custom_list, null);
		TextView title = dialogView.findViewById(R.id.dialog_list_title);
		LinearLayout container = dialogView.findViewById(R.id.dialog_list_container);
		title.setText("Select Test Device");
		AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
				.setView(dialogView)
				.setCancelable(true);
		final AlertDialog dialog = builder.create();
		for (BluetoothDevice device : devices) {
			Button deviceButton = new Button(getContext());
			deviceButton.setText(device.getName());
			deviceButton.setBackgroundResource(R.drawable.button_focus_selector);
			deviceButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
			deviceButton.setOnClickListener(v -> {
				bluetoothTestViewModel.onDeviceSelected(device);
				dialog.dismiss();
			});
			container.addView(deviceButton);
		}
		Button scanButton = new Button(getContext());
		scanButton.setText("Scan for other devices...");
		scanButton.setBackgroundResource(R.drawable.button_focus_selector);
		scanButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
		scanButton.setOnClickListener(v -> {
			openAddAccessoryScreen();
			dialog.dismiss();
		});
		container.addView(scanButton);

		dialog.setOnShowListener(dialogInterface -> {
			if (container.getChildCount() > 0) {
				container.getChildAt(0).requestFocus();
			}
		});

		dialog.show();
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "onResume()");
	}

	/**
	 * Opens the Android TV settings screen for adding a new Bluetooth accessory.
	 * Falls back to the general settings screen if the specific activity is not found.
	 */
	private void openAddAccessoryScreen() {
		String msg = "Opening Add Accessory screen.";
		mainViewModel.appendLog(getTag(), msg);
		Log.d(TAG, msg);
		try {
			Intent intent = new Intent();
			intent.setClassName("com.android.tv.settings", "com.android.tv.settings.accessories.AddAccessoryActivity");
			startActivity(intent);
		} catch (Exception e) {
			Log.e(TAG, "Failed to open AddAccessoryActivity, falling back to general settings.", e);
			startActivity(new Intent(Settings.ACTION_SETTINGS));
		}
	}
}
