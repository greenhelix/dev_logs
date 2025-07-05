package com.innopia.factorytools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.System;
import java.util.Timer;
import java.util.TimerTask;

// HDMI CEC
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.hardware.tv.hdmi.cec.CecMessage;
import android.hardware.tv.hdmi.cec.IHdmiCec;
import android.hardware.tv.hdmi.cec.IHdmiCecCallback;

public class HdmiTest extends Test {

	private final String TAG = "InnoFactory.HdmiTest";
	
	public static final String EXTRA_KEY_HDMI_EDID 	= "HDMI_EDID";
	public static final String EXTRA_KEY_HDMI_CEC 	= "HDMI_CEC";

	public static final String RESOLUTION_1080P 	= "1920X1080P";
	public static final String RESOLUTION_720P 	= "1280X720P";
	public static final String RESOLUTION_UNKNOWN 	= "UNKNOWN"; 

	private boolean mIsStarted = false;

	private BroadcastReceiver mBroadcastReceiver;
	private IntentFilter mIntentFilter;

	private Timer mCheckTimer;
	private IHdmiCec mHdmiCec;

	public HdmiTest(Context context) {
		super(context);
		
		mBroadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if(intent.getAction().equals("android.intent.action.HDMI_PLUGGED")) {
					boolean state = intent.getBooleanExtra("state", false);
					Log.d(TAG,"onReceive android.intent.action.HDMI_PLUGGED - " + state);
					if(state) {
						if(mIsStarted) {
							try{
								Thread.sleep(1000);
							} catch(InterruptedException e) {
								e.printStackTrace();
							}
							startHdmiCec();
							mIsStarted = false;
						}

						if(mCheckTimer != null) {
							mCheckTimer.cancel();
							mCheckTimer = null;
						}

						checkManufacturerId();
						checkCecVersion();
		
					}
				}
			}
		};

		mIntentFilter= new IntentFilter();
		mIntentFilter.addAction("android.intent.action.HDMI_PLUGGED");
	}


	@Override
	public void start() {
		mIsStarted = true;
		mContext.registerReceiver(mBroadcastReceiver, mIntentFilter);
	}



	@Override
	public void stop() {
		stopHdmiCec();
		mIsStarted = false;
		mContext.unregisterReceiver(mBroadcastReceiver);
	}

	private void onReceiveCecVersion(String version) {
		Log.d(TAG,"onReceiveCecVersion : " + version);
		if(mCheckTimer!= null) {
			mCheckTimer.cancel();
			mCheckTimer = null;
		}
		stopHdmiCec();
		Bundle extra = new Bundle();
		extra.putString(EXTRA_KEY_HDMI_CEC, version);
		mOnResultListener.onResult(HdmiTest.this, RESULT_SUCCEEDED, extra);
	}

	private void checkCecVersion() {
		sendCecMessage();
	}

	private void retryCheckManufacturerId() {
		if ( mCheckTimer != null ) {
			mCheckTimer.cancel();
			mCheckTimer = null;
		}

		mCheckTimer = new Timer();
		mCheckTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				Log.d(TAG, "retry EDID Check");
				checkManufacturerId();
			}
		}, 2000);
	}

	private void checkManufacturerId() {
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				String manufacturerId = null;
				try {

					Process process = Runtime.getRuntime().exec("mdb disp hdmi_getcapbility");
					BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
					String line = bufferedReader.readLine();

					while(line != null) {
						if(line.startsWith("Manufacturer Name: ")) {
							manufacturerId = line.replace("Manufacturer Name: ", "");		
							break;
						}
						line = bufferedReader.readLine();
					}

					bufferedReader.close();

				} catch (IOException e) {
					manufacturerId = null;
				} finally {
					Bundle extra = new Bundle();
					Log.d(TAG, "EDID - Manufacturer Name : " + manufacturerId);
					if ( manufacturerId == null || manufacturerId.trim().isEmpty() ) {
						extra.putString(EXTRA_KEY_HDMI_EDID, "");
						mOnResultListener.onResult(HdmiTest.this, RESULT_FAILED, extra);
						retryCheckManufacturerId();
					} else {
						extra.putString(EXTRA_KEY_HDMI_EDID, manufacturerId);
						mOnResultListener.onResult(HdmiTest.this, RESULT_SUCCEEDED, extra);
					}
				}	
			}	
		}).start();
	}	






	private void startHdmiCec() 
	{
		Log.d(TAG, "startHdmiCec");
		mHdmiCec = IHdmiCec.Stub.asInterface(
				ServiceManager.getService(IHdmiCec.DESCRIPTOR + "/default"));
		if ( mHdmiCec == null ) {
			Log.d(TAG, "Could not initialize HDMI CEC AIDL HAL");
			return ;
		}

		try {
			mHdmiCec.setCallback(new HdmiCecCallback());
		} catch ( RemoteException e ) { 
			Log.d(TAG, "Failed to set Callback : " + e.toString());
		}
	}

	private void stopHdmiCec()
	{
		Log.d(TAG, "stopHdmiCec");
		try {
			mHdmiCec.setCallback(null);
			mHdmiCec = null;
		} catch ( RemoteException e ) { 
			Log.d(TAG, "Failed to set Callback : " + e.toString());
		}
	}

	private void sendCecMessage()
	{
		Log.d(TAG, "sendCecMessage");
		if ( mHdmiCec == null ) {
            startHdmiCec();
            return ;
        }

        try {
            byte[] tmp = new byte[] {(byte)0x9F, (byte)0};
            CecMessage message = new CecMessage();
            message.initiator = (byte) (4&0xF);
            message.destination = (byte) (0&0xF);
            message.body = tmp;

            mHdmiCec.sendMessage(message);
        } catch ( RemoteException e ) {
            Log.d(TAG, "Failed to send CEC message : " + e.toString());
        }
	}

	class HdmiCecCallback extends IHdmiCecCallback.Stub {

        @Override
        public void onCecMessage(CecMessage message) throws RemoteException {
            Log.d(TAG, "onCecMessage");
            dump(message.initiator, message.destination, message.body);
        }

        @Override
        public synchronized String getInterfaceHash() throws RemoteException {
            Log.d(TAG, "getInterfaceHash");
            return IHdmiCecCallback.Stub.HASH;
        }

        @Override
        public int getInterfaceVersion() throws RemoteException {
            Log.d(TAG, "getInterfaceVersion");
            return IHdmiCecCallback.Stub.VERSION;
        }

        private void dump(int src, int dest, byte[] body) {
            Log.d(TAG, "CEC Message : " + src + " -> " + dest);
            if ( body == null || body.length <= 0 ) {
                Log.d(TAG, "CEC Message empty");
                return ;
            }

            StringBuilder s = new StringBuilder();
            s.append("CEC Message : ");
            for ( byte data : body ) {
                s.append(String.format(":%02X", data));
            }

			if ( body[0] == (byte)0x9E ) {
				onReceiveCecVersion(String.format("%X", body[1]));
			}

            Log.d(TAG, s.toString());
        }
    }
}
