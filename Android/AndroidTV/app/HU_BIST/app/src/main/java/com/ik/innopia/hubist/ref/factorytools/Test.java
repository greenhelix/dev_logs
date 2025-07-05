package com.innopia.factorytools;

import android.content.Context;
import android.os.Bundle;


public abstract class Test {

	public static final int RESULT_SUCCEEDED = 0;
	public static final int RESULT_FAILED = 1;
	public static final int RESULT_UNKNOWN = 2;

	public Context mContext;
	public OnResultListener mOnResultListener;

	public interface OnResultListener {
		void onResult(Test test, int resultCode, Bundle extra);
	}



	public Test(Context context) {
		mContext = context;
	}	



	public void setOnResultListener(OnResultListener onResultListener) {
		mOnResultListener = onResultListener;
	}

	public boolean hasOnResultListener() {
		boolean ret = false;
		if(mOnResultListener != null) ret =  true;
		else ret = false;
		return ret;
	}



	public abstract void start();
	public abstract void stop();
	
}
