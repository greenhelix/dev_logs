package com.innopia.factorytools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;


public class UsbTest extends Test {

	private final String TAG = "InnoFactory.UsbTest";

	public static final String EXTRA_KEY_USB_NAME = "USB_NAME";

	public static final String USB_1 = "1-1";
	public static final String USB_2 = "2-1";
	public static final String USB_3 = "3-1";

	private final String USB_DIRECTORY_PATH = "/sys/bus/usb/devices/";
	
	private BroadcastReceiver mBroadcastReceiver;
	private IntentFilter mIntentFilter;

	private ArrayList<String> mUsbDirectoryNameList;



	public UsbTest(Context context) {
		super(context);
		
		mUsbDirectoryNameList = new ArrayList<String>();
		mUsbDirectoryNameList.add(USB_1);
		mUsbDirectoryNameList.add(USB_2);
		mUsbDirectoryNameList.add(USB_3);
	
		mBroadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();

				if(action.equals(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)
						|| action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
					Log.d(TAG,"onReceive action : " + action);
					checkDirectory();
				}
			}
		};

		mIntentFilter = new IntentFilter();
		mIntentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		mIntentFilter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
	}



	@Override
	public void start() {
		mContext.registerReceiver(mBroadcastReceiver, mIntentFilter);

		checkDirectory();
	}



	@Override
	public void stop() {
		try {
			mContext.unregisterReceiver(mBroadcastReceiver);
		 } catch(IllegalArgumentException e) {
		     e.printStackTrace();
		}
	}

	public void checkDirectory() {
		Bundle bundle = new Bundle();
		for ( String directoryName : mUsbDirectoryNameList ) {
			File directory = new File(USB_DIRECTORY_PATH+directoryName);
			if ( directory.exists() && directory.isDirectory() ) { 
				bundle.putString(EXTRA_KEY_USB_NAME, directoryName);
				mOnResultListener.onResult(UsbTest.this, RESULT_SUCCEEDED, bundle);
				return ;
			}
		}

		bundle.putString(EXTRA_KEY_USB_NAME, USB_1);
		mOnResultListener.onResult(UsbTest.this, RESULT_FAILED, bundle);
	}

}
