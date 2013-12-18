package com.zst.xposed.halo.floatingwindow;

import android.os.Build;
import android.view.Gravity;

public class Common {
	
	/* Preference misc */
	public static final String THIS_PACKAGE_NAME = Common.class.getPackage().getName();
	public static final String PREFERENCE_MAIN_FILE = THIS_PACKAGE_NAME + "_main";
	public static final String PREFERENCE_BLACKLIST_FILE = THIS_PACKAGE_NAME + "_blacklist";
	
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
	public static final String KEY_WINDOW_MOVING_RETAIN_START_POSITION = "window_moving_start_pos_enabled";
	public static final String KEY_WINDOW_MOVING_CONSTANT_POSITION = "window_moving_move_pos_enabled";
	public static final String KEY_WINDOW_TRIANGLE_ENABLE = "window_triangle_enabled";
	public static final String KEY_WINDOW_TRIANGLE_COLOR = "window_triangle_color";
	public static final String KEY_WINDOW_TRIANGLE_ALPHA = "window_triangle_alpha";
	public static final String KEY_WINDOW_TRIANGLE_SIZE = "window_triangle_size";
	public static final String KEY_WINDOW_TRIANGLE_CLICK_ACTION = "window_triangle_sp_action";
	public static final String KEY_WINDOW_TRIANGLE_LONGPRESS_ACTION = "window_triangle_lp_action";
	public static final String KEY_WINDOW_TRIANGLE_RESIZE_ENABLED = "window_triangle_resize_enabled";
	public static final String KEY_WINDOW_QUADRANT_ENABLE = "window_quadrant_enabled";
	public static final String KEY_WINDOW_QUADRANT_COLOR = "window_quadrant_color";
	public static final String KEY_WINDOW_QUADRANT_ALPHA = "window_quadrant_alpha";
	public static final String KEY_WINDOW_QUADRANT_SIZE = "window_quadrant_size";
	public static final String KEY_WINDOW_QUADRANT_CLICK_ACTION = "window_quadrant_sp_action";
	public static final String KEY_WINDOW_QUADRANT_LONGPRESS_ACTION = "window_quadrant_lp_action";
	public static final String KEY_WINDOW_QUADRANT_RESIZE_ENABLED = "window_quadrant_resize_enabled";
	public static final String KEY_WINDOW_BORDER_ENABLED = "window_border_enabled";
	public static final String KEY_WINDOW_BORDER_COLOR = "window_border_color";
	public static final String KEY_WINDOW_BORDER_THICKNESS = "window_border_thickness";
	public static final String KEY_DISABLE_AUTO_CLOSE = "window_disable_auto_close";
	public static final String KEY_SHOW_APP_IN_RECENTS = "window_show_recents";
	public static final String KEY_FLOATING_QUICK_SETTINGS = "system_notif_floating_quick_settings";
	public static final String KEY_RESTART_SYSTEMUI = "restart_systemui";
	public static final String KEY_FORCE_OPEN_APP_ABOVE_HALO = "window_force_open_app_above_halo";
	
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
	public static final boolean DEFAULT_WINDOW_MOVING_RETAIN_START_POSITION = true;
	public static final boolean DEFAULT_WINDOW_MOVING_CONSTANT_POSITION = true;
	public static final boolean DEFAULT_WINDOW_TRIANGLE_ENABLE = true;
	public static final String DEFAULT_WINDOW_TRIANGLE_COLOR = "FFFFFF";
	public static final float DEFAULT_WINDOW_TRIANGLE_ALPHA = 1f;
	public static final int DEFAULT_WINDOW_TRIANGLE_SIZE = 36;
	public static final String DEFAULT_WINDOW_TRIANGLE_CLICK_ACTION = "0";
	public static final String DEFAULT_WINDOW_TRIANGLE_LONGPRESS_ACTION = "1";
	public static final boolean DEFAULT_WINDOW_TRIANGLE_RESIZE_ENABLED = true;
	public static final boolean DEFAULT_WINDOW_QUADRANT_ENABLE = false;
	public static final String DEFAULT_WINDOW_QUADRANT_COLOR = "FFFFFF";
	public static final float DEFAULT_WINDOW_QUADRANT_ALPHA = 1f;
	public static final int DEFAULT_WINDOW_QUADRANT_SIZE = 36;
	public static final String DEFAULT_WINDOW_QUADRANT_CLICK_ACTION = "0";
	public static final String DEFAULT_WINDOW_QUADRANT_LONGPRESS_ACTION = "0";
	public static final boolean DEFAULT_WINDOW_QUADRANT_RESIZE_ENABLED = false;
	public static final boolean DEFAULT_WINDOW_BORDER_ENABLED = false;
	public static final String DEFAULT_WINDOW_BORDER_COLOR = "FFFFFF";
	public static final int DEFAULT_WINDOW_BORDER_THICKNESS = 0;
	public static final boolean DEFAULT_DISABLE_AUTO_CLOSE = false;
	public static final boolean DEFAULT_SHOW_APP_IN_RECENTS = false;
	public static final boolean DEFAULT_FLOATING_QUICK_SETTINGS = false;
	public static final boolean DEFAULT_FORCE_OPEN_APP_ABOVE_HALO = false;
	
	/* Xposed Constants */
	public static final int FLAG_FLOATING_WINDOW = 0x00002000;
	public static final String CHANGE_APP_FOCUS = THIS_PACKAGE_NAME + ".CHANGE_APP_FOCUS";
	public static final String REFRESH_APP_LAYOUT = THIS_PACKAGE_NAME + ".REFRESH_APP_LAYOUT";

	/* Others */
	public static final String LOG_TAG = "XHaloFloatingWindow(" + Build.VERSION.SDK_INT + ") - ";	
	public static final int LAYOUT_RECEIVER_TAG = android.R.id.background;
}
