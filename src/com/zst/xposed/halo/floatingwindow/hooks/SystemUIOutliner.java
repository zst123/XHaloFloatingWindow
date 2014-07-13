package com.zst.xposed.halo.floatingwindow.hooks;

import static de.robv.android.xposed.XposedHelpers.findClass;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.zst.xposed.halo.floatingwindow.Common;
import com.zst.xposed.halo.floatingwindow.helpers.Util;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class SystemUIOutliner {
	
	private static Context mContext;
	private static View mOutline;
	private static WindowManager mWm;
	
	static final int HIDE = -10000;
	
	public static void handleLoadPackage(LoadPackageParam lpp) {
		if (!lpp.packageName.equals("com.android.systemui")) return;
		
		try {
			focusChangeContextFinder(lpp);
		} catch (Throwable e) {
			XposedBridge.log(Common.LOG_TAG + "Movable / SystemUIOutliner");
			XposedBridge.log(e);
		}
	}
	
	private static void focusChangeContextFinder(LoadPackageParam l) throws Throwable {
		Class<?> hookClass = findClass("com.android.systemui.SystemUIService", l.classLoader);
		XposedBridge.hookAllMethods(hookClass, "onCreate", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				Service thiz = (Service) param.thisObject;
				mContext = thiz.getApplicationContext();
				mContext.registerReceiver(mIntentReceiver, new IntentFilter(Common.SHOW_OUTLINE));
			}
		});
	}
	
	final static BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context ctx, Intent intent) {
			int[] array = intent.getIntArrayExtra(Common.INTENT_APP_PARAMS);
			int[] array2 = intent.getIntArrayExtra(Common.INTENT_APP_SNAP);
			if (array != null) {
				refreshOutlineView(ctx, array[0], array[1], array[2], array[3]);
			} else if (array2 != null) {
				refreshOutlineView(ctx, array2[0], array2[1], array2[2]);
			} else {
				refreshOutlineView(ctx, HIDE, HIDE, HIDE, HIDE);
			}
		}
	};
	
	// Create a view in SystemUI window manager
	private static void createOutlineView(Context ctx) {
		if (mWm == null) {
			mWm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
		}
		WindowManager.LayoutParams layOutParams = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
				WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
				PixelFormat.TRANSLUCENT);
		layOutParams.gravity = Gravity.TOP | Gravity.LEFT;
		Util.addPrivateFlagNoMoveAnimationToLayoutParam(layOutParams);
		mOutline = getOutlineView(ctx, 0xFF33b5e5);
		mOutline.setFocusable(false);
		mOutline.setClickable(false);
		mOutline.setVisibility(View.GONE);
		
		mWm.addView(mOutline, layOutParams);
	}
	
	// show the outline with positioning (x,y)
	private static void refreshOutlineView(Context ctx, int x, int y, int height, int width) {
		if (mOutline == null) {
			createOutlineView(ctx);
		}
		if (x == HIDE || y == HIDE || height == HIDE || width == HIDE) {
			mOutline.setVisibility(View.GONE);
			return;
		}
		WindowManager.LayoutParams param = (WindowManager.LayoutParams) mOutline.getLayoutParams();
		param.x = x;
		param.y = y;
		param.height = height;
		param.width = width;		
		param.gravity = Gravity.TOP | Gravity.LEFT;
		mWm.updateViewLayout(mOutline, param);
		mOutline.setVisibility(View.VISIBLE);
	}
	
	// show the outline with gravity
	private static void refreshOutlineView(Context ctx, int w, int h, int g) {
		if (mOutline == null) {
			createOutlineView(ctx);
		}
		if (h == HIDE || w == HIDE || g == HIDE) {
			mOutline.setVisibility(View.GONE);
			return;
		}
		WindowManager.LayoutParams param = (WindowManager.LayoutParams) mOutline.getLayoutParams();
		param.x = 0;
		param.y = 0;
		param.height = h;
		param.width = w;		
		param.gravity = g;
		mWm.updateViewLayout(mOutline, param);
		mOutline.setVisibility(View.VISIBLE);
	}
	
	// create outline view with translucent filling
	private static View getOutlineView(Context ctx, int color) {
		FrameLayout outline = new FrameLayout(ctx);
		Util.setBackgroundDrawable(outline, Util.makeOutline(color, Util.realDp(4, ctx)));
		
		View filling = new View(ctx);
		filling.setBackgroundColor(color);
		filling.setAlpha(0.5f);
		outline.addView(filling);
		
		return outline;
	}
}
