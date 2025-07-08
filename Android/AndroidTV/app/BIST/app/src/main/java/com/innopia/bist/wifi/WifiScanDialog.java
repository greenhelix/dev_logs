package com.innopia.bist.wifi;

import android.app.DialogFragment;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.net.wifi.ScanResult;
import android.view.Window;
import android.view.WindowManager;

import com.innopia.bist.MainActivity;
import com.innopia.bist.R;

import java.util.ArrayList;
import java.util.List;

//public class WifiScanDialog extends DialogFragment {
public class WifiScanDialog extends DialogFragment implements WifiScanAdapter.OnConnectClickListener {

    private static final String TAG = "BIST_WIFI_DIALOG";
    private static final String ARG_WIFI_LIST = "wifi_list";

    private List<ScanResult> wifiList = new ArrayList<>();
    private RecyclerView recyclerView;

    public interface WifiConnectionListener {
        void onConnectAttempt(ScanResult scanResult, String password);
    }
    private WifiConnectionListener mListener;

    private void logToMainActivity(String message) {
        if (getActivity() instanceof MainActivity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ((MainActivity) getActivity()).appendToLog(TAG + ": " + message);
            }
        }
    }

    public static WifiScanDialog newInstance(List<ScanResult> wifiList) {
        WifiScanDialog dialog = new WifiScanDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_WIFI_LIST, new ArrayList<>(wifiList));
        dialog.setArguments(args);
        return dialog;
    }

    public void setWifiConnectionListener(WifiConnectionListener listener) {
        this.mListener = listener;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.dialog_wifi_scan, container, false);
        Log.d(TAG, "onCreateView: Dialog view is being created.");
        logToMainActivity("Wi-Fi scan dialog opened.");

        if (getArguments() != null) {
            wifiList = (List<ScanResult>) getArguments().getSerializable(ARG_WIFI_LIST);
        }

        recyclerView = rootView.findViewById(R.id.recycler_wifi_scan);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        WifiScanAdapter adapter = new WifiScanAdapter(wifiList);
        adapter.setOnConnectClickListener(this);
        recyclerView.setAdapter(adapter);

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = getDialog().getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.END;
            params.width = getResources().getDisplayMetrics().widthPixels / 4;
            params.height = WindowManager.LayoutParams.MATCH_PARENT;
            window.setAttributes(params);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView.post(() -> {
            if (recyclerView.getAdapter() != null && recyclerView.getAdapter().getItemCount() > 0) {
                View firstItem = recyclerView.getLayoutManager().findViewByPosition(0);
                if (firstItem != null) {
                    firstItem.requestFocus();
                }
            }
        });
    }

    @Override
    public void onConnectClick(ScanResult scanResult, String password) {
        if (mListener != null) {
            Log.d(TAG, "Connect button clicked in dialog for SSID: " + scanResult.SSID);
            logToMainActivity("Connect button clicked in dialog for " + scanResult.SSID);
            mListener.onConnectAttempt(scanResult, password);
        } else {
            Log.e(TAG, "onConnectClick: WifiConnectionListener (mListener) is null.");
            logToMainActivity("Error: Listener not set, cannot process connection.");
        }
        dismiss();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}
