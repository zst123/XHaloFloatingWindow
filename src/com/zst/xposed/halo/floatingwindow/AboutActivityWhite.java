package com.zst.xposed.halo.floatingwindow;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class AboutActivityWhite extends Activity {

	@Override
	protected void onCreate(Bundle f) {
		appleFloating(this.getApplicationContext(), this.getWindow(), Res.MY_PACKAGE_NAME);
		super.onCreate(f);
		setContentView(R.layout.activity_about);
	}
	
	public void c(View v){
		finish();
	}
	public  void appleFloating(Context context , Window mWindow, String class_name ){
		try{
		Intent intent__ = new Intent(context.getPackageManager().getLaunchIntentForPackage(class_name));
	        	ResolveInfo rInfo = context.getPackageManager().resolveActivity(intent__, 0);
	        	ActivityInfo info = rInfo.activityInfo;	            
	        	TypedArray ta = context.obtainStyledAttributes(info.theme, com.android.internal.R.styleable.Window);
	        	
	            TypedValue backgroundValue = ta.peekValue(com.android.internal.R.styleable.Window_windowBackground);
	            // Apps that have no title don't need no title bar
	            boolean gotTitle = ta.getBoolean(com.android.internal.R.styleable.Window_windowNoTitle, false);
	            if (gotTitle) mWindow.requestFeature(Window.FEATURE_NO_TITLE);
	           
	            if (backgroundValue != null && backgroundValue.toString().contains("light")) {
	                context.getTheme().applyStyle(R.style.Theme_Halo_FloatingWindowLight, true);
	            } else {  //Checks if light or dark theme
	                context.getTheme().applyStyle(R.style.Theme_Halo_FloatingWindow, true);
	            }
	            
	            ta.recycle();
		}catch(Throwable t){
            context.getTheme().applyStyle(R.style.Theme_Halo_FloatingWindow, true);
		}
	            // Create our new window
	           //mWindow.mIsFloatingWindow = true; < We dont need this. onCreate Hook will compare getTaskId and resize accordingly
	            mWindow.setCloseOnTouchOutsideIfNotSet(true);
	            mWindow.setGravity(Gravity.CENTER);
	            mWindow.setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND,WindowManager.LayoutParams.FLAG_DIM_BEHIND);
	            WindowManager.LayoutParams params = mWindow.getAttributes(); 
	            SharedPreferences pref = getApplicationContext().getSharedPreferences(
	   				 Res.MY_PACKAGE_NAME, MODE_WORLD_READABLE);
	            Float alp = pref.getFloat(Res.KEY_ALPHA, Res.DEFAULT_ALPHA);
				Float dimm = pref.getFloat(Res.KEY_DIM, Res.DEFAULT_DIM);

	            params.alpha = alp;	
	            params.dimAmount = dimm;
	            mWindow.setAttributes((android.view.WindowManager.LayoutParams) params);
			     scaleFloatingWindow(context,mWindow);
	}

	public  void scaleFloatingWindow(Context context ,  Window mWindow ) {		
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics); 
        SharedPreferences pref = getApplicationContext().getSharedPreferences(
				 Res.MY_PACKAGE_NAME, MODE_WORLD_READABLE);
        if (metrics.heightPixels > metrics.widthPixels) { // portrait 
        	Float width_portrait = pref.getFloat(Res.KEY_PORTRAIT_WIDTH, Res.DEFAULT_PORTRAIT_WIDTH);
    		Float height__portrait = pref.getFloat(Res.KEY_PORTRAIT_HEIGHT, Res.DEFAULT_PORTRAIT_HEIGHT);
            mWindow.setLayout((int)(metrics.widthPixels * width_portrait), (int)(metrics.heightPixels * height__portrait));
        } else {  // landscape
        	Float width_ls = pref.getFloat(Res.KEY_LANDSCAPE_WIDTH, Res.DEFAULT_LANDSCAPE_WIDTH);
    		Float height__ls = pref.getFloat(Res.KEY_LANDSCAPE_HEIGHT, Res.DEFAULT_LANDSCAPE_HEIGHT);
        	mWindow.setLayout((int)(metrics.widthPixels * width_ls), (int)(metrics.heightPixels * height__ls));
        }
    }
}
