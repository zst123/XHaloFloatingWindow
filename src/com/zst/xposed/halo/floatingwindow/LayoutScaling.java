package com.zst.xposed.halo.floatingwindow;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import de.robv.android.xposed.XSharedPreferences;

public class LayoutScaling {

	public static XSharedPreferences pref;
	public static void applyThemeLess(Context context , Window mWindow){
		context.getTheme().applyStyle(R.style.Theme_Halo_FloatingWindow, true);
		appleFloating( context ,  mWindow);
	}
	public static void applyTheme(Context context , Window mWindow, String class_name ){
		try{
			Intent intent__ = new Intent(context.getPackageManager().getLaunchIntentForPackage(class_name));
		        	ResolveInfo rInfo = context.getPackageManager().resolveActivity(intent__, 0);
		        	ActivityInfo info = rInfo.activityInfo;	            
		        	TypedArray ta = context.obtainStyledAttributes(info.theme, com.android.internal.R.styleable.Window);
		        	
		            TypedValue backgroundValue = ta.peekValue(com.android.internal.R.styleable.Window_windowBackground);
		            
		            if (backgroundValue != null && backgroundValue.toString().contains("light")) {
		                context.getTheme().applyStyle(R.style.Theme_Halo_FloatingWindowLight, true);
		            } else {  //Checks if light or dark theme
		                context.getTheme().applyStyle(R.style.Theme_Halo_FloatingWindow, true);
		            }
		            
		            ta.recycle();
			}catch(Throwable t){
	            context.getTheme().applyStyle(R.style.Theme_Halo_FloatingWindow, true);
			}
		appleFloating( context ,  mWindow);
	}
	public static void appleFloating(Context context , Window mWindow){
	    pref = new XSharedPreferences(Res.MY_PACKAGE_NAME,Res.MY_PACKAGE_NAME);

	            mWindow.setCloseOnTouchOutsideIfNotSet(true);
	            mWindow.setGravity(pref.getInt(Res.KEY_GRAVITY, Res.DEFAULT_GRAVITY));
	            mWindow.setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND,WindowManager.LayoutParams.FLAG_DIM_BEHIND);
	            WindowManager.LayoutParams params = mWindow.getAttributes(); 
				Float alp = pref.getFloat(Res.KEY_ALPHA, Res.DEFAULT_ALPHA);
				Float dimm = pref.getFloat(Res.KEY_DIM, Res.DEFAULT_DIM);

	            params.alpha = alp;	
	            params.dimAmount = dimm;
	            mWindow.setAttributes((android.view.WindowManager.LayoutParams) params);
			     scaleFloatingWindow(context,mWindow);
			     
	}

	public static void scaleFloatingWindow(Context context ,  Window mWindow ) {
		DisplayMetrics metrics = new DisplayMetrics();
		try{
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        display.getMetrics(metrics); 
		}catch (Exception e){
			DisplayMetrics dm = context.getResources().getDisplayMetrics();
			metrics = new DisplayMetrics();
			metrics = dm;
		}
	    pref = new XSharedPreferences(Res.MY_PACKAGE_NAME,Res.MY_PACKAGE_NAME);
        if (metrics.heightPixels > metrics.widthPixels) { // portrait 
        	Float width_portrait = pref.getFloat(Res.KEY_PORTRAIT_WIDTH, Res.DEFAULT_PORTRAIT_WIDTH);
    		Float height__portrait = pref.getFloat(Res.KEY_PORTRAIT_HEIGHT, Res.DEFAULT_PORTRAIT_HEIGHT);
            mWindow.setLayout((int)(metrics.widthPixels * width_portrait), (int)(metrics.heightPixels * height__portrait));
        } else {  // landscape
        	Float width_ls = pref.getFloat(Res.KEY_LANDSCAPE_WIDTH, Res.DEFAULT_LANDSCAPE_WIDTH);
    		Float height__ls = pref.getFloat(Res.KEY_LANDSCAPE_HEIGHT, Res.DEFAULT_LANDSCAPE_HEIGHT);
        	mWindow.setLayout((int)(metrics.widthPixels * width_ls), (int)(metrics.heightPixels * height__ls));
        }
        mWindow.setWindowAnimations(android.R.style.Animation_Dialog);
        mWindow.setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        mWindow.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
		int i = pref.getInt(Res.KEY_KEYBOARD_MODE, Res.DEFAULT_KEYBOARD_MODE);
		if (i ==2){
	        mWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
		}else if (i == 3){
	        mWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		}

    }
}
