package com.innopia.bist.test;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.hardware.hdmi.HdmiControlManager;
import android.hardware.hdmi.HdmiDeviceInfo;
import android.os.Build;
import android.view.Display;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * HdmiTest class performs tests on the HDMI connection.
 * It checks for connected HDMI displays, retrieves display information (derived from EDID),
 * and queries HDMI CEC status with proper permission handling.
 */
public class HdmiTest implements Test {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final String TAG = "HdmiTest";

    @Override
    public void runManualTest(Map<String, Object> params, Consumer<String> callback) {
        executor.execute(() -> {
            Context context = (Context) params.get("context");
            if (context == null) {
                callback.accept("Error: Context is null");
                return;
            }
            String result = getHdmiInfo(context);
            callback.accept(result);
        });
    }

    /**
     * Gathers information about the connected HDMI display, including
     * connection status, resolution, HDR capabilities, and CEC information.
     * It includes branching logic to inform the user about permission status.
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
            return "== HDMI Test ==\nResult: FAIL - HDMI display not found.";
        }

        StringBuilder info = new StringBuilder();
        info.append("== HDMI Test ==\n");
        info.append("Status: Connected\n");
        info.append(String.format("Resolution: %dx%d\n", hdmiDisplay.getWidth(), hdmiDisplay.getHeight()));
        info.append(String.format("Refresh Rate: %.2f Hz\n", hdmiDisplay.getRefreshRate()));

        // Get display information derived from EDID (e.g., HDR capabilities)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Display.HdrCapabilities hdrCapabilities = hdmiDisplay.getHdrCapabilities();
            info.append("\n== Display Info (from EDID) ==\n");
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

        // Get HDMI CEC Information with permission check
        info.append("\n== HDMI CEC Info ==\n");
        HdmiControlManager hdmiControlManager = (HdmiControlManager) context.getSystemService(Context.HDMI_CONTROL_SERVICE);

        boolean cecPermissionDenied = false;
        if (hdmiControlManager == null) {
            info.append("CEC Status: Service unavailable\n");
        } else {
            try {
                // This call will throw a SecurityException if the app lacks the signature permission.
                List<HdmiDeviceInfo> devices = hdmiControlManager.getHdmiCecClient().getDeviceList();
                info.append("CEC Status: Supported\n");
                if (devices.isEmpty()) {
                    info.append("- No active CEC devices found.\n");
                } else {
                    for (HdmiDeviceInfo device : devices) {
                        info.append(String.format("- Device: %s, Address: %d\n", device.getDisplayName(), device.getLogicalAddress()));
                    }
                }
            } catch (SecurityException e) {
                // Catch the security exception to provide a clear message.
                cecPermissionDenied = true;
                info.append("CEC Status: FAILED (Permission Denied)\n");
                info.append("Reason: App lacks the required system-level privileges to access HDMI CEC.\n");
            }
        }

        // Final test result depends on whether CEC check failed due to permissions.
        if (cecPermissionDenied) {
            info.append("\nResult: FAIL");
        } else {
            info.append("\nResult: PASS");
        }

        return info.toString();
    }
}
