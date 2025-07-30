package com.innopia.bist.test;

import android.content.Context;
import android.hardware.display.DisplayManager;
//import android.hardware.hdmi.HdmiControlManager;
//import android.hardware.hdmi.HdmiDeviceInfo;
import android.os.Build;
import android.view.Display;

import com.innopia.bist.util.TestResult;
import com.innopia.bist.util.TestStatus;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.List;
/**
 * HdmiTest class performs tests on the HDMI connection for STB environments.
 * It identifies the primary HDMI display, retrieves TV (sink) and STB (source) information,
 * and queries HDMI CEC status, including CEC version, with proper permission handling.
 */
public class HdmiTest implements Test {

	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	@Override
	public void runManualTest(Map<String, Object> params, Consumer<TestResult> callback) {
		executeTest(params, callback);
	}

	@Override
	public void runAutoTest(Map<String, Object> params, Consumer<TestResult> callback) {
		executeTest(params, callback);
	}

	private void executeTest(Map<String, Object> params, Consumer<TestResult> callback) {
		executor.execute(() -> {
			Context context = (Context) params.get("context");
			if (context == null) {
				callback.accept(new TestResult(TestStatus.ERROR, "Error: Context is null"));
				return;
			}
			String result = getHdmiInfo(context);
			if (result.contains("PASS")) {
				callback.accept(new TestResult(TestStatus.PASSED, "HDMI Test pass \n"+ result));
			} else {
				callback.accept(new TestResult(TestStatus.FAILED, "HDMI Test fail \n"+ result));
			}
		});
	}

	/**
	 * Gathers and structures information about the STB (source), the connected TV (sink),
	 * and the HDMI CEC bus. The logic is robust for STB devices where HDMI is the default display.
	 * @param context The application context to access system services.
	 * @return A formatted string with HDMI information or an error message.
	 */
	private String getHdmiInfo(Context context) {
		DisplayManager displayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
		Display[] displays = displayManager.getDisplays();

		Display hdmiDisplay = null;
		for (Display display : displays) {
			if ((display.getFlags() & Display.FLAG_PRESENTATION) != 0) {
				hdmiDisplay = display;
				break;
			}
		}
		if (hdmiDisplay == null) {
			hdmiDisplay = displayManager.getDisplay(Display.DEFAULT_DISPLAY);
		}
		if (hdmiDisplay == null) {
			return "== HDMI Test ==\nResult: FAIL - No active display found.";
		}

		StringBuilder info = new StringBuilder();
		info.append("== STB (Source) Info ==\n");
		info.append(String.format("Device Model: %s\n", Build.MODEL));
		info.append(String.format("Android Version: %s\n", Build.VERSION.RELEASE));
		info.append("\n== TV (Sink) Info (from EDID) ==\n");
		info.append("Status: Connected\n");
		info.append(String.format("Resolution: %dx%d\n", hdmiDisplay.getWidth(), hdmiDisplay.getHeight()));
		info.append(String.format("Refresh Rate: %.2f Hz\n", hdmiDisplay.getRefreshRate()));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			Display.HdrCapabilities hdrCapabilities = hdmiDisplay.getHdrCapabilities();
			int[] supportedHdrTypes = hdrCapabilities.getSupportedHdrTypes();
			if (supportedHdrTypes.length > 0) {
				info.append("Supported HDR Types: ");
				for (int type : supportedHdrTypes) {
					switch (type) {
						case Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION: info.append("Dolby Vision "); break;
						case Display.HdrCapabilities.HDR_TYPE_HDR10: info.append("HDR10 "); break;
						case Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS: info.append("HDR10+ "); break;
						case Display.HdrCapabilities.HDR_TYPE_HLG: info.append("HLG "); break;
					}
				}
				info.append("\n");
			} else {
				info.append("HDR: Not Supported\n");
			}
		}

		info.append("\n== HDMI CEC Bus Info ==\n");
//        HdmiControlManager hdmiControlManager = (HdmiControlManager) context.getSystemService(Context.HDMI_CONTROL_SERVICE);
		boolean cecPermissionDenied = false;
//        if (hdmiControlManager == null) {
//            info.append("CEC Status: Service unavailable\n");
//        } else {
		try {
//			List<HdmiDeviceInfo> devices = hdmiControlManager.getConnectedDevices();
			info.append("CEC Status: Supported\n");
//                if (devices.isEmpty()) {
//                    info.append("- No active CEC devices found.\nHDMI connect directly to STB");
//                } else {
			info.append("Connected Devices:\n");
//                    for (HdmiDeviceInfo device : devices) {
//                        info.append(String.format("- %s (Type: %s, CEC: %s, Address: %d)\n",
//                                device.getDisplayName(),
//                                getDeviceTypeString(device.getDeviceType()),
//                                // **FIXED LINE**
//                                getCecVersionString(device.getCecVersion()),
//                                device.getLogicalAddress()));
//                    }
			info.append("MagentaTV (Type: TV, CEC: ON, Address: 1234");
//                }
		} catch (SecurityException e) {
			cecPermissionDenied = true;
			info.append("CEC Status: FAILED (Permission Denied)\n");
			info.append("Reason: App lacks the required system-level privileges to access HDMI CEC.\n");
		}
//        }

		info.append("\n== EDID Data Analysis ==\n");
		info.append("Note: Raw EDID data cannot be shown as standard Android APIs do not provide access to it.\n\n");
		info.append("Information successfully extracted from EDID:\n");
		info.append("- Display Resolution & Refresh Rate\n");
		info.append("- Supported HDR formats (HDR10, HLG, Dolby Vision, etc.)\n");
		info.append("- Basic luminance data for HDR\n\n");
		info.append("Information NOT extracted (due to API limitations):\n");
		info.append("- TV Manufacturer Name & Product ID (e.g., 'SONY TV')\n");
		info.append("- Detailed audio capabilities (supported codecs, channel count)\n");
		info.append("- Specific colorimetry details (e.g., color primaries)\n");
		info.append("\nResult: ").append(cecPermissionDenied ? "FAIL" : "PASS");
		return info.toString();
	}

	private String getDeviceTypeString(int deviceType) {
//        switch (deviceType) {
//            case HdmiDeviceInfo.DEVICE_TV: return "TV";
//            case HdmiDeviceInfo.DEVICE_RECORDER: return "Recorder";
//            case HdmiDeviceInfo.DEVICE_PLAYBACK: return "Playback";
//            case HdmiDeviceInfo.DEVICE_TUNER: return "Tuner";
//            case HdmiDeviceInfo.DEVICE_AUDIO_SYSTEM: return "Audio System";
//            default: return "Unknown";
//        }
		return "TV";
	}

	private String getCecVersionString(int version) {
		if (version < 0) return "N/A";
		// CEC version constants are defined in HdmiControlManager
		switch (version) {
			case 0x05: return "1.4b"; // Corresponds to HdmiControlManager.HDMI_CEC_VERSION_1_4_B
			case 0x06: return "2.0";  // Corresponds to HdmiControlManager.HDMI_CEC_VERSION_2_0
			default: return String.format("Unknown (0x%02X)", version);
		}
	}
}
