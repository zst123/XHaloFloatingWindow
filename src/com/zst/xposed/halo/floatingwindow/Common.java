package com.zst.xposed.halo.floatingwindow;

import com.zst.xposed.halo.floatingwindow.preferences.TitleBarSettingsActivity;

import android.os.Build;
import android.view.Gravity;

public class Common {

	/* Preference misc */
	public static final String THIS_PACKAGE_NAME = Common.class.getPackage().getName();
	public static final String PREFERENCE_MAIN_FILE = THIS_PACKAGE_NAME + "_main";
	public static final String PREFERENCE_BLACKLIST_FILE = THIS_PACKAGE_NAME + "_blacklist";
	public static final String PREFERENCE_WHITELIST_FILE = THIS_PACKAGE_NAME + "_whitelist";
	public static final String PREFERENCE_STATUSBAR_LAUNCHER_FILE = THIS_PACKAGE_NAME + "_statusbar_launcher";
	

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
	public static final String KEY_SYSTEM_PREVENT_HOME_TO_FRONT = "system_prevent_home_to_front";
	public static final String KEY_SYSTEM_RECENTS_LONGPRESS_OPTION = "system_recents_long_click_option";
	public static final String KEY_WINDOW_MOVING_RETAIN_START_POSITION = "window_moving_start_pos_enabled";
	public static final String KEY_WINDOW_MOVING_CONSTANT_POSITION = "window_moving_move_pos_enabled";
	public static final String KEY_WINDOW_RESIZING_LIVE_UPDATE = "window_resizing_live_updating";
	public static final String KEY_WINDOW_RESIZING_AERO_SNAP_ENABLED = "window_resizing_aero_snap_enabled";
	public static final String KEY_WINDOW_RESIZING_AERO_SNAP_DELAY = "window_resizing_aero_snap_delay";
	public static final String KEY_WINDOW_RESIZING_AERO_SNAP_SWIPE_APP = "window_resizing_aero_snap_swipe_app";
	public static final String KEY_WINDOW_RESIZING_AERO_SNAP_TITLEBAR_HIDE = "window_resizing_aero_snap_titlebar_hide";
	public static final String KEY_WINDOW_RESIZING_AERO_SNAP_SPLITBAR_ENABLED = "window_resizing_aero_snap_splitbar";
	public static final String KEY_WINDOW_RESIZING_AERO_SNAP_SPLITBAR_COLOR = "window_resizing_aero_snap_splitbar_color";
	public static final String KEY_WINDOW_TITLEBAR_SIZE = "window_moving_titlebar_size";
	public static final String KEY_WINDOW_TITLEBAR_SEPARATOR_ENABLED = "window_moving_titlebar_separator_enabled";
	public static final String KEY_WINDOW_TITLEBAR_SEPARATOR_SIZE = "window_moving_titlebar_separator_size";
	public static final String KEY_WINDOW_TITLEBAR_SEPARATOR_COLOR = "window_moving_titlebar_separator_color";
	public static final String KEY_WINDOW_TITLEBAR_MAXIMIZE_HIDE = "window_moving_titlebar_max_hide";
	public static final String KEY_WINDOW_TITLEBAR_ICON_TYPE = "window_moving_titlebar_icon_type";
	public static final String KEY_WINDOW_TITLEBAR_SINGLE_WINDOW = "window_moving_titlebar_single_window";
	public static final String KEY_WINDOW_ACTIONBAR_DRAGGING_ENABLED = "window_moving_move_ab_enabled";
	public static final String KEY_WINDOW_TRIANGLE_ENABLE = "window_triangle_enabled";
	public static final String KEY_WINDOW_TRIANGLE_COLOR = "window_triangle_color";
	public static final String KEY_WINDOW_TRIANGLE_ALPHA = "window_triangle_alpha";
	public static final String KEY_WINDOW_TRIANGLE_SIZE = "window_triangle_size";
	public static final String KEY_WINDOW_TRIANGLE_CLICK_ACTION = "window_triangle_sp_action";
	public static final String KEY_WINDOW_TRIANGLE_LONGPRESS_ACTION = "window_triangle_lp_action";
	public static final String KEY_WINDOW_TRIANGLE_RESIZE_ENABLED = "window_triangle_resize_enabled";
	public static final String KEY_WINDOW_TRIANGLE_DRAGGING_ENABLED = "window_triangle_dragging_enabled";
	public static final String KEY_WINDOW_QUADRANT_ENABLE = "window_quadrant_enabled";
	public static final String KEY_WINDOW_QUADRANT_COLOR = "window_quadrant_color";
	public static final String KEY_WINDOW_QUADRANT_ALPHA = "window_quadrant_alpha";
	public static final String KEY_WINDOW_QUADRANT_SIZE = "window_quadrant_size";
	public static final String KEY_WINDOW_QUADRANT_CLICK_ACTION = "window_quadrant_sp_action";
	public static final String KEY_WINDOW_QUADRANT_LONGPRESS_ACTION = "window_quadrant_lp_action";
	public static final String KEY_WINDOW_QUADRANT_RESIZE_ENABLED = "window_quadrant_resize_enabled";
	public static final String KEY_WINDOW_QUADRANT_DRAGGING_ENABLED = "window_quadrant_dragging_enabled";
	public static final String KEY_WINDOW_BORDER_ENABLED = "window_border_enabled";
	public static final String KEY_WINDOW_BORDER_COLOR = "window_border_color";
	public static final String KEY_WINDOW_BORDER_THICKNESS = "window_border_thickness";
	public static final String KEY_TINTED_TITLEBAR_ENABLED = "window_tinted_title_enabled";
	public static final String KEY_TINTED_TITLEBAR_HSV = "window_tinted_title_hsv";
	public static final String KEY_TINTED_TITLEBAR_BORDER_TINT = "window_tinted_title_bordertint";
	public static final String KEY_TINTED_TITLEBAR_CORNER_TINT = "window_tinted_title_cornertint";
	public static final String KEY_DISABLE_AUTO_CLOSE = "window_disable_auto_close";
	public static final String KEY_SHOW_APP_IN_RECENTS = "window_show_recents";
	public static final String KEY_FORCE_APP_IN_RECENTS = "window_force_recents";
	public static final String KEY_MINIMIZE_APP_TO_STATUSBAR = "window_minimize_to_statusbar";
	public static final String KEY_FLOATING_QUICK_SETTINGS = "system_notif_floating_quick_settings";
	public static final String KEY_RESTART_SYSTEMUI = "restart_systemui";
	public static final String KEY_FORCE_OPEN_APP_ABOVE_HALO = "window_force_open_app_above_halo";
	public static final String KEY_BLACKLIST_APPS = "window_blacklist";
	public static final String KEY_WHITELIST_APPS = "window_whitelist";
	public static final String KEY_BLACKLIST_HELP = "window_blacklist_help";
	public static final String KEY_WHITELIST_HELP = "window_whitelist_help";
	public static final String KEY_WHITEBLACKLIST_OPTIONS = "window_whiteblacklist_options";
	public static final String KEY_STATUSBAR_TASKBAR_ENABLED = "statusbar_taskbar_enabled";
	public static final String KEY_STATUSBAR_TASKBAR_PINNED_APPS = "statusbar_taskbar_pin_apps";
	public static final String KEY_STATUSBAR_TASKBAR_RESTART_SYSTEMUI = "statusbar_taskbar_restart_systemui";
	public static final String KEY_STATUSBAR_TASKBAR_RUNNING_APPS_ENABLED = "statusbar_taskbar_running_enabled";
	public static final String KEY_STATUSBAR_TASKBAR_HIDE_ICON = "statusbar_taskbar_hide_icon";
	public static final String KEY_STATUSBAR_TASKBAR_NUMBER = "statusbar_taskbar_number";
	

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
	public static final boolean DEFAULT_SYSTEM_PREVENT_HOME_TO_FRONT = false;
	public static final boolean DEFAULT_SYSTEM_RECENTS_LONGPRESS_OPTION = false;
	public static final boolean DEFAULT_WINDOW_MOVING_RETAIN_START_POSITION = true;
	public static final boolean DEFAULT_WINDOW_MOVING_CONSTANT_POSITION = true;
	public static final boolean DEFAULT_WINDOW_RESIZING_LIVE_UPDATE = false;
	public static final boolean DEFAULT_WINDOW_RESIZING_AERO_SNAP_ENABLED = true;
	public static final int DEFAULT_WINDOW_RESIZING_AERO_SNAP_DELAY = 1000;
	public static final boolean DEFAULT_WINDOW_RESIZING_AERO_SNAP_SWIPE_APP = false;
	public static final boolean DEFAULT_WINDOW_RESIZING_AERO_SNAP_TITLEBAR_HIDE = true;
	public static final boolean DEFAULT_WINDOW_RESIZING_AERO_SNAP_SPLITBAR_ENABLED = true;
	public static final String DEFAULT_WINDOW_RESIZING_AERO_SNAP_SPLITBAR_COLOR = "33b5e5";
	public static final boolean DEFAULT_WINDOW_TITLEBAR_ENABLED = true;
	public static final int DEFAULT_WINDOW_TITLEBAR_SIZE = 32;
	public static final boolean DEFAULT_WINDOW_TITLEBAR_SEPARATOR_ENABLED = false;
	public static final int DEFAULT_WINDOW_TITLEBAR_SEPARATOR_SIZE = 2;
	public static final String DEFAULT_WINDOW_TITLEBAR_SEPARATOR_COLOR = "FFFFFF";
	public static final boolean DEFAULT_WINDOW_TITLEBAR_MAXIMIZE_HIDE = true;
	public static final int DEFAULT_WINDOW_TITLEBAR_ICONS_TYPE = TitleBarSettingsActivity.TITLEBAR_ICON_DEFAULT;
	public static final boolean DEFAULT_WINDOW_TITLEBAR_SINGLE_WINDOW = false;
	public static final boolean DEFAULT_WINDOW_ACTIONBAR_DRAGGING_ENABLED = true;
	public static final boolean DEFAULT_WINDOW_TRIANGLE_ENABLE = true;
	public static final String DEFAULT_WINDOW_TRIANGLE_COLOR = "FFFFFF";
	public static final float DEFAULT_WINDOW_TRIANGLE_ALPHA = 1f;
	public static final int DEFAULT_WINDOW_TRIANGLE_SIZE = 36;
	public static final String DEFAULT_WINDOW_TRIANGLE_CLICK_ACTION = "0";
	public static final String DEFAULT_WINDOW_TRIANGLE_LONGPRESS_ACTION = "1";
	public static final boolean DEFAULT_WINDOW_TRIANGLE_RESIZE_ENABLED = true;
	public static final boolean DEFAULT_WINDOW_TRIANGLE_DRAGGING_ENABLED = false;
	public static final boolean DEFAULT_WINDOW_QUADRANT_ENABLE = false;
	public static final String DEFAULT_WINDOW_QUADRANT_COLOR = "FFFFFF";
	public static final float DEFAULT_WINDOW_QUADRANT_ALPHA = 1f;
	public static final int DEFAULT_WINDOW_QUADRANT_SIZE = 36;
	public static final String DEFAULT_WINDOW_QUADRANT_CLICK_ACTION = "0";
	public static final String DEFAULT_WINDOW_QUADRANT_LONGPRESS_ACTION = "0";
	public static final boolean DEFAULT_WINDOW_QUADRANT_RESIZE_ENABLED = false;
	public static final boolean DEFAULT_WINDOW_QUADRANT_DRAGGING_ENABLED = false;
	public static final boolean DEFAULT_WINDOW_BORDER_ENABLED = true;
	public static final String DEFAULT_WINDOW_BORDER_COLOR = "000000";
	public static final int DEFAULT_WINDOW_BORDER_THICKNESS = 0;
	public static final boolean DEFAULT_TINTED_TITLEBAR_ENABLED = true;
	public static final float DEFAULT_TINTED_TITLEBAR_HSV = 0.9f;
	public static final boolean DEFAULT_TINTED_TITLEBAR_BORDER_TINT = true;
	public static final boolean DEFAULT_TINTED_TITLEBAR_CORNER_TINT = true;
	public static final boolean DEFAULT_DISABLE_AUTO_CLOSE = true;
	public static final boolean DEFAULT_SHOW_APP_IN_RECENTS = false;
	public static final boolean DEFAULT_FORCE_APP_IN_RECENTS = false;
	public static final boolean DEFAULT_MINIMIZE_APP_TO_STATUSBAR = true;
	public static final boolean DEFAULT_FLOATING_QUICK_SETTINGS = false;
	public static final boolean DEFAULT_FORCE_OPEN_APP_ABOVE_HALO = false;
	public static final String DEFAULT_WHITEBLACKLIST_OPTIONS = "0";
	public static final boolean DEFAULT_STATUSBAR_TASKBAR_ENABLED = false;
	public static final boolean DEFAULT_STATUSBAR_TASKBAR_RUNNING_APPS_ENABLED = true;
	public static final boolean DEFAULT_STATUSBAR_TASKBAR_HIDE_ICON = false;
	public static final int DEFAULT_STATUSBAR_TASKBAR_NUMBER = 5;

	/* Xposed Constants */
	public static final int FLAG_FLOATING_WINDOW = 0x00002000;
	public static final String EXTRA_SNAP_SIDE = THIS_PACKAGE_NAME + ".EXTRA_SNAP_SIDE";
	public static final String REFRESH_APP_LAYOUT = THIS_PACKAGE_NAME + ".REFRESH_APP_LAYOUT";

	/* Others */
	public static final String LOG_TAG = "XHaloFloatingWindow(SDK: " + Build.VERSION.SDK_INT + ") - ";
	public static final int LAYOUT_RECEIVER_TAG = android.R.id.background;
	public static final int LAYOUT_OVERLAY_TAG = android.R.id.extractArea;
	public static final String XDA_THREAD = "http://forum.xda-developers.com/showthread.php?t=2419287";

	/* SystemUI Broadcast */
	public static final String STATUSBAR_TASKBAR_REFRESH = THIS_PACKAGE_NAME + ".STATUSBAR_TASKBAR_REFRESH";
	public static final String STATUSBAR_TASKBAR_LAUNCH = THIS_PACKAGE_NAME + ".STATUSBAR_TASKBAR_LAUNCH";
	public static final String SHOW_OUTLINE = THIS_PACKAGE_NAME + ".SHOW_OUTLINE";
	public static final String SHOW_MULTIWINDOW_DRAGGER = THIS_PACKAGE_NAME + ".SHOW_MULTIWINDOW_DRAGGER";
	public static final String SEND_MULTIWINDOW_INFO = THIS_PACKAGE_NAME + ".SEND_MULTIWINDOW_INFO";
	public static final String SEND_MULTIWINDOW_APP_FOCUS = THIS_PACKAGE_NAME + ".SEND_MULTIWINDOW_APP_FOCUS";
	public static final String REMOVE_NOTIFICATION_RESTORE = THIS_PACKAGE_NAME + ".REMOVE_NOTIFICATION_RESTORE.";
	public static final String SEND_MULTIWINDOW_SWIPE = THIS_PACKAGE_NAME + ".SEND_MULTIWINDOW_SWIPE.";
	public static final String INTENT_APP_TOKEN = "token";
	public static final String INTENT_APP_ID = "id";
	public static final String INTENT_APP_PARAMS = "layout_paramz";
	public static final String INTENT_APP_EXTRA = "layout_extra";
	public static final String INTENT_APP_SWAP = "layout_swap";
	public static final String INTENT_APP_SNAP = "layout_snap";
	public static final String INTENT_APP_TIME = "time";
}
