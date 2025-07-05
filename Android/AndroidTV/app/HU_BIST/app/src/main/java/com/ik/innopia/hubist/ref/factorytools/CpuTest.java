package com.innopia.factorytools;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.NumberFormatException;


public class CpuTest extends Test {
	
	public static final String EXTRA_KEY_CPU_TEMPERATURE = "CPU_TEMPERATURE";

	private final String CMD_GET_CPU_TEMPERATURE = "cat /sys/class/thermal/thermal_zone0/temp";
	//private final String CMD_GET_CPU_TEMPERATURE = "cat /sys/class/hwmon/hwmon0/temp1_input";

	private final String CONFIG_TAG_CPU_TEMPERATURE_MIN = "cpu_temperature_min";
	private final String CONFIG_TAG_CPU_TEMPERATURE_MAX = "cpu_temperature_max";

	private Thread mThread;
	private boolean mIsStop;

	private int mMin;
	private int mMax;



	public CpuTest(Context context, Bundle config) {
		super(context);
		
		if(config != null) {
			try {
				mMin = Integer.parseInt(config.getString(CONFIG_TAG_CPU_TEMPERATURE_MIN));
				mMax = Integer.parseInt(config.getString(CONFIG_TAG_CPU_TEMPERATURE_MAX));
			} catch (NumberFormatException e) {
				mMin = 0;
				mMax = 0;
			}
		}

		mIsStop = false;

		mThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while(!mIsStop) {
					float temperature = getCpuTemperature();

					((Activity)mContext).runOnUiThread(new Runnable() {
						@Override
						public void run() {

							Bundle extra = new Bundle();
							extra.putFloat(EXTRA_KEY_CPU_TEMPERATURE, temperature);
							if(mMin != 0 && mMax != 0 && mMin < temperature && temperature < mMax) {
								mOnResultListener.onResult(CpuTest.this, RESULT_SUCCEEDED, extra);
							} else {
								mOnResultListener.onResult(CpuTest.this, RESULT_FAILED, extra);
							}
						}
					});
				
					try {
						Thread.sleep(1000);	
					} catch (InterruptedException e) {
						e.printStackTrace();
					}	
				}
			}
		});

	}



	@Override
	public void start() {
		if(mThread != null) {
			mThread.start();
		}
	}



	@Override
	public void stop() {
		if(mThread != null && mThread.isAlive()) {
			mIsStop = true;
			mThread.interrupt();
			mThread = null;
		}
	}



	private float getCpuTemperature() {
		Process process;
		float temperature = -1;

		try {
			process = Runtime.getRuntime().exec(CMD_GET_CPU_TEMPERATURE);
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String readLine = bufferedReader.readLine();

			temperature = Float.parseFloat(readLine)/1000.0f;
			bufferedReader.close();

		} catch (IOException e) {

		} finally {
			return temperature;
		}
	}

}
