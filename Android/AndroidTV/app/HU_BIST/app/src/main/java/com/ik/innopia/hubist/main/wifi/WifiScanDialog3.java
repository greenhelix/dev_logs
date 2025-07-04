package com.ik.innopia.hubist.main.wifi;

import android.app.DialogFragment;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ik.innopia.hubist.R;

import android.net.wifi.ScanResult;
import android.view.Window;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;

//public class WifiScanDialog extends DialogFragment {
public class WifiScanDialog3 extends DialogFragment implements WifiScanAdapter3.OnConnectClickListener {

    private static final String TAG = "BIST";
    private static final String ARG_WIFI_LIST = "wifi_list";

    private List<ScanResult> wifiList = new ArrayList<>();
    private RecyclerView recyclerView;

    public interface WifiConnectionListener {
        void onConnectAttempt(ScanResult scanResult, String password);
    }
    private WifiConnectionListener mListener;

    public static WifiScanDialog3 newInstance(List<ScanResult> wifiList) {
        WifiScanDialog3 dialog = new WifiScanDialog3();
        Bundle args = new Bundle();
        args.putSerializable(ARG_WIFI_LIST, new ArrayList<>(wifiList));
        dialog.setArguments(args);
        return dialog;
    }

    public void setWifiConnectionListener(WifiConnectionListener listener) {
        this.mListener = listener;
        Log.d(TAG, "[Dialog] WifiConnectionListener가 명시적으로 설정되었습니다.");
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.dialog_wifi_scan3, container, false);

        if (getArguments() != null) {
            wifiList = (List<ScanResult>) getArguments().getSerializable(ARG_WIFI_LIST);
        }

        recyclerView = rootView.findViewById(R.id.recycler_wifi_scan);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        WifiScanAdapter3 adapter = new WifiScanAdapter3(wifiList);
        adapter.setOnConnectClickListener(this);
        recyclerView.setAdapter(adapter);

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = getDialog().getWindow();
        if (window != null) {
            // 사이드 패널처럼 보이게 하는 핵심 코드
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.END; // 화면 오른쪽에 붙이기
            params.width = getResources().getDisplayMetrics().widthPixels / 4; // 화면 너비의 절반 크기
            params.height = WindowManager.LayoutParams.MATCH_PARENT; // 높이는 꽉 채우기
            window.setAttributes(params);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // 기본 배경 제거
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
        if(mListener != null) {
            Log.d(TAG, "[Dialog] Adapter로부터 연결 요청 수신. Fragment로 전달합니다.");
            mListener.onConnectAttempt(scanResult, password);
        } else {
            Log.e(TAG, "[Dialog] mListener가 null입니다. Fragment로 전달할 수 없습니다.");
        }
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
