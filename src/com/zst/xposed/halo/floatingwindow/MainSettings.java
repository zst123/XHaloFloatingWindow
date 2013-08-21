package com.zst.xposed.halo.floatingwindow;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.View;

public class MainSettings extends Activity {
boolean sv = true;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_settings);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main_settings, menu);
		return true;
	}

	
	public void get(){

		 SharedPreferences pref = getApplicationContext().getSharedPreferences(
				 Res.MY_PACKAGE_NAME, MODE_WORLD_READABLE);
		 pref.getString(Res.KEY_APP_FORCED, Res.DEFAULT_APP_FORCED);
	}
	public void help(View v){
		//if (sv){
		Intent t = new Intent(getPackageManager().getLaunchIntentForPackage("com.whatsapp"));
		 t.addFlags(HaloFloatingInject.FLAG_FLOATING_WINDOW);
		// t.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		 startActivity(t);
		//}else{
//			Intent t = new Intent("android.media.action.IMAGE_CAPTURE");

			//Intent t = new Intent(getPackageManager().getLaunchIntentForPackage("com.whatsapp"));
			// t.addFlags(HaloFloatingInject.FLAG_FLOATING_WINDOW);
			 //startActivity(t);
	//	}
		//sv = !sv;
		
	}
}

