package com.innopia.factorytools;

import android.content.Context;
import android.os.SystemProperties;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

public class Provision extends Test {
	private static final String TAG = "InnoFactory.PROVISION";

	public static final String PROVISION_STEP		= "provision_step";

	public static final int PROVISION_STEP_CSR	= 1;
	public static final int PROVISION_STEP_APP_KEY	= 2;
	public static final int PROVISION_STEP_EMMC_OTP	= 3;

	public static final String EXTRA_KEY_CSR_DATA = "csr_data";
	public static final String CMD_MAKE_CSR = "rkp_factory_extraction_tool --self_test --output_format build+csr > /vendor/factory/csr.json";
	public static final String CMD_READ_CSR = "cat /vendor/factory/csr.json";
	public static final String CSR_DATA = "/vendor/factory/csr.json";

	private Context mContext;

	private int mProvisionStep = 0;

	public Provision(Context context) {
		super(context);
		mContext = context;
	}

	@Override
	public void start() {}

	@Override
	public void stop() {}

	public Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			checkResult();
		}
	};

	public int getProvisionStep(){
		return mProvisionStep;
	}

	public void setProvisionStep(int key) {
		mProvisionStep = key;

		switch( mProvisionStep ) {
			case PROVISION_STEP_CSR:
				Log.d(TAG, "setProvisionStep() CSR start");
				//executeCommand(CMD_MAKE_CSR);
				SystemProperties.set("inno.provision.csr", "start");
				break;

			case PROVISION_STEP_APP_KEY:
				Log.d(TAG, "setProvisionStep() APP_KEY start");
				SystemProperties.set("inno.provision.app_key", "start");
				break;

			case PROVISION_STEP_EMMC_OTP:
				Log.d(TAG, "setProvisionStep() EMMC_OTP start");
				SystemProperties.set("inno.provision.emmc_otp", "start");
				break;
		}

		mHandler.sendEmptyMessageDelayed(mProvisionStep, 2000);
	}

	private void checkResult() {
		String ret = null;
		Bundle bundle = new Bundle();

		if ( (mProvisionStep != PROVISION_STEP_CSR) &&
				(mProvisionStep != PROVISION_STEP_APP_KEY) &&
				(mProvisionStep != PROVISION_STEP_EMMC_OTP) )
		{
			Log.e(TAG, "checkResult() Wrong prvision step! step=" + mProvisionStep);
			return;
		}

		bundle.putInt(PROVISION_STEP, mProvisionStep);

		switch( mProvisionStep ) {
			case PROVISION_STEP_CSR:
				Log.d(TAG, "checkResult() provision step=CSR");
				//ret = getCsrData(CMD_READ_CSR);
				ret = readCsrData(CSR_DATA);
				break;

			case PROVISION_STEP_APP_KEY:
				Log.d(TAG, "checkResult() provision step=APP_KEY");
				ret = SystemProperties.get("inno.provision.app_key", "x");
				break;

			case PROVISION_STEP_EMMC_OTP:
				Log.d(TAG, "checkResult() provision step=EMMC_OTP");
				ret = SystemProperties.get("inno.provision.emmc_otp", "x");
				break;
		}
		Log.d(TAG, "checkResult() provision result=" + ret);
		if( ret.equals("success") ) {
			Log.d(TAG, "checkResult() Receive Success!");
			mOnResultListener.onResult(this, RESULT_SUCCEEDED, bundle);
			return;
		} else if( ret.equals("fail") ) {
			// display fail check
			Log.d(TAG, "checkResult() Receive Fail!");
			mOnResultListener.onResult(this, RESULT_FAILED, bundle);
			return;
		} else if ( mProvisionStep == PROVISION_STEP_CSR ) {
			Log.d(TAG, "checkResult() Receive Success!");
			bundle.putString(EXTRA_KEY_CSR_DATA, ret);
			mOnResultListener.onResult(this, RESULT_SUCCEEDED, bundle);
			return ;
		}

		Log.d(TAG, "checkResult() Check the provisioning results again...");
		mHandler.sendEmptyMessageDelayed(mProvisionStep, 2000);
	}

	private String readCsrData(String path) {
		try {
			InputStream is = new FileInputStream(path);
			int n = is.available();
			if ( n <= 0 ) {
				Log.d(TAG, "read Csr failed");
				return null;
			}

			byte[] arr = is.readAllBytes();
			if ( arr != null ) {
				String data = new String(arr);
				Log.d(TAG, "stream(" + n + ") / read(" + arr.length + ")");
				Log.d(TAG, data);
				return data;
			}
		} catch ( IOException e ) {
			e.printStackTrace();
		}
		return null;
	}

	private String getCsrData(String cmd) {
		String data = null;
		try {
			Process process = Runtime.getRuntime().exec(cmd);
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			data = bufferedReader.readLine();
			bufferedReader.close();
		} catch ( IOException e ) {
			e.printStackTrace();
		} finally {
			return data;
		}
	}

	private void executeCommand (String cmd) {
		try {
			Process process = Runtime.getRuntime().exec(cmd);

			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

			bufferedReader.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}


}
