package com.zst.xposed.halo.floatingwindow;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;
import de.robv.android.xposed.XSharedPreferences;

public class LayoutScaling {

	public static XSharedPreferences pref;
	
	public static void appleFloating(Context context , Window mWindow){
	    pref = HaloFlagInject.pref;
	    pref.reload();
	    boolean isMovable = pref.getBoolean(Res.KEY_MOVABLE_WINDOW, Res.DEFAULT_MOVABLE_WINDOW);
	    if(isMovable){
	    	mWindow.setCloseOnTouchOutside(false);
			mWindow.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
	    }else{
	    	mWindow.setCloseOnTouchOutsideIfNotSet(true);
	    }
	            mWindow.setGravity(pref.getInt(Res.KEY_GRAVITY, Res.DEFAULT_GRAVITY));
	            mWindow.setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND,WindowManager.LayoutParams.FLAG_DIM_BEHIND);
	            WindowManager.LayoutParams params = mWindow.getAttributes(); 
				Float alp = pref.getFloat(Res.KEY_ALPHA, Res.DEFAULT_ALPHA);
				Float dimm = pref.getFloat(Res.KEY_DIM, Res.DEFAULT_DIM);

	            params.alpha = alp;	
	            params.dimAmount = dimm;
	            mWindow.setAttributes(params);
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
		pref = HaloFlagInject.pref;
	    pref.reload();
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
