package com.innopia.factorytools;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;


public class TestView extends LinearLayout {

	public static final int STATUS_SUCCEEDED = 0;
	public static final int STATUS_FAILED =1;
	public static final int STATUS_TESTING = 2;
	public static final int STATUS_UNKNOWN = 3;
	public static final int STATUS_READY = 4;

	private View mTestView;
	private TextView mTextViewTest;
	private TextView mTextViewExtra;
	
	private int mStatus;

	private Context mContext;
	

	public TestView(Context context) {
		super(context);
		mContext = context;
		mStatus = 3;
		init(null, 3);
	}



	public TestView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		mStatus = 3;
		init(attrs, 3);
	}



	public TestView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		mStatus = 3;
		init(attrs, defStyle);
	}



	private void init(AttributeSet attrs, int defStyle) {
		LayoutInflater layoutInflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mTestView = layoutInflater.inflate(R.layout.view_test, this, false);
		addView(mTestView);

		mTextViewTest = (TextView)mTestView.findViewById(R.id.textViewTest);
		mTextViewExtra = (TextView)mTestView.findViewById(R.id.textViewExtra);

		TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.TestButton, defStyle, 0);
		String stringTest = typedArray.getString(R.styleable.TestButton_test);

		mStatus = 3;

		mTextViewTest.setText(stringTest);
	}

	public int getStatus() {
	 return mStatus;
	}


	public void setStatus(int status) {

		((Activity)mContext).runOnUiThread(new Runnable() {
			@Override
			public void run() {
				switch(status) {
					case STATUS_TESTING:
						mTestView.setBackgroundColor(Color.GREEN);
						mTextViewTest.setTextColor(Color.BLACK);
						mTextViewExtra.setTextColor(Color.BLACK);
						mStatus = STATUS_TESTING;
						break;
					case STATUS_SUCCEEDED:
						mTestView.setBackgroundColor(Color.BLUE);
						mTextViewTest.setTextColor(Color.WHITE);
						mTextViewExtra.setTextColor(Color.WHITE);
						mStatus = STATUS_SUCCEEDED;
						break;
					case STATUS_FAILED:
						mTestView.setBackgroundColor(Color.RED);
						mTextViewTest.setTextColor(Color.BLACK);
						mTextViewExtra.setTextColor(Color.BLACK);
						mStatus = STATUS_FAILED;
						break;
					case STATUS_UNKNOWN:
						mTestView.setBackgroundColor(Color.LTGRAY);
						mTextViewTest.setTextColor(Color.BLACK);
						mTextViewExtra.setTextColor(Color.BLACK);
						mStatus = STATUS_UNKNOWN;
						break;
					case STATUS_READY:
						mTestView.setBackgroundColor(Color.YELLOW);
						mTextViewTest.setTextColor(Color.BLACK);
						mTextViewExtra.setTextColor(Color.BLACK);
						mStatus = STATUS_READY;
						break;
				}
			}
		});
	}



	public void setStatus(int status, String extra) {

		((Activity)mContext).runOnUiThread(new Runnable() {
			@Override
			public void run() {
				switch(status) {
					case STATUS_TESTING:
						mTestView.setBackgroundColor(Color.GREEN);
						mTextViewTest.setTextColor(Color.BLACK);
						mTextViewExtra.setTextColor(Color.BLACK);
						break;
					case STATUS_SUCCEEDED:
						mTestView.setBackgroundColor(Color.BLUE);
						mTextViewTest.setTextColor(Color.WHITE);
						mTextViewExtra.setTextColor(Color.WHITE);
						break;
					case STATUS_FAILED:
						mTestView.setBackgroundColor(Color.RED);
						mTextViewTest.setTextColor(Color.BLACK);
						mTextViewExtra.setTextColor(Color.BLACK);
						break;
					case STATUS_UNKNOWN:
						mTestView.setBackgroundColor(Color.LTGRAY);
						mTextViewTest.setTextColor(Color.BLACK);
						mTextViewExtra.setTextColor(Color.BLACK);
						break;
					case STATUS_READY:
						mTestView.setBackgroundColor(Color.YELLOW);
						mTextViewTest.setTextColor(Color.BLACK);
						mTextViewExtra.setTextColor(Color.BLACK);
						break;
				}
				mStatus = status;

				if(extra != null && !extra.equals("")) {
					mTextViewExtra.setVisibility(View.VISIBLE);
				} else {
					mTextViewExtra.setVisibility(View.GONE);
				}

				mTextViewExtra.setText(extra);
			}
		});
	}


	public void setStatus(int status ,boolean needUiThread) {

				switch(status) {
					case STATUS_TESTING:
						mTestView.setBackgroundColor(Color.GREEN);
						mTextViewTest.setTextColor(Color.BLACK);
						mTextViewExtra.setTextColor(Color.BLACK);
						mStatus = STATUS_TESTING;
						break;
					case STATUS_SUCCEEDED:
						mTestView.setBackgroundColor(Color.BLUE);
						mTextViewTest.setTextColor(Color.WHITE);
						mTextViewExtra.setTextColor(Color.WHITE);
						mStatus = STATUS_SUCCEEDED;
						break;
					case STATUS_FAILED:
						mTestView.setBackgroundColor(Color.RED);
						mTextViewTest.setTextColor(Color.BLACK);
						mTextViewExtra.setTextColor(Color.BLACK);
						mStatus = STATUS_FAILED;
						break;
					case STATUS_UNKNOWN:
						mTestView.setBackgroundColor(Color.LTGRAY);
						mTextViewTest.setTextColor(Color.BLACK);
						mTextViewExtra.setTextColor(Color.BLACK);
						mStatus = STATUS_UNKNOWN;
						break;
					case STATUS_READY:
						mTestView.setBackgroundColor(Color.YELLOW);
						mTextViewTest.setTextColor(Color.BLACK);
						mTextViewExtra.setTextColor(Color.BLACK);
						mStatus = STATUS_READY;
						break;
				}
	}



	public void setStatus(int status, String extra, boolean needUiThread) {

				switch(status) {
					case STATUS_TESTING:
						mTestView.setBackgroundColor(Color.GREEN);
						mTextViewTest.setTextColor(Color.BLACK);
						mTextViewExtra.setTextColor(Color.BLACK);
						break;
					case STATUS_SUCCEEDED:
						mTestView.setBackgroundColor(Color.BLUE);
						mTextViewTest.setTextColor(Color.WHITE);
						mTextViewExtra.setTextColor(Color.WHITE);
						break;
					case STATUS_FAILED:
						mTestView.setBackgroundColor(Color.RED);
						mTextViewTest.setTextColor(Color.BLACK);
						mTextViewExtra.setTextColor(Color.BLACK);
						break;
					case STATUS_UNKNOWN:
						mTestView.setBackgroundColor(Color.LTGRAY);
						mTextViewTest.setTextColor(Color.BLACK);
						mTextViewExtra.setTextColor(Color.BLACK);
						break;
					case STATUS_READY:
						mTestView.setBackgroundColor(Color.YELLOW);
						mTextViewTest.setTextColor(Color.BLACK);
						mTextViewExtra.setTextColor(Color.BLACK);
						break;
				}
				mStatus = status;

				((Activity)mContext).runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if(extra != null && !extra.equals("")) {
							mTextViewExtra.setVisibility(View.VISIBLE);
						} else {
							mTextViewExtra.setVisibility(View.GONE);
						}
						mTextViewExtra.setText(extra);
					}
				});
	}
}
