package com.innopia.bist;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import com.innopia.bist.fragment.BluetoothTestFragment;
import com.innopia.bist.fragment.CpuTestFragment;
import com.innopia.bist.fragment.EthernetTestFragment;
import com.innopia.bist.fragment.HdmiTestFragment;
import com.innopia.bist.fragment.MemoryTestFragment;
import com.innopia.bist.fragment.RcuTestFragment;
import com.innopia.bist.fragment.UsbTestFragment;
import com.innopia.bist.fragment.VideoTestFragment;
import com.innopia.bist.fragment.WifiTestFragment;
import com.innopia.bist.info.HwInfo;
import com.innopia.bist.info.SystemInfo;
import com.innopia.bist.util.SecretCodeManager;
import com.innopia.bist.util.ServiceUtils;
import com.innopia.bist.util.Status;
import com.innopia.bist.util.TestStatus;
import com.innopia.bist.util.TestType;
import com.innopia.bist.util.UsbDetachReceiver;
import com.innopia.bist.viewmodel.MainViewModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class MainActivity extends AppCompatActivity {

	private static final String TAG = "BIST_MAIN";
	private static final String TAG_TEST_FRAGMENT = "TEST_FRAGMENT";

	private MainViewModel mainViewModel;
	private BroadcastReceiver appUsbDetachReceiver;

	private List<Button> mainTestButtons;
	private View defaultFocusButton;
	private SecretCodeManager secretCodeManager;
	private boolean isFocusHighlightEnabled = true;

	private ImageView ivWifiStatus;
	private ImageView ivBtStatus;
	private ImageView ivEthStatus;
	private TextView tvLogWindow;
	private ScrollView svLog;

	private Map<TestType, Button> testButtonMap;

	private static final int ALL_PERMISSIONS_REQUEST_CODE = 100;
	private final String[] REQUIRED_PERMISSIONS = new String[]{
			Manifest.permission.ACCESS_FINE_LOCATION,
			Manifest.permission.ACCESS_COARSE_LOCATION,
			Manifest.permission.BLUETOOTH_SCAN,
			Manifest.permission.BLUETOOTH_CONNECT,
			Manifest.permission.ACCESS_WIFI_STATE,
			Manifest.permission.CHANGE_WIFI_STATE,
			Manifest.permission.ACCESS_NETWORK_STATE,
			Manifest.permission.CHANGE_NETWORK_STATE,
			Manifest.permission.NEARBY_WIFI_DEVICES,
			Manifest.permission.WRITE_EXTERNAL_STORAGE,
			Manifest.permission.READ_EXTERNAL_STORAGE,
			Manifest.permission.MANAGE_EXTERNAL_STORAGE
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		secretCodeManager = new SecretCodeManager(this);
		mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

		setupViews();
		setupObservers();
		setupBackButtonHandler();
		checkAndRequestPermissions();

		mainViewModel.appendLog(TAG, "Activity starting.");
		mainViewModel.setSysInfo(getSysInfo());
		mainViewModel.setHwInfo(getHwInfo());

		appUsbDetachReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (UsbDetachReceiver.ACTION_USB_DETACHED_APP.equals(intent.getAction())) {
					mainViewModel.appendLog(TAG, "USB device detach detected.");
				}
			}
		};

		checkAndLogBistServiceStatus();
	}

	private void setupViews() {
		ivWifiStatus = findViewById(R.id.iv_wifi_status);
		ivBtStatus = findViewById(R.id.iv_bt_status);
		ivEthStatus = findViewById(R.id.iv_ethernet_status);
		tvLogWindow = findViewById(R.id.text_log_window);
		svLog = findViewById(R.id.log_scroll_view);
		TextView sysInfoText = findViewById(R.id.text_sys_info);
		TextView hwInfoText = findViewById(R.id.text_hw_info);

		mainTestButtons = new ArrayList<>();
		testButtonMap = new HashMap<>();

		Button btnEthernetTest = findViewById(R.id.button_ethernet_test);
		btnEthernetTest.setOnClickListener(v -> showTestFragment(EthernetTestFragment.newInstance()));
		mainTestButtons.add(btnEthernetTest);
		testButtonMap.put(TestType.ETHERNET, btnEthernetTest);
		defaultFocusButton = btnEthernetTest;

		Button btnWifiTest = findViewById(R.id.button_wifi_test);
		btnWifiTest.setOnClickListener(v -> showTestFragment(WifiTestFragment.newInstance()));
		mainTestButtons.add(btnWifiTest);
		testButtonMap.put(TestType.WIFI, btnWifiTest);

		Button btn_BluetoothTest = findViewById(R.id.button_bt_test);
		btn_BluetoothTest.setOnClickListener(v -> showTestFragment(BluetoothTestFragment.newInstance()));
		mainTestButtons.add(btn_BluetoothTest);
		testButtonMap.put(TestType.BLUETOOTH, btn_BluetoothTest);

		Button btnHdmiTest = findViewById(R.id.button_hdmi_test);
		btnHdmiTest.setOnClickListener(v -> showTestFragment(HdmiTestFragment.newInstance()));
		mainTestButtons.add(btnHdmiTest);
		testButtonMap.put(TestType.HDMI, btnHdmiTest);

		Button btnVideoTest = findViewById(R.id.button_video_test);
		btnVideoTest.setOnClickListener(v -> showTestFragment(VideoTestFragment.newInstance()));
		mainTestButtons.add(btnVideoTest);
		testButtonMap.put(TestType.VIDEO, btnVideoTest);

		Button btnUsbTest = findViewById(R.id.button_usb_test);
		btnUsbTest.setOnClickListener(v -> showTestFragment(UsbTestFragment.newInstance()));
		mainTestButtons.add(btnUsbTest);
		testButtonMap.put(TestType.USB, btnUsbTest);

		Button btnMemoryTest = findViewById(R.id.button_memory_test);
		btnMemoryTest.setOnClickListener(v -> showTestFragment(MemoryTestFragment.newInstance()));
		mainTestButtons.add(btnMemoryTest);
		testButtonMap.put(TestType.MEMORY, btnMemoryTest);

		Button btnCpuTest = findViewById(R.id.button_cpu_test);
		btnCpuTest.setOnClickListener(v -> showTestFragment(CpuTestFragment.newInstance()));
		mainTestButtons.add(btnCpuTest);
		testButtonMap.put(TestType.CPU, btnCpuTest);

		Button btnRcuTest = findViewById(R.id.button_rcu_test);
		btnRcuTest.setOnClickListener(v -> showTestFragment(RcuTestFragment.newInstance()));
		mainTestButtons.add(btnRcuTest);
		testButtonMap.put(TestType.RCU, btnRcuTest);

		Button btnFactoryResetTest = findViewById(R.id.button_factory_reset_test);
		btnFactoryResetTest.setOnClickListener(v -> startFactoryReset());
		mainTestButtons.add(btnFactoryResetTest);

		Button btnRebootTest = findViewById(R.id.button_reboot_test);
		btnRebootTest.setOnClickListener(v -> startReboot());
		mainTestButtons.add(btnRebootTest);

		Button btnSettingsTest = findViewById(R.id.button_settings_test);
		btnSettingsTest.setOnClickListener(v -> startSettings());
		mainTestButtons.add(btnSettingsTest);

		Button btnStartAutoTest = findViewById(R.id.button_start_auto_test);
		btnStartAutoTest.setOnClickListener(v -> {
			// mainViewModel.startAutoTest("/storage/usb_storage");
			mainViewModel.startAutoTest(true); //for testing
		});
		Button btnResetTest = findViewById(R.id.button_reset_test);
		btnResetTest.setOnClickListener(v -> {
			mainViewModel.resetAllTests();
		});

		mainTestButtons.add(btnStartAutoTest);

		mainViewModel.sysInfoLiveData.observe(this, sysInfoText::setText);
		mainViewModel.hwInfoLiveData.observe(this, hwInfoText::setText);
	}

	private void setupObservers() {
		mainViewModel.logLiveData.observe(this, logs -> {
			if (logs != null) {
				tvLogWindow.setText(String.join("\n", logs));
				if (mainViewModel.isLogAutoScrollEnabled() && svLog.hasFocus()) {
					int bottom = svLog.getHeight();
					svLog.smoothScrollTo(0, bottom);
				}
			}
		});

		// This observer handles the top status icons (Wi-Fi, BT, etc.)
		mainViewModel.hardwareStatusLiveData.observe(this, statuses -> {
			if (statuses == null) return;
			updateStatusIcon(ivWifiStatus, statuses.get(TestType.WIFI) == Status.ON, R.drawable.ic_wifi_on, R.drawable.ic_wifi_off);
			updateStatusIcon(ivBtStatus, statuses.get(TestType.BLUETOOTH) == Status.ON, R.drawable.ic_bt_on, R.drawable.ic_bt_off);
			updateStatusIcon(ivEthStatus, statuses.get(TestType.ETHERNET) == Status.ON, R.drawable.ic_ethernet_on, R.drawable.ic_ethernet_off);
		});

		// Observer for auto-test running state to enable/disable buttons.
		mainViewModel.isAutoTestRunning.observe(this, isRunning -> {
			for (Button button : mainTestButtons) {
				if (button != null) button.setEnabled(!isRunning);
			}
		});

		// Observer for individual test statuses to update button colors.
		mainViewModel.testStatusesLiveData.observe(this, statuses -> {
			if (statuses == null) return;
			for (Map.Entry<TestType, TestStatus> entry : statuses.entrySet()) {
				updateButtonUI(entry.getKey(), entry.getValue());
			}
		});

		// Observer for user action requests to show dialogs during auto-test.
		mainViewModel.userActionRequired.observe(this, message -> {
			if (message != null && !message.isEmpty()) {
				showUserActionDialog(message);
			}
		});
		mainViewModel.navigateToTestFragment.observe(this, testType -> {
			if (testType != null) {
				showFragmentForAutoTest(testType);
			}
		});
		mainViewModel.clearFragmentContainer.observe(this, aVoid -> {
			clearFragmentContainer();
		});
	}

	private void showFragmentForAutoTest(TestType type) {
		Fragment fragmentToShow;
		switch (type) {
			case WIFI:
				fragmentToShow = WifiTestFragment.newInstance();
				break;
			case BLUETOOTH:
				fragmentToShow = BluetoothTestFragment.newInstance();
				break;
			case ETHERNET:
				fragmentToShow = EthernetTestFragment.newInstance();
				break;
			case CPU:
				fragmentToShow = CpuTestFragment.newInstance();
				break;
			case MEMORY:
				fragmentToShow = MemoryTestFragment.newInstance();
				break;
			case VIDEO:
				fragmentToShow = VideoTestFragment.newInstance();
				break;
			case HDMI:
				fragmentToShow = HdmiTestFragment.newInstance();
				break;
			case USB:
				fragmentToShow = UsbTestFragment.newInstance();
				break;
			case RCU:
				fragmentToShow = RcuTestFragment.newInstance();
				break;
			default:
				// If there's no fragment for a test type, just clear the view.
				clearFragmentContainer();
				return;
		}

		mainViewModel.appendLog(TAG, "Auto-test showing fragment for: " + type.name());
		getSupportFragmentManager().beginTransaction()
				.replace(R.id.fragment_container, fragmentToShow, TAG_TEST_FRAGMENT)
				// DO NOT add to back stack for auto-test navigation
				.commit();
	}

	private void clearFragmentContainer() {
		FragmentManager fm = getSupportFragmentManager();
		Fragment currentFragment = fm.findFragmentByTag(TAG_TEST_FRAGMENT);
		if (currentFragment != null) {
			mainViewModel.appendLog(TAG, "Auto-test finished. Clearing fragment container.");
			fm.beginTransaction().remove(currentFragment).commit();
			// Restore focus to a default button if needed
			if(defaultFocusButton != null) {
				defaultFocusButton.requestFocus();
			}
		}
	}

	private void updateButtonUI(TestType type, TestStatus status) {
		Button button = testButtonMap.get(type);
		if (button == null) return;

		int colorResId;
		switch (status) {
			case PASSED: colorResId = R.color.green; break;
			case FAILED: colorResId = R.color.red; break;
			case RETEST: colorResId = R.color.purple; break;
			case RUNNING:
			case WAITING_FOR_USER: colorResId = R.color.yellow; break;
			case PENDING:
			default: colorResId = R.color.normal; break;
		}
		int backgroundColor = ContextCompat.getColor(this, colorResId);

		// 2. 기본 상태의 드로어블 생성 (색상이 적용된 사각형)
		GradientDrawable defaultDrawable = new GradientDrawable();
		defaultDrawable.setShape(GradientDrawable.RECTANGLE);
		defaultDrawable.setCornerRadius(4 * getResources().getDisplayMetrics().density); // 4dp
		defaultDrawable.setColor(backgroundColor);
		defaultDrawable.setStroke((int) (3 * getResources().getDisplayMetrics().density), Color.BLACK);
		// 3. 포커스 상태의 드로어블 생성 (기본 드로어블 위에 노란 테두리를 덧씌움)
		Drawable[] focusLayers = new Drawable[2];
		focusLayers[0] = defaultDrawable; // 아래층: 색상이 적용된 배경
		focusLayers[1] = ContextCompat.getDrawable(this, R.drawable.button_state_selector); // 위층: 노란 테두리
		LayerDrawable focusDrawable = new LayerDrawable(focusLayers);

		// 4. StateListDrawable을 만들어 상태별 드로어블을 지정
		StateListDrawable stateListDrawable = new StateListDrawable();
		// 포커스 상태일 때는 LayerDrawable을 사용
		stateListDrawable.addState(new int[]{android.R.attr.state_focused}, focusDrawable);
		// 기본 상태일 때는 색상만 있는 드로어블을 사용
		stateListDrawable.addState(new int[]{}, defaultDrawable);

		// 5. 완성된 StateListDrawable을 버튼의 최종 배경으로 설정
		button.setBackground(stateListDrawable);
	}

	// This method shows a dialog when user interaction is needed.
	private void showUserActionDialog(String message) {
		// Inflate the custom layout
		LayoutInflater inflater = LayoutInflater.from(this);
		View dialogView = inflater.inflate(R.layout.dialog_custom_action, null);

		// Find views in the custom layout
		TextView tvMessage = dialogView.findViewById(R.id.dialog_message);
		tvMessage.setText(message);
		LinearLayout yesNoLayout = dialogView.findViewById(R.id.dialog_yes_no_layout);
		Button btnYes = dialogView.findViewById(R.id.dialog_button_yes);
		Button btnNo = dialogView.findViewById(R.id.dialog_button_no);
		Button btnOk = dialogView.findViewById(R.id.dialog_button_ok);

		AlertDialog.Builder builder = new AlertDialog.Builder(this)
				.setView(dialogView)
				.setTitle("User Action Required")
				.setCancelable(false);

		final AlertDialog dialog = builder.create();

		if (message.contains("?")) {
			yesNoLayout.setVisibility(View.VISIBLE);
			btnOk.setVisibility(View.GONE);

			btnYes.setOnClickListener(v -> {
				mainViewModel.userActionConfirmed(true);
				dialog.dismiss();
			});
			btnNo.setOnClickListener(v -> {
				mainViewModel.userActionConfirmed(false);
				dialog.dismiss();
			});

			// Set default focus on the YES button when the dialog appears
			dialog.setOnShowListener(dialogInterface -> btnYes.requestFocus());

		} else {
			yesNoLayout.setVisibility(View.GONE);
			btnOk.setVisibility(View.VISIBLE);

			btnOk.setOnClickListener(v -> {
				// For informational dialogs, "OK" implies the user has acknowledged or performed the action.
				mainViewModel.userActionConfirmed(true);
				dialog.dismiss();
			});

			// Set default focus on the OK button when the dialog appears
			dialog.setOnShowListener(dialogInterface -> btnOk.requestFocus());
		}

		dialog.show();
	}

	private void setupBackButtonHandler() {
		OnBackPressedCallback callback = new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				FragmentManager fm = getSupportFragmentManager();
				Fragment currentFragment = fm.findFragmentByTag(TAG_TEST_FRAGMENT);
				if (currentFragment != null) {
					fm.beginTransaction().remove(currentFragment).commit();
					if(defaultFocusButton != null) {
						defaultFocusButton.requestFocus();
					}
				} else {
					showExitConfirmDialog();
				}
			}
		};
		getOnBackPressedDispatcher().addCallback(this, callback);
	}

	private void showExitConfirmDialog() {
		new AlertDialog.Builder(this)
				.setTitle("Exit Application")
				.setMessage("Are you sure you want to exit?")
				.setPositiveButton("YES", (dialog, which) -> finish())
				.setNegativeButton("NO", (dialog, which) -> dialog.dismiss())
				.create()
				.show();
	}

	@Override
	protected void onResume() {
		super.onResume();
		IntentFilter filter = new IntentFilter(UsbDetachReceiver.ACTION_USB_DETACHED_APP);
		ContextCompat.registerReceiver(this, appUsbDetachReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(appUsbDetachReceiver);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (secretCodeManager.onKeyPressed(keyCode)) {
			isFocusHighlightEnabled = !isFocusHighlightEnabled;
			secretCodeManager.showToast(isFocusHighlightEnabled);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void showTestFragment(Fragment testFragment) {
		if (testFragment == null) return;
		mainViewModel.appendLog(TAG, testFragment.getClass().getSimpleName() + " button clicked. Opening fragment...");
		getSupportFragmentManager().beginTransaction()
				.replace(R.id.fragment_container, testFragment, TAG_TEST_FRAGMENT)
				.addToBackStack(null) // Allows returning to the main screen with the back button
				.commit();
	}

	private void checkAndLogBistServiceStatus() {
		final String bistServiceClassName = "com.innopia.bistservice.BISTService";
		boolean isRunning = ServiceUtils.isServiceRunning(this, bistServiceClassName);
		String statusMessage = "BIST Service is " + (isRunning ? "running." : "not running.");
		mainViewModel.appendLog(TAG, statusMessage);
	}

	private void checkAndRequestPermissions() {
		List<String> permissionsToRequest = new ArrayList<>();
		for (String permission : REQUIRED_PERMISSIONS) {
			if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
				permissionsToRequest.add(permission);
			}
		}
		if (!permissionsToRequest.isEmpty()) {
			ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), ALL_PERMISSIONS_REQUEST_CODE);
			Log.d(TAG, "Requesting necessary permissions...");
		} else {
			Log.d(TAG, "All necessary permissions already granted.");
		}
	}

	private void startFactoryReset() {
		Intent intent = new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS);
		try {
			startActivity(intent);
		} catch (ActivityNotFoundException e) {
			Log.e("NavigationError", "Device Info settings activity not found.", e);
			try {
				startActivity(new Intent(Settings.ACTION_SETTINGS));
			} catch (ActivityNotFoundException e2) {
				Log.e("NavigationError", "Main settings activity not found.", e2);
			}
		}
	}

//	private void startFactoryReset() {
//		Intent intent = new Intent(Intent.ACTION_FACTORY_RESET);
//		intent.setPackage("android");
//		intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
//		this.getApplicationContext().sendBroadcastAsUser(intent, new UserHandle(UserHandle.USER_CURRENT));
//	}

	private void startReboot() {
		PowerManager pm = (PowerManager) this.getApplicationContext().getSystemService(Context.POWER_SERVICE);
		pm.reboot("bist");
	}

	private void startSettings() {
		Intent intent = new Intent(Settings.ACTION_SETTINGS);
		if (intent.resolveActivity(this.getApplicationContext().getPackageManager()) != null) {
			this.getApplicationContext().startActivity(intent);
		} else {
			Log.e(TAG, "Settings app cannot be found.");
		}
	}

	public void updateStatusIcon(ImageView imageView, boolean isConnected, int onIconResId, int offIconResId) {
		imageView.setImageDrawable(ContextCompat.getDrawable(this, isConnected ? onIconResId : offIconResId));
	}

	private String getSysInfo() {
		final SystemInfo si = new SystemInfo(this.getApplicationContext());
		final String hwVersion = si.getHwVersion();
		final String swVersion = si.getSwVersion();
		final String appVersion = si.getAppVersion();
		final String modelName = si.getModelName();
		final String serialNumber = si.getSerialNumber();
		final String date = si.getDate();
//		final String cpuTemp = si.getCpuTemp();
		final String cpuTemp = "no permission";
		final String dataPartition = si.getDataPartition();
		final String ethernetMac = si.getEthernetMac();
		final String wifiMac = si.getWifiMac();
		final String btMac = si.getBtMac();
		final String ipAddress = si.getIpAddress();
		StringBuilder sysInfo = new StringBuilder();
		sysInfo.append("SystemInfo:\n");
		sysInfo.append(" HW Ver: ").append(hwVersion).append("\n");
		sysInfo.append(" SW Ver: ").append(swVersion).append("\n");
		sysInfo.append(" App Ver: ").append(appVersion).append("\n");
		sysInfo.append(" Model Name: ").append(modelName).append("\n");
		sysInfo.append(" Serial Number: ").append(serialNumber).append("\n");
		sysInfo.append(" Date: ").append(date).append("\n");
		sysInfo.append(" CPU Temp: " + cpuTemp + "\n");
		sysInfo.append(" Data Partition: ").append(dataPartition).append("\n");
		sysInfo.append(" Ethernet MAC: ").append(ethernetMac).append("\n");
		sysInfo.append(" Wi-Fi MAC: ").append(wifiMac).append("\n");
		sysInfo.append(" BT MAC: ").append(btMac).append("\n");
		sysInfo.append(" IP Addr: ").append(ipAddress).append("\n");
		return sysInfo.toString();
	}

	private String getHwInfo() {
		final String chipId = HwInfo.getChipId();
		final String ddr = HwInfo.getDdr();
		final String emmc = HwInfo.getEmmc();
		final String wifi = HwInfo.getWifi();
		StringBuilder hwInfo = new StringBuilder();
		hwInfo.append("HwInfo:\n");
		hwInfo.append(" Chip ID: ").append(chipId).append("\n");
		hwInfo.append(" DDR: ").append(ddr).append("\n");
		hwInfo.append(" EMMC: ").append(emmc).append("\n");
		hwInfo.append(" Wi-Fi: ").append(wifi);
		return hwInfo.toString();
	}
}
