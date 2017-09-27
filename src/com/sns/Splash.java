package com.sns;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;

public class Splash extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.splash);

		new Handler().postDelayed(new Runnable() {

			@Override
			public void run() {
				finish();
			}

		}, 2000);

	}

	/*
	 * 스플래시 화면에서 뒤로가기 버튼은 활성화되지 않는다
	 */
	@Override
	public void onBackPressed() {

	}
}
