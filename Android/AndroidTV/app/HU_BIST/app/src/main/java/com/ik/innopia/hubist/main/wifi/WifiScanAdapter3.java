package com.ik.innopia.hubist.main.wifi;

import android.net.wifi.ScanResult;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.ik.innopia.hubist.R;

import java.util.List;

//public class WifiScanAdapter extends RecyclerView.Adapter<WifiScanAdapter3.ViewHolder> {
public class WifiScanAdapter3 extends RecyclerView.Adapter<WifiScanAdapter3.ViewHolder> {
    private static final String TAG = "BIST";

    private List<ScanResult> wifiList;
    private int expandedPosition = -1;
    private OnConnectClickListener connectClickListener;

    public WifiScanAdapter3(List<ScanResult> wifiList) {
        this.wifiList = wifiList;
    }

    public interface OnConnectClickListener {
        void onConnectClick(ScanResult scanResult, String password);
    }

    public void setOnConnectClickListener(OnConnectClickListener listener) {
        this.connectClickListener = listener;
    }

    @Override
    public WifiScanAdapter3.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wifi_scan3, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(WifiScanAdapter3.ViewHolder holder, int position) {
        ScanResult scanResult = wifiList.get(position);
        if(!scanResult.SSID.isEmpty()){
            holder.ssidText.setText(scanResult.SSID);
        }
//        holder.bssidText.setText(scanResult.BSSID);
//        holder.levelText.setText(String.valueOf(scanResult.level));

        final boolean isExpanded = position == expandedPosition;

        // Add this block to request focus when the layout expands
        if (isExpanded) {
            holder.passwordEdit.requestFocus();
            // Optionally, show the keyboard as well
            // InputMethodManager imm = (InputMethodManager) holder.itemView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            // imm.showSoftInput(holder.passwordEdit, InputMethodManager.SHOW_IMPLICIT);
        }

        holder.expandableLayout.setVisibility(isExpanded? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            int previousExpandedPositon = expandedPosition;
            expandedPosition = isExpanded ? -1 : holder.getAdapterPosition();

            if(previousExpandedPositon != -1) {
                notifyItemChanged(previousExpandedPositon);
            }
            if (expandedPosition != -1) {
                notifyItemChanged(expandedPosition);
            }
            holder.itemView.setFocusable(false);
        });

        holder.connectButton.setOnClickListener(v -> {
            String password = holder.passwordEdit.getText().toString();
            Log.d(TAG, "Button Click - PW: "+password);
            if (connectClickListener != null) {
            Log.d(TAG, "[Adapter] 연결 버튼 클릭됨. SSID: " + scanResult.SSID + ", PW: " + password);
            connectClickListener.onConnectClick(scanResult, password);
        } else {
            Log.e(TAG, "[Adapter] OnConnectClickListener가 설정되지 않았습니다.");
        }
    });
    }

    @Override
    public int getItemCount() {
        return wifiList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView ssidText, bssidText, levelText;
        LinearLayout expandableLayout;
        EditText passwordEdit;
        Button connectButton;

        public ViewHolder(View itemView) {
            super(itemView);
            ssidText = itemView.findViewById(R.id.text_ssid);
//            bssidText = itemView.findViewById(R.id.text_bssid);
//            levelText = itemView.findViewById(R.id.text_level);
            expandableLayout = itemView.findViewById(R.id.expandable_layout);
            passwordEdit = itemView.findViewById(R.id.edit_password);
            connectButton = itemView.findViewById(R.id.btn_connect);
        }
    }
}
