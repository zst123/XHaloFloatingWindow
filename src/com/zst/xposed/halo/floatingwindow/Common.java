package com.zst.xposed.halo.floatingwindow;

import android.os.Build;
import android.view.Gravity;

public class Common {
	
	/* Preference misc */
	public static final String THIS_PACKAGE_NAME = Common.class.getPackage().getName();
	public static final String PREFERENCE_MAIN_FILE = THIS_PACKAGE_NAME + "_main";
	
	/* Preference keys */
	public static final String KEY_ALPHA = "window_alpha";
	public static final String KEY_DIM = "window_dim";
	public static final String KEY_PORTRAIT_WIDTH = "window_portrait_width";
	public static final String KEY_PORTRAIT_HEIGHT = "window_portrait_height";
	public static final String KEY_LANDSCAPE_WIDTH = "window_landscape_width";
	public static final String KEY_LANDSCAPE_HEIGHT = "window_landscape_height";
	public static final String KEY_GRAVITY = "window_gravity";
	public static final String KEY_MOVABLE_WINDOW = "window_movable";
	public static final String KEY_KEYBOARD_MODE = "window_keyboard_mode";
	public static final String KEY_APP_PAUSE = "system_app_pausing";
	public static final String KEY_NOTIFICATION_LONGPRESS_OPTION = "system_notif_longpress_option";
	public static final String KEY_NOTIFICATION_SINGLE_CLICK_HALO = "system_notif_single_click_halo";
	public static final String KEY_WINDOW_TRIANGLE_COLOR = "window_triangle_color";
	public static final String KEY_WINDOW_TRIANGLE_ALPHA = "window_triangle_alpha";
	public static final String KEY_WINDOW_TRIANGLE_SIZE = "window_triangle_size";
	
	/* Preference defaults */
	public static final float DEFAULT_ALPHA = 1f;
	public static final float DEFAULT_DIM = 0.25f;
	public static final float DEFAULT_PORTRAIT_WIDTH = 0.95f;
	public static final float DEFAULT_PORTRAIT_HEIGHT = 0.7f;
	public static final float DEFAULT_LANDSCAPE_WIDTH = 0.7f;
	public static final float DEFAULT_LANDSCAPE_HEIGHT = 0.85f;
	public static final int DEFAULT_KEYBOARD_MODE = 1;
	public static final int DEFAULT_GRAVITY = Gravity.CENTER;
	public static final boolean DEFAULT_APP_PAUSE = true;
	public static final boolean DEFAULT_MOVABLE_WINDOW = false;
	public static final boolean DEFAULT_NOTIFICATION_LONGPRESS_OPTION = false;
	public static final boolean DEFAULT_NOTIFICATION_SINGLE_CLICK_HALO = false;
	public static final String DEFAULT_WINDOW_TRIANGLE_COLOR = "FFFFFF";
	public static final float DEFAULT_WINDOW_TRIANGLE_ALPHA = 1f;
	public static final int DEFAULT_WINDOW_TRIANGLE_SIZE = 36;
	
	/* Xposed Constants */
	public static final int FLAG_FLOATING_WINDOW = 0x00002000;
	public static final String CHANGE_APP_FOCUS = THIS_PACKAGE_NAME + ".CHANGE_APP_FOCUS";

	/* Others */
	public static final String LOG_TAG = "XHaloFloatingWindow(" + Build.VERSION.SDK_INT + ") - ";	
}
