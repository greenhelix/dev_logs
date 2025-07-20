package com.innopia.bist.test;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.hdmi.HdmiControlManager;
import android.hardware.hdmi.HdmiDeviceInfo;
import android.os.Build;
import android.view.Display;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * HdmiTest class performs tests on the HDMI connection.
 * It checks for connected HDMI displays, retrieves display information (derived from EDID),
 * and queries HDMI CEC status.
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
     * @param context The application context to access system services.
     * @return A formatted string with HDMI information or an error message.
     */
    private String getHdmiInfo(Context context) {
        DisplayManager displayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        Display[] displays = displayManager.getDisplays();

        // Find an external presentation display, which is likely the HDMI output.
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

        // Get HDMI CEC Information
        info.append("\n== HDMI CEC Info ==\n");
        HdmiControlManager hdmiControlManager = (HdmiControlManager) context.getSystemService(Context.HDMI_CONTROL_SERVICE);
        if (hdmiControlManager != null && hdmiControlManager.getHdmiCecClient() != null) {
            info.append("CEC Status: Supported\n");
            // Requires HDMI_CEC permission
            try {
                for (HdmiDeviceInfo device : hdmiControlManager.getHdmiCecClient().getDeviceList()) {
                    info.append(String.format("- Device: %s, Address: %d\n", device.getDisplayName(), device.getLogicalAddress()));
                }
            } catch (SecurityException e) {
                info.append("CEC Info: Permission denied. Add android.permission.HDMI_CEC to manifest.\n");
            }
        } else {
            info.append("CEC Status: Not Supported or Service unavailable\n");
        }

        // Final test result
        info.append("\nResult: PASS");

        return info.toString();
    }
}
