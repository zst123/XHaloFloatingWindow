package com.zst.xposed.halo.floatingwindow;

import android.os.Bundle;
import android.app.Activity;
import android.view.View;

public class AboutActivityWhite extends Activity {

	@Override
	protected void onCreate(Bundle f) {
		super.onCreate(f);
		setContentView(R.layout.activity_about);
	}
	
	public void c(View v){
		finish();
	}
}
