package com.innopia.factorytools;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import java.util.Timer;
import java.util.TimerTask;

import android.util.Log;

public class BluetoothTest extends Test {

	public static final String EXTRA_KEY_BLUETOOTH_NAME = "BLUETOOTH_NAME";
	public static final String EXTRA_KEY_BLUETOOTH_RSSI = "BLUETOOTH_RSSI";

	private final String CONFIG_TAG_BT_NAME 	= "bluetooth_name";
	private final String CONFIG_TAG_BT_MAC_ADDRESS  = "bluetooth_mac_address";
	private final String CONFIG_TAG_BT_RSSI_MIN 	= "bluetooth_rssi_min";
	private final String CONFIG_TAG_BT_RSSI_MAX 	= "bluetooth_rssi_max";
	private final String CONFIG_TAG_BT_TEST_TIME	= "bluetooth_test_time";

	private final String TAG = "InnoFactory.BluetoothTest";

	private BluetoothAdapter mBluetoothAdapter;
	private BroadcastReceiver mBroadcastReceiver;

	private String mBluetoothName;
	private String mBluetoothMacAddress;
	private int mRssiMin;
	private int mRssiMax;
	private int mTestTime;

	private Timer mTestTimer;
	private Timer mTryDiscoveryTimer;

	private int mPrevResult = RESULT_FAILED;

	
	public BluetoothTest(Context context, Bundle config) {
		super(context);
	
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	
		if(config != null) {
			mBluetoothName = config.getString(CONFIG_TAG_BT_NAME);
			mBluetoothMacAddress = config.getString(CONFIG_TAG_BT_MAC_ADDRESS);
			try {
				mRssiMin = Integer.parseInt(config.getString(CONFIG_TAG_BT_RSSI_MIN));
				mRssiMax = Integer.parseInt(config.getString(CONFIG_TAG_BT_RSSI_MAX));
				mTestTime = Integer.parseInt(config.getString(CONFIG_TAG_BT_TEST_TIME))*1000;
			} catch (NumberFormatException e) {
				mRssiMin = 0;
				mRssiMax = 0;
				mTestTime = 0;
			}

			Log.d(TAG, "BluetoothName :" + mBluetoothName + " BluetoothMacAddress :" +mBluetoothMacAddress);
		}

		mBroadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if(intent.getAction().equals(BluetoothDevice.ACTION_FOUND)) {
					BluetoothDevice bluetoothDevice = (BluetoothDevice)intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);


					String name = bluetoothDevice.getName();
					String address = bluetoothDevice.getAddress();
					short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
					Log.d(TAG, "Found device Name :"+name+" Address :" + address + " (rssi :" + rssi + ")");

					if(mOnResultListener != null) {
						if(mBluetoothName != null) {
							if(name != null && mBluetoothName.equals(name)) {
								sendResult(name, rssi);
							}						
						} else {
							if(name != null && address != null && mBluetoothMacAddress != null && mBluetoothMacAddress.equals(address)) {
								sendResult(name, rssi);
							}
						}
					}
				}
			}	
		};
	}


	@Override
	public void start() {
		setBluetoothState(true);
		try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothDevice.ACTION_FOUND);

		mContext.registerReceiver(mBroadcastReceiver, intentFilter);
		
		if(mBluetoothAdapter.isDiscovering()) {
			mBluetoothAdapter.cancelDiscovery();
			
			if(mTestTimer != null) {
				if(mTryDiscoveryTimer != null) {
					mTryDiscoveryTimer.cancel();	
				}
				mTestTimer.cancel();
			}
		}	
					
	
		mTryDiscoveryTimer = new Timer();

		TimerTask tryDiscoveryTimerTask = new TimerTask() {
			@Override
			public void run() {
				if(mBluetoothAdapter.isDiscovering()) {
					mBluetoothAdapter.cancelDiscovery();
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				Log.d(TAG, "BluetoothTest :: startDiscovery");
				mBluetoothAdapter.startDiscovery();
			}
		};

		mTryDiscoveryTimer.schedule(tryDiscoveryTimerTask, 0, 10000);

		mTestTimer = new Timer();

		TimerTask testTimerTask = new TimerTask(){
			@Override
			public void run() {
				mTryDiscoveryTimer.cancel();
				mBluetoothAdapter.cancelDiscovery();
				mOnResultListener.onResult(BluetoothTest.this, RESULT_FAILED, null);
			}

		};

		mTestTimer.schedule(testTimerTask, mTestTime);
	}



	@Override
	public void stop() {
		if(mBluetoothAdapter.isDiscovering()) {
			mBluetoothAdapter.cancelDiscovery();
			mContext.unregisterReceiver(mBroadcastReceiver);
		}
	}


	
	private void sendResult(String name, short rssi) {
		setBluetoothState(false);

		mTryDiscoveryTimer.cancel();
		mTestTimer.cancel();
		mBluetoothAdapter.cancelDiscovery();
		mContext.unregisterReceiver(mBroadcastReceiver);

		Bundle extra = new Bundle();
		extra.putString(EXTRA_KEY_BLUETOOTH_NAME, name);
		extra.putShort(EXTRA_KEY_BLUETOOTH_RSSI, rssi);

		if(mRssiMin != 0 && mRssiMax != 0 && mRssiMin < rssi && rssi < mRssiMax){
			mPrevResult = RESULT_SUCCEEDED;
			mOnResultListener.onResult(BluetoothTest.this, RESULT_SUCCEEDED, extra);
		} else {
			if(mPrevResult == RESULT_SUCCEEDED) return;
			mOnResultListener.onResult(BluetoothTest.this, RESULT_FAILED, extra);
		}
	}

	private void setBluetoothState(boolean enable) {
		Log.d(TAG, "setBluetoothState() IN (enable=" + enable + ")");

		int state = BluetoothAdapter.getDefaultAdapter().getState();

		Log.d(TAG, "setBluetoothState() " + (enable ? "Enable" : "Disable") + " the Bluetooth! " +
				"(current=" + (state == BluetoothAdapter.STATE_OFF ? "OFF" : "ON") + ")");

		if ( enable ) {
			if ( state != BluetoothAdapter.STATE_ON ) {
				BluetoothAdapter.getDefaultAdapter().enable();
			}
		}
		else {
			if ( state != BluetoothAdapter.STATE_OFF ) {
				BluetoothAdapter.getDefaultAdapter().disable();
			}
		}
	}
}
