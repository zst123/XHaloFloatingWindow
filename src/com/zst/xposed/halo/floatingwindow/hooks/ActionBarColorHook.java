package com.zst.xposed.halo.floatingwindow.hooks;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

import com.zst.xposed.halo.floatingwindow.Common;
import com.zst.xposed.halo.floatingwindow.R;

import android.app.ActionBar;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class ActionBarColorHook {
	
	/*
	 * Hook for making the titlebar match the app's actionbar if available.
	 * Code adapted and modified from Tinted StatusBar. Credits to MohammadAG.
	 */
	
	/* Constants */
	static final int COLOR_PLAY_MUSIC_ORANGE = Color.parseColor("#f4842d");
	static final String COLOR_BLACK = "#000000";
	static final String COLOR_WHITE = "#FFFFFF";
	static final String COLOR_DEFAULT_TITLEBAR_BACKGROUND = COLOR_BLACK;
	static final String COLOR_DEFAULT_TITLEBAR_ICON_NORMAL = COLOR_WHITE;
	static final String COLOR_DEFAULT_TITLEBAR_ICON_INVERT = COLOR_BLACK;
	static enum Tint {
		TITLE_BAR,
		ICON,
		ICON_INVERTED
	};
	
	/* Current App Variables*/
	static XSharedPreferences mPref;
	static RelativeLayout mHeader;
	static TextView mAppTitle;
	static ImageButton mCloseButton;
	static ImageButton mMaxButton;
	static ImageButton mMinButton;
	static ImageButton mMoreButton;
	static ImageView mTriangle;
	static ImageView mQuadrant;
	static int mBorderThickness;
	
	public static void handleLoadPackage(LoadPackageParam llpp, XSharedPreferences pref) {
		mPref = pref;
		mPref.reload();
		
		if (!mPref.getBoolean(Common.KEY_TINTED_TITLEBAR_ENABLED,
				Common.DEFAULT_TINTED_TITLEBAR_ENABLED)) return;
		
		try {
			final Class<?> ActionBarImpl = findClass("com.android.internal.app.ActionBarImpl", null);
			final Class<?> ActivityClass = XposedHelpers.findClass("android.app.Activity", null);
			
			findAndHookMethod(ActionBarImpl, "setBackgroundDrawable", Drawable.class,
					new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					int color = getMainColorFromActionBarDrawable((Drawable) param.args[0]);
					int defaultNormal = getDefaultTint(Tint.ICON);
					int invertedIconTint = getDefaultTint(Tint.ICON_INVERTED);
					int icon_color = getIconColorForColor(color, defaultNormal, invertedIconTint,
							getHsvMax());
					changeTitleBarColor(color, icon_color);
				}
			});
			
			findAndHookMethod(ActionBarImpl, "hide", new XC_MethodHook() {
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {					
					int titleBarTint = getDefaultTint(Tint.TITLE_BAR);
					int defaultNormal = getDefaultTint(Tint.ICON);
					int invertedIconTint = getDefaultTint(Tint.ICON_INVERTED);
					int icon_color = getIconColorForColor(titleBarTint, defaultNormal,
							invertedIconTint, getHsvMax());
					changeTitleBarColor(titleBarTint, icon_color);
				};
			});
			
			findAndHookMethod(ActionBarImpl, "show", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					ActionBar actionBar = (ActionBar) param.thisObject;
					changeColorFromActionBarObject(actionBar);
				}
			});
			
			findAndHookMethod(ActivityClass, "onResume", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					Activity activity = (Activity) param.thisObject;
					ActionBar actionBar = activity.getActionBar();
					
					if ((activity.getIntent().getFlags() & Common.FLAG_FLOATING_WINDOW)
							!= Common.FLAG_FLOATING_WINDOW) return;
					
					if (actionBar != null && actionBar.isShowing()) {
						// If it's not showing, we shouldn't detect it.
						changeColorFromActionBarObject(actionBar);
					}
					
				}
			});
		} catch (ClassNotFoundError e) {
		} catch (NoSuchMethodError e) {
		}
	}
	
	private static void changeColorFromActionBarObject(ActionBar actionBar){
		Object actionBarContainer = getObjectField(actionBar, "mContainerView");
		int actionBarTextColor = -2;
		try {
			TextView mTitleView = (TextView) getObjectField(
					getObjectField(actionBarContainer, "mActionBarView"), "mTitleView");
			if (mTitleView != null) {
				if (mTitleView.getVisibility() == View.VISIBLE) {
					actionBarTextColor = mTitleView.getCurrentTextColor();
				}
			}
		} catch (Throwable t) {
		}
		
		Drawable drawable = (Drawable) getObjectField(actionBarContainer, "mBackground");
		int color = getMainColorFromActionBarDrawable(drawable);
		int defaultNormal = getDefaultTint(Tint.ICON);
		int invertedIconTint = getDefaultTint(Tint.ICON_INVERTED);
		int iconTint;
		
		if (actionBarTextColor != -2) {
			iconTint = actionBarTextColor;
		} else {
			iconTint = getIconColorForColor(color, defaultNormal,
					invertedIconTint, getHsvMax());
		}
		
		changeTitleBarColor(color, iconTint);
	}
	
	// cache references since finding views are expensive
	public static void setTitleBar(View overlayView) {
		mHeader = (RelativeLayout) overlayView.findViewById(R.id.movable_titlebar);
		mAppTitle = (TextView) overlayView.findViewById(R.id.movable_titlebar_appname);
		mCloseButton = (ImageButton) overlayView.findViewById(R.id.movable_titlebar_close);
		mMaxButton = (ImageButton) overlayView.findViewById(R.id.movable_titlebar_max);
		mMinButton = (ImageButton) overlayView.findViewById(R.id.movable_titlebar_min);
		mMoreButton = (ImageButton) overlayView.findViewById(R.id.movable_titlebar_more);
		mTriangle = (ImageView) overlayView.findViewById(R.id.movable_corner);
		mQuadrant = (ImageView) overlayView.findViewById(R.id.movable_quadrant);
	}
	
	public static void setBorderThickness(int thickness) {
		mBorderThickness = thickness;
	}
	
	private static void changeTitleBarColor(int bg_color, int icon_color) {
		if (!MovableWindow.isHoloFloat || !MovableWindow.mMovableWindow) return;
		// if this is not movable or holo, it will just be null anyways.
		
		try {
			mHeader.setBackgroundColor(bg_color);
			mAppTitle.setTextColor(icon_color);
			mCloseButton.setColorFilter(icon_color, Mode.SRC_ATOP);
			mMaxButton.setColorFilter(icon_color, Mode.SRC_ATOP);
			mMinButton.setColorFilter(icon_color, Mode.SRC_ATOP);
			mMoreButton.setColorFilter(icon_color, Mode.SRC_ATOP);
		} catch (NullPointerException npe) {
			Log.d("test1", Common.LOG_TAG + "ActionBarColorHook.java - changeTitleBarColor - NPE", npe);
		}
		
		if (mPref.getBoolean(Common.KEY_TINTED_TITLEBAR_BORDER_TINT,
				Common.DEFAULT_TINTED_TITLEBAR_BORDER_TINT)) {
			MovableWindow.setWindowBorder(bg_color, mBorderThickness);
		}
		
		if (mPref.getBoolean(Common.KEY_TINTED_TITLEBAR_CORNER_TINT,
				Common.DEFAULT_TINTED_TITLEBAR_CORNER_TINT)) {
			try {
				Drawable triangle_background = mTriangle.getBackground();
				triangle_background.setColorFilter(bg_color, Mode.SRC_ATOP);
				Drawable quadrant_background = mQuadrant.getBackground();
				quadrant_background.setColorFilter(bg_color, Mode.SRC_ATOP);
				if (Build.VERSION.SDK_INT >= 16) {
					mTriangle.setBackground(triangle_background);
					mQuadrant.setBackground(quadrant_background);
				} else {
					mTriangle.setBackgroundDrawable(triangle_background);
					mQuadrant.setBackgroundDrawable(quadrant_background);
				}
			} catch (NullPointerException e) {
				Log.d("test1", Common.LOG_TAG + "ActionBarColorHook.java - changeTitleBarColor2 - NPE", e);
			}
		}
		// if border is zero, the method will take care of it.
	}
	
	private static int getIconColorForColor(int color, int defaultNormal, int defaultInverted,
			float hsvMaxValue) {
		float[] hsv = new float[3];
		Color.colorToHSV(color, hsv);
		float value = hsv[2];
		if (value > hsvMaxValue) {
			return defaultInverted;
		} else {
			return defaultNormal;
		}
	}
	
	private static int getMainColorFromActionBarDrawable(Drawable drawable)
			throws IllegalArgumentException {
		/*
		 * This should fix the bug where a huge part of the ActionBar background
		 * is drawn white.
		 */
		Drawable copyDrawable = drawable.getConstantState().newDrawable();
		if (copyDrawable instanceof ColorDrawable) {
			return ((ColorDrawable) drawable).getColor();
		}
		Bitmap bitmap = drawableToBitmap(copyDrawable);
		int pixel = bitmap.getPixel(0, 5);
		int red = Color.red(pixel);
		int blue = Color.blue(pixel);
		int green = Color.green(pixel);
		int alpha = Color.alpha(pixel);
		return Color.argb(alpha, red, green, blue);
	}
	
	private static Bitmap drawableToBitmap(Drawable drawable) throws IllegalArgumentException {
		if (drawable instanceof BitmapDrawable) {
			return ((BitmapDrawable) drawable).getBitmap();
		}
		Bitmap bitmap;
		try {
			bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
					drawable.getIntrinsicHeight(), Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap);
			drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
			drawable.draw(canvas);
		} catch (IllegalArgumentException e) {
			throw e;
		}
		
		return bitmap;
	}
	
	private static float getHsvMax() {
		mPref.reload();
		return mPref.getFloat(Common.KEY_TINTED_TITLEBAR_HSV, Common.DEFAULT_TINTED_TITLEBAR_HSV);
	}
	
	private static int getDefaultTint(Tint tintType) {
		return Color.parseColor(getDefaultTintString(tintType));
	}
	
	private static String getDefaultTintString(Tint tintType) {
		switch (tintType) {
		case TITLE_BAR:
			return COLOR_DEFAULT_TITLEBAR_BACKGROUND;
		case ICON:
			return COLOR_DEFAULT_TITLEBAR_ICON_NORMAL;
		case ICON_INVERTED:
			return COLOR_DEFAULT_TITLEBAR_ICON_INVERT;
		}
		return COLOR_BLACK;
	}
}