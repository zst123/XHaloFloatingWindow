package com.zst.xposed.halo.floatingwindow.helpers;

import com.zst.xposed.halo.floatingwindow.Common;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;

public class LayoutScaling {
	
	// set the relevent settings to have halo windows
	public static void appleFloating(XSharedPreferences pref, Window window) {
		pref.reload();
		boolean isMovable = pref.getBoolean(Common.KEY_MOVABLE_WINDOW, Common.DEFAULT_MOVABLE_WINDOW);
		if (isMovable) {
			window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
		} else {
			XposedHelpers.callMethod(window, "setCloseOnTouchOutsideIfNotSet", true);
			// window.setCloseOnTouchOutsideIfNotSet(true);
		}
		window.setGravity(pref.getInt(Common.KEY_GRAVITY, Common.DEFAULT_GRAVITY));
		window.setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND,
				WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		
		WindowManager.LayoutParams params = window.getAttributes();
		Float alp = pref.getFloat(Common.KEY_ALPHA, Common.DEFAULT_ALPHA);
		Float dimm = pref.getFloat(Common.KEY_DIM, Common.DEFAULT_DIM);
		params.alpha = alp;
		params.dimAmount = dimm;
		
		Util.addPrivateFlagNoMoveAnimationToLayoutParam(params);
		
		window.setAttributes(params);
		window.addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
		window.setWindowAnimations(android.R.style.Animation_Dialog);
		window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
		int i = pref.getInt(Common.KEY_KEYBOARD_MODE, Common.DEFAULT_KEYBOARD_MODE);
		if (i == 2) {
			window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
		} else if (i == 3) {
			window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		}
		
		scaleFloatingWindow(pref, window.getContext(), window);
	}

	private static void scaleFloatingWindow(XSharedPreferences pref, Context context, Window window) {
		pref.reload();
		DisplayMetrics metrics = new DisplayMetrics();
		try {
			WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
			Display display = wm.getDefaultDisplay();
			display.getMetrics(metrics);
		} catch (Exception e) {
			DisplayMetrics dm = context.getResources().getDisplayMetrics();
			metrics = new DisplayMetrics();
			metrics = dm;
		}
		pref.reload();
		if (metrics.heightPixels > metrics.widthPixels) { // portrait
			Float width_portrait = pref.getFloat(Common.KEY_PORTRAIT_WIDTH,
					Common.DEFAULT_PORTRAIT_WIDTH);
			Float height__portrait = pref.getFloat(Common.KEY_PORTRAIT_HEIGHT,
					Common.DEFAULT_PORTRAIT_HEIGHT);
			window.setLayout((int) (metrics.widthPixels * width_portrait),
					(int) (metrics.heightPixels * height__portrait));
		} else { // landscape
			Float width_ls = pref.getFloat(Common.KEY_LANDSCAPE_WIDTH,
					Common.DEFAULT_LANDSCAPE_WIDTH);
			Float height__ls = pref.getFloat(Common.KEY_LANDSCAPE_HEIGHT,
					Common.DEFAULT_LANDSCAPE_HEIGHT);
			window.setLayout((int) (metrics.widthPixels * width_ls),
					(int) (metrics.heightPixels * height__ls));
		}
	}
}
