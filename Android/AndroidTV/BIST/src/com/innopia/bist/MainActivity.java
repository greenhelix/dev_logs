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
import com.innopia.bist.fragment.ButtonTestFragment;
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
import com.innopia.bist.util.FileUtils;
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
	private View lastFocusedViewBeforeFragment;

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

	private AlertDialog mAutoTestInProgressDialog = null;

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
		checkConfigAndStartAutoTests();
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

		Button btnEthernet = findViewById(R.id.button_ethernet);
		btnEthernet.setOnClickListener(v -> {
			showTestFragment(EthernetTestFragment.newInstance());
			defaultFocusButton = btnEthernet;
		});
		mainTestButtons.add(btnEthernet);
		testButtonMap.put(TestType.ETHERNET, btnEthernet);
		defaultFocusButton = btnEthernet;

		Button btnWifi = findViewById(R.id.button_wifi);
		btnWifi.setOnClickListener(v -> {
			showTestFragment(WifiTestFragment.newInstance());
			defaultFocusButton = btnWifi;
		});
		mainTestButtons.add(btnWifi);
		testButtonMap.put(TestType.WIFI, btnWifi);

		Button btnBt = findViewById(R.id.button_bt);
		btnBt.setOnClickListener(v -> {
			showTestFragment(BluetoothTestFragment.newInstance());
			defaultFocusButton = btnBt;
		});
		mainTestButtons.add(btnBt);
		testButtonMap.put(TestType.BLUETOOTH, btnBt);

		Button btnUsb = findViewById(R.id.button_usb);
		btnUsb.setOnClickListener(v -> {
			showTestFragment(UsbTestFragment.newInstance());
			defaultFocusButton = btnUsb;
		});
		mainTestButtons.add(btnUsb);
		testButtonMap.put(TestType.USB, btnUsb);

		Button btnHdmi = findViewById(R.id.button_hdmi);
		btnHdmi.setOnClickListener(v -> {
			showTestFragment(HdmiTestFragment.newInstance());
			defaultFocusButton = btnHdmi;
		});
		mainTestButtons.add(btnHdmi);
		testButtonMap.put(TestType.HDMI, btnHdmi);

		Button btnCpu = findViewById(R.id.button_cpu);
		btnCpu.setOnClickListener(v -> {
			showTestFragment(CpuTestFragment.newInstance());
			defaultFocusButton = btnCpu;
		});
		mainTestButtons.add(btnCpu);
		testButtonMap.put(TestType.CPU, btnCpu);

		Button btnMemory = findViewById(R.id.button_memory);
		btnMemory.setOnClickListener(v -> {
			showTestFragment(MemoryTestFragment.newInstance());
			defaultFocusButton = btnMemory;
		});
		mainTestButtons.add(btnMemory);
		testButtonMap.put(TestType.MEMORY, btnMemory);

		Button btnVideo = findViewById(R.id.button_video);
		btnVideo.setOnClickListener(v -> {
			updateButtonUI(TestType.VIDEO, TestStatus.RUNNING);
			showTestFragment(VideoTestFragment.newInstance());
			defaultFocusButton = btnVideo;
		});
		mainTestButtons.add(btnVideo);
		testButtonMap.put(TestType.VIDEO, btnVideo);

		Button btnRcu = findViewById(R.id.button_rcu);
		btnRcu.setOnClickListener(v -> {
			showTestFragment(RcuTestFragment.newInstance());
			defaultFocusButton = btnRcu;
		});
		mainTestButtons.add(btnRcu);
		testButtonMap.put(TestType.RCU, btnRcu);

		Button btnButton = findViewById(R.id.button_button);
		btnButton.setOnClickListener(v -> {
			showTestFragment(ButtonTestFragment.newInstance());
			defaultFocusButton = btnButton;
		});
		mainTestButtons.add(btnButton);
		testButtonMap.put(TestType.BUTTON, btnButton);

		Button btnFactoryReset = findViewById(R.id.button_factory_reset);
		btnFactoryReset.setOnClickListener(v -> startFactoryReset());
		mainTestButtons.add(btnFactoryReset);

		Button btnReboot = findViewById(R.id.button_reboot);
		btnReboot.setOnClickListener(v -> startReboot());
		mainTestButtons.add(btnReboot);

		Button btnSettings = findViewById(R.id.button_settings);
		btnSettings.setOnClickListener(v -> startSettings());
		mainTestButtons.add(btnSettings);

		Button btnAutoTest = findViewById(R.id.button_auto_test);
		btnAutoTest.setOnClickListener(v -> {
			// mainViewModel.startAutoTest("/storage/usb_storage");
			//mainViewModel.startAutoTest(true); //for testing
			checkConfigAndStartAutoTests();
		});

		Button btnClearResults = findViewById(R.id.button_clear_results);
		btnClearResults.setOnClickListener(v -> {
			mainViewModel.clearResults();
			defaultFocusButton = btnClearResults;
		});

		mainTestButtons.add(btnClearResults);

		mainViewModel.sysInfoLiveData.observe(this, sysInfoText::setText);
		mainViewModel.hwInfoLiveData.observe(this, hwInfoText::setText);
	}

	private void setupObservers() {
		mainViewModel.logLiveData.observe(this, logs -> {
			View view = getCurrentFocus();
			List<String> logsCopy = new ArrayList<>(logs);
			tvLogWindow.setText(String.join("\n", logsCopy));
			svLog.fullScroll(ScrollView.FOCUS_DOWN);
			if (view != null) {
				view.requestFocus();
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
			if (isRunning == false) {
				// remove dialog
				if (mAutoTestInProgressDialog != null) {
					mAutoTestInProgressDialog.dismiss();
				}
			}
		});

		// Observer for individual test statuses to update button colors.
		mainViewModel.testStatusesLiveData.observe(this, statuses -> {
			if (statuses == null) return;
			for (Map.Entry<TestType, TestStatus> entry : statuses.entrySet()) {
				updateButtonUI(entry.getKey(), entry.getValue());
			}
			defaultFocusButton.requestFocus();
		});

		// Observer for user action requests to show dialogs during auto-test.
		mainViewModel.userActionRequired.observe(this, message -> {
			if (message != null && !message.isEmpty()) {
				showUserActionDialog(message);
			}
		});
		mainViewModel.navigateToTestFragment.observe(this, testType -> {
			if (testType != null) {
				if (testType == TestType.VIDEO && mAutoTestInProgressDialog != null && mAutoTestInProgressDialog.isShowing()) {
					mAutoTestInProgressDialog.dismiss();
				}
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
				.commit();
	}

	private void clearFragmentContainer() {
		FragmentManager fm = getSupportFragmentManager();
		Fragment currentFragment = fm.findFragmentByTag(TAG_TEST_FRAGMENT);
		if (currentFragment != null) {
			mainViewModel.appendLog(TAG, "Auto-test finished. Clearing fragment container.");
			fm.beginTransaction().remove(currentFragment).commit();
			//if(defaultFocusButton != null) {
			//	defaultFocusButton.requestFocus();
			//}
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
		GradientDrawable defaultDrawable = new GradientDrawable();
		defaultDrawable.setShape(GradientDrawable.RECTANGLE);
		defaultDrawable.setCornerRadius(4 * getResources().getDisplayMetrics().density);
		defaultDrawable.setColor(backgroundColor);
		defaultDrawable.setStroke((int) (3 * getResources().getDisplayMetrics().density), Color.BLACK);
		Drawable[] focusLayers = new Drawable[2];
		focusLayers[0] = defaultDrawable;
		focusLayers[1] = ContextCompat.getDrawable(this, R.drawable.button_state_selector);
		LayerDrawable focusDrawable = new LayerDrawable(focusLayers);
		StateListDrawable stateListDrawable = new StateListDrawable();
		stateListDrawable.addState(new int[]{android.R.attr.state_focused}, focusDrawable);
		stateListDrawable.addState(new int[]{}, defaultDrawable);
		button.setBackground(stateListDrawable);
		//if (status == TestStatus.PASSED || status == TestStatus.FAILED) {
		//	button.post(() -> button.requestFocus());
		//}
	}

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

			dialog.setOnShowListener(dialogInterface -> btnYes.requestFocus());

		} else {
			yesNoLayout.setVisibility(View.GONE);
			btnOk.setVisibility(View.VISIBLE);

			btnOk.setOnClickListener(v -> {
				mainViewModel.userActionConfirmed(true);
				dialog.dismiss();
			});

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
					fm.beginTransaction().remove(currentFragment).commitNow();
					if (lastFocusedViewBeforeFragment != null) {
						lastFocusedViewBeforeFragment.requestFocus();
					}
					return;
				}

				if (getCurrentFocus() != defaultFocusButton) {
					if (defaultFocusButton != null) {
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
		LayoutInflater inflater = LayoutInflater.from(this);
		View dialogView = inflater.inflate(R.layout.dialog_custom_action, null);

		TextView tvMessage = dialogView.findViewById(R.id.dialog_message);
		LinearLayout yesNoLayout = dialogView.findViewById(R.id.dialog_yes_no_layout);
		Button btnYes = dialogView.findViewById(R.id.dialog_button_yes);
		Button btnNo = dialogView.findViewById(R.id.dialog_button_no);
		Button btnOk = dialogView.findViewById(R.id.dialog_button_ok);

		AlertDialog.Builder builder = new AlertDialog.Builder(this)
			.setView(dialogView)
			.setTitle("Exit Application")
			.setCancelable(false);

		final AlertDialog dialog = builder.create();

		tvMessage.setText("Are you sure you want to exit?");
		yesNoLayout.setVisibility(View.VISIBLE);
		btnOk.setVisibility(View.GONE);

		btnYes.setOnClickListener(v -> {
			dialog.dismiss();
			finish();
		});
		btnNo.setOnClickListener(v -> dialog.dismiss());

		dialog.setOnShowListener(dialogInterface -> btnYes.requestFocus());
		dialog.show();
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
		lastFocusedViewBeforeFragment = getCurrentFocus();
		mainViewModel.appendLog(TAG, testFragment.getClass().getSimpleName() + " button clicked. Opening fragment...");
		getSupportFragmentManager().beginTransaction()
				.replace(R.id.fragment_container, testFragment, TAG_TEST_FRAGMENT)
				.commit();
	}

	private void checkAndLogBistServiceStatus() {
		final String bistServiceClassName = "com.innopia.bistservice.BISTService";
		boolean isRunning = ServiceUtils.isServiceRunning(this, bistServiceClassName);
		String statusMessage = "BIST Service is " + (isRunning ? "running." : "not running.");
		mainViewModel.appendLog(TAG, statusMessage);
	}

	private void checkConfigAndStartAutoTests() {
		// check whether config exists
		Bundle config = FileUtils.getConfigValues(this.getApplicationContext(), "config.xml");
		// if it exists, start auto tests
		if (config == null) {
			mainViewModel.appendLog(TAG, "config not found");
			return;
		}
		mainViewModel.startAutoTest(config);

		// need to show a dialog saying running auto tests
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this)
				.setTitle("Auto Test")
				.setMessage("Auto Test In Progress...")
				.setCancelable(false);
		mAutoTestInProgressDialog = dialogBuilder.create();
		mAutoTestInProgressDialog.show();

		// TODO: able to be cancelable with back key
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
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this)
				.setTitle("Factory Reset")
				.setMessage("Start Factory Reset?")
				.setPositiveButton("Yes", (dialog, which) -> {
					Intent intent = new Intent(Intent.ACTION_FACTORY_RESET);
					intent.setPackage("android");
					intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
					this.getApplicationContext().sendBroadcastAsUser(intent, new UserHandle(UserHandle.USER_CURRENT));
				})
				.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
		AlertDialog dialog = dialogBuilder.create();
		dialog.show();
		dialog.getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus();
	}

	private void startReboot() {
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this)
				.setTitle("Reboot")
				.setMessage("Start Reboot?")
				.setPositiveButton("Yes", (dialog, which) -> {
					 PowerManager pm = (PowerManager) this.getApplicationContext().getSystemService(Context.POWER_SERVICE);
					 pm.reboot("bist");
				})
				.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
		AlertDialog dialog = dialogBuilder.create();
		dialog.show();
		dialog.getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus();
	}

	private void startSettings() {
		Intent intent = new Intent();
		intent.setClassName("com.android.tv.settings", "com.android.tv.settings.MainSettings");
		startActivity(intent);
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
		final int cpuTemp = si.getCpuTemp();
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
		sysInfo.append(" CPU Temp: ").append(cpuTemp).append("°C\n");
		sysInfo.append(" Data Partition: ").append(dataPartition).append("\n");
		sysInfo.append(" Ethernet MAC: ").append(ethernetMac).append("\n");
		sysInfo.append(" Wi-Fi MAC: ").append(wifiMac).append("\n");
		sysInfo.append(" BT MAC: ").append(btMac).append("\n");
		sysInfo.append(" IP Addr: ").append(ipAddress).append("\n");
		return sysInfo.toString();
	}

	private String getHwInfo() {
		final String cpu = HwInfo.getCpu();
		final String ddr = HwInfo.getDdr();
		final String emmc = HwInfo.getEmmc();
		final String wifi = HwInfo.getWifi();

		StringBuilder hwInfo = new StringBuilder();
		hwInfo.append("HwInfo:\n");
		hwInfo.append(" CPU: ").append(cpu).append("\n");
		hwInfo.append(" DDR: ").append(ddr).append("\n");
		hwInfo.append(" EMMC: ").append(emmc).append("\n");
		hwInfo.append(" Wi-Fi: ").append(wifi);
		return hwInfo.toString();
	}
}
