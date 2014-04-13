package com.zst.xposed.halo.floatingwindow.hooks;

import com.zst.xposed.halo.floatingwindow.Common;
import com.zst.xposed.halo.floatingwindow.R;
import com.zst.xposed.halo.floatingwindow.helpers.AeroSnap;
import com.zst.xposed.halo.floatingwindow.helpers.MovableOverlayView;
import com.zst.xposed.halo.floatingwindow.helpers.MultiWindowDragger;
import com.zst.xposed.halo.floatingwindow.helpers.Util;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.content.res.XModuleResources;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class MovableWindow {

	static final String INTENT_APP_PKG = "pkg";

	static XSharedPreferences mPref;
	static XModuleResources mModRes;
	/* App ActionBar Moving Values */
	private static Float screenX;
	private static Float screenY;
	private static Float viewX;
	private static Float viewY;
	private static Float leftFromScreen;
	private static Float topFromScreen;

	static Activity activity; // Current app activity
	static boolean isHoloFloat = false; // Current app has floating flag?
	static boolean mMovableWindow;
	static boolean mActionBarDraggable;
	static boolean mRetainStartPosition;
	static boolean mConstantMovePosition;
	static boolean mMinimizeToStatusbar;
	static int mPreviousOrientation;
	
	public static MovableOverlayView mOverlayView;

	/* AeroSnap*/
	static AeroSnap mAeroSnap;
	static boolean mAeroSnapEnabled;
	static int mAeroSnapDelay;

	static final int ID_NOTIFICATION_RESTORE = 22222222;

	public static void handleLoadPackage(LoadPackageParam l, XSharedPreferences p, XModuleResources res) throws Throwable {
		mModRes = res;
		mPref = p;

		activityHook();
		inject_dispatchTouchEvent();

		try {
			injectTriangle(l);
		} catch (Exception e) {
			XposedBridge.log(Common.LOG_TAG + "Movable / inject_DecorView_generateLayout");
			XposedBridge.log(e);
		}
	}

	private static void activityHook(){
		/* Initialize all the preference variables here.
		 */
		XposedBridge.hookAllMethods(Activity.class, "onCreate", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				activity = (Activity) param.thisObject;
				mPref.reload();
				isHoloFloat = (activity.getIntent().getFlags() & Common.FLAG_FLOATING_WINDOW)
						== Common.FLAG_FLOATING_WINDOW;
				mMovableWindow = mPref.getBoolean(Common.KEY_MOVABLE_WINDOW,
						Common.DEFAULT_MOVABLE_WINDOW);
				mActionBarDraggable = mPref.getBoolean(Common.KEY_WINDOW_ACTIONBAR_DRAGGING_ENABLED,
						Common.DEFAULT_WINDOW_ACTIONBAR_DRAGGING_ENABLED);
				mRetainStartPosition = mPref.getBoolean(Common.KEY_WINDOW_MOVING_RETAIN_START_POSITION,
						Common.DEFAULT_WINDOW_MOVING_RETAIN_START_POSITION);
				mConstantMovePosition = mPref.getBoolean(Common.KEY_WINDOW_MOVING_CONSTANT_POSITION,
						Common.DEFAULT_WINDOW_MOVING_CONSTANT_POSITION);

				mPreviousOrientation = activity.getResources().getConfiguration().orientation;
				mMinimizeToStatusbar = mPref.getBoolean(Common.KEY_MINIMIZE_APP_TO_STATUSBAR,
						Common.DEFAULT_MINIMIZE_APP_TO_STATUSBAR);

				mAeroSnapEnabled = mPref.getBoolean(Common.KEY_WINDOW_RESIZING_AERO_SNAP_ENABLED,
						Common.DEFAULT_WINDOW_RESIZING_AERO_SNAP_ENABLED);
				mAeroSnapDelay = mPref.getInt(Common.KEY_WINDOW_RESIZING_AERO_SNAP_DELAY,
						Common.DEFAULT_WINDOW_RESIZING_AERO_SNAP_DELAY);
				mAeroSnap = mAeroSnapEnabled ? new AeroSnap(activity.getWindow(), mAeroSnapDelay) : null;
				
				boolean splitbar_enabled = mAeroSnapEnabled ? mPref.getBoolean(Common.KEY_WINDOW_RESIZING_AERO_SNAP_SPLITBAR_ENABLED,
						Common.DEFAULT_WINDOW_RESIZING_AERO_SNAP_SPLITBAR_ENABLED) : false;

				MultiWindowDragger.setEnabled(splitbar_enabled);
			}
		});

		// re-initialize the variables when resuming as they may get replaced by another activity.
		XposedBridge.hookAllMethods(Activity.class, "onResume", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				activity = (Activity) param.thisObject;
				if (!isHoloFloat) {
					MultiWindowDragger.appsSignalHideDragger(activity);
					// Signal to the dragger that a non-halo window is
					// currently shown and we should hide the bar now
					return;
				}
				if (mMovableWindow) {
					FrameLayout decor_view = (FrameLayout) activity.getWindow()
							.peekDecorView().getRootView();
					mOverlayView = (MovableOverlayView) decor_view.getTag(Common.LAYOUT_OVERLAY_TAG);
					decor_view.bringChildToFront(mOverlayView);
					ActionBarColorHook.setTitleBar(mOverlayView);
					
					activity.sendBroadcast(new Intent(Common.REMOVE_NOTIFICATION_RESTORE
							+ activity.getPackageName()));
					// send broadcast so the notification will be hidden if we 
					// un-minimize the app without using the notification itself
				}
				
				MultiWindowDragger.setWindow(activity.getWindow());
				// register listener for multiwindow dragger
				MultiWindowDragger.appsRegisterListener(activity, true);
				
				if (mMovableWindow && isHoloFloat && mAeroSnap != null) {
					final int snap = activity.getIntent().getIntExtra(Common.EXTRA_SNAP_SIDE,
							AeroSnap.SNAP_NONE);
					if (snap != AeroSnap.SNAP_NONE) {
						mAeroSnap.forceSnap(snap);
						//FIXME bug with whatsapp starting up normally and not snapped.
					}
					activity.getIntent().removeExtra(Common.EXTRA_SNAP_SIDE);
				}
			}
		});

		// unregister the receiver for syncing window position
		XposedBridge.hookAllMethods(Activity.class, "onDestroy", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				unregisterLayoutBroadcastReceiver(((Activity) param.thisObject).getWindow());
				MultiWindowDragger.appsRegisterListener((Activity) param.thisObject, false);
			}
		});
	}

	private static void injectTriangle(final LoadPackageParam lpparam)
			throws Throwable {
		XposedBridge.hookAllMethods(Activity.class, "onStart", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (!isHoloFloat) return;
				mPref.reload();
				if (!mMovableWindow) return;
				activity = (Activity) param.thisObject;
				Window window = (Window) activity.getWindow();

				// register the receiver for syncing window position
				registerLayoutBroadcastReceiver(window);
				// set layout position from previous activity if available
				setLayoutPositioning(window);

				FrameLayout decorView = (FrameLayout) window.peekDecorView().getRootView();
				if (decorView == null) return;
				// make sure the titlebar/drag-to-move-bar is not behind the statusbar
				decorView.setFitsSystemWindows(true);
				try {
					// disable resizing animation to speed up scaling (doesn't work on all roms)
					XposedHelpers.callMethod(decorView, "hackTurnOffWindowResizeAnim", true);
				} catch (Throwable e) {
				}

				mOverlayView = new MovableOverlayView(activity, mModRes, mPref, mAeroSnap);
				decorView.addView(mOverlayView, -1, MovableOverlayView.getParams());
				decorView.setTagInternal(Common.LAYOUT_OVERLAY_TAG, mOverlayView);
				// Add our overlay view
				
				ActionBarColorHook.setTitleBar(mOverlayView);
			}
		});
	}

	// maximize and restore the window.
	public static void maximizeApp(Activity activity) {
		if ((activity.getWindow().getAttributes().width  == ViewGroup.LayoutParams.MATCH_PARENT) ||
			(activity.getWindow().getAttributes().height == ViewGroup.LayoutParams.MATCH_PARENT)) {
			if (AeroSnap.isSnapped()) {
				// we need to maximize instead of restoring since it is snapped to the edge
				mAeroSnap.restoreOldPositionWithoutRefresh();
				// dont refresh since we need to maximize it again
				saveNonMaximizedLayout(activity.getWindow());
				// save our unsnapped position, then maximize
				activity.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
						ViewGroup.LayoutParams.MATCH_PARENT);
				return;
			}
			restoreNonMaximizedLayout(activity.getWindow());
		} else {
			saveNonMaximizedLayout(activity.getWindow());
			activity.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.MATCH_PARENT);
		}
		// after that, send a broadcast to sync the position of the window
		initAndRefreshLayoutParams(activity.getWindow(), activity, activity.getPackageName());
	}

	// Send the app to the back, and show a notification to restore
	public static void minimizeAndShowNotification(final Activity ac) {
		if (!mMinimizeToStatusbar) {
			ac.moveTaskToBack(true);
			return;
		}

		Intent i = new Intent(Common.REMOVE_NOTIFICATION_RESTORE + ac.getPackageName());
		ApplicationInfo app_info = ac.getApplication().getApplicationInfo();
		PendingIntent intent = PendingIntent.getBroadcast(ac, 0, i,
				PendingIntent.FLAG_UPDATE_CURRENT);
		String title = String.format(mModRes.getString(R.string.dnm_minimize_notif_title),
        		app_info.loadLabel(ac.getPackageManager()));

		Notification n  = new Notification.Builder(ac)
		        .setContentTitle(title)
		        .setContentText(mModRes.getString(R.string.dnm_minimize_notif_summary))
		        .setSmallIcon(app_info.icon)
		        .setAutoCancel(true)
		        .setContentIntent(intent)
		        .setOngoing(true)
		        .getNotification();

		final NotificationManager notificationManager =
		  (NotificationManager) ac.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(ID_NOTIFICATION_RESTORE, n);

		ac.moveTaskToBack(true);

		ac.registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				notificationManager.cancel(ID_NOTIFICATION_RESTORE);
				Intent broadcast = new Intent(Common.CHANGE_APP_FOCUS);
				broadcast.putExtra(Common.INTENT_APP_TOKEN, ac.getActivityToken());
				broadcast.putExtra(Common.INTENT_APP_ID, ac.getTaskId());
				context.sendBroadcast(broadcast);
				context.unregisterReceiver(this);
			}
		}, new IntentFilter(Common.REMOVE_NOTIFICATION_RESTORE + ac.getPackageName()));
	}

	// hook the touch events to move the window and have aero snap.
	private static void inject_dispatchTouchEvent() throws Throwable {
		XposedBridge.hookAllMethods(Activity.class, "dispatchTouchEvent", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if (!mMovableWindow) return;

				Activity a = (Activity) param.thisObject;
				boolean isHoloFloat = (a.getIntent().getFlags() & Common.FLAG_FLOATING_WINDOW) == Common.FLAG_FLOATING_WINDOW;
				if (!isHoloFloat) return;

				MotionEvent event = (MotionEvent) param.args[0];
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					viewX = event.getX();
					viewY = event.getY();
					changeFocusApp(a);
					MultiWindowDragger.appsTouchSignal(a);
					if (a.getWindow().getAttributes().gravity != (Gravity.LEFT | Gravity.TOP)) {
						// Fix First Resize moving into corner
						screenX = event.getRawX();
						screenY = event.getRawY();
						leftFromScreen = (screenX - viewX);
						topFromScreen = (screenY - viewY);
						a.getWindow().setGravity(Gravity.LEFT | Gravity.TOP);
						updateView(a.getWindow(), leftFromScreen, topFromScreen);
					}
					break;
				case MotionEvent.ACTION_MOVE:
					if (mActionBarDraggable) {
					ActionBar ab = a.getActionBar();
					int height = (ab != null) ? ab.getHeight() : Util.dp(48, a.getApplicationContext());

					if (viewY < height) {
						screenX = event.getRawX();
						screenY = event.getRawY();
						leftFromScreen = (screenX - viewX);
						topFromScreen = (screenY - viewY);
						Window mWindow = a.getWindow();
						mWindow.setGravity(Gravity.LEFT | Gravity.TOP);
						updateView(mWindow, leftFromScreen, topFromScreen);
						initAndRefreshLayoutParams(a.getWindow(), a.getBaseContext(), activity.getPackageName());
					}
					}
					break;
				}
				ActionBar ab = a.getActionBar();
				int height = (ab != null) ? ab.getHeight() : Util.dp(48, a.getApplicationContext());
				if (viewY < height && mAeroSnap != null && mActionBarDraggable) {
					mAeroSnap.dispatchTouchEvent(event);
				}
			}
		});
	}

	/* (Start) Layout Position Method Helpers */
	static boolean layout_moved;
	static int layout_x;
	static int layout_y;
	static int layout_width;
	static int layout_height;
	static float layout_alpha;

	static int[] old_layout;

	private static boolean saveNonMaximizedLayout(Window window) {
		final WindowManager.LayoutParams params = window.getAttributes();
		int[] layout = { params.x, params.y, params.width, params.height };
		old_layout = layout;
		return true;
	}

	private static boolean restoreNonMaximizedLayout(Window window) {
		WindowManager.LayoutParams params = window.getAttributes();
		params.x = old_layout[0];
		params.y = old_layout[1];
		params.width = old_layout[2];
		params.height = old_layout[3];
		params.gravity = Gravity.LEFT | Gravity.TOP;
		window.setAttributes(params);
		return true;
	}

	private static boolean initLayoutPositioning(Window window, boolean force) {
		if (!mRetainStartPosition && !force)
			return false;

		final WindowManager.LayoutParams params = window.getAttributes();
		layout_moved = true;
		layout_x = params.x;
		layout_y = params.y;
		layout_width = params.width;
		layout_height = params.height;
		layout_alpha = params.alpha;
		return true;
	}

	private static void setLayoutPositioning(Window window) {
		if (!layout_moved) return;

		if (!mRetainStartPosition) return;

		WindowManager.LayoutParams params = window.getAttributes();
		params.x = layout_x;
		params.y = layout_y;
		params.width = layout_width;
		params.height = layout_height;
		params.alpha = layout_alpha;
		params.gravity = Gravity.LEFT | Gravity.TOP;
		window.setAttributes(params);
	}

	private static void registerLayoutBroadcastReceiver(final Window window) {
		if (!(mRetainStartPosition || mConstantMovePosition))
			return;

		BroadcastReceiver br = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (intent.getAction().equals(Intent.ACTION_CONFIGURATION_CHANGED)) {
					Configuration config = window.getContext().getResources().getConfiguration();
					if (config.orientation != mPreviousOrientation) {
						WindowManager.LayoutParams paramz = window.getAttributes();
						final int old_x = paramz.x;
						final int old_y = paramz.y;
						final int old_height = paramz.height;
						final int old_width = paramz.width;
						paramz.x = old_y;
						paramz.y = old_x;
						paramz.width = old_height;
						paramz.height = old_width;
						window.setAttributes(paramz);
						mPreviousOrientation = config.orientation;
					}
					return;
				}
				if (intent.getStringExtra(INTENT_APP_PKG).equals(
						window.getContext().getApplicationInfo().packageName)) {
					setLayoutPositioning(window);
				}
			}
		};
		IntentFilter filters = new IntentFilter();
		filters.addAction(Common.REFRESH_APP_LAYOUT);
		filters.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
		window.getContext().registerReceiver(br, filters);
		window.getDecorView().setTagInternal(Common.LAYOUT_RECEIVER_TAG, br);
	}

	private static void unregisterLayoutBroadcastReceiver(Window window) {
		if (!(mRetainStartPosition || mConstantMovePosition))
			return;

		try {
			BroadcastReceiver br = (BroadcastReceiver) window.getDecorView().getTag(
					Common.LAYOUT_RECEIVER_TAG);
			window.getContext().unregisterReceiver(br);
		} catch (Exception e) {
		}
	}

	public static void initAndRefreshLayoutParams(Window w, Context ctx, String pkg) {
		initAndRefreshLayoutParams(w, ctx, pkg, false);
	}
	
	public static void initAndRefreshLayoutParams(Window w, Context ctx, String pkg, boolean force) {
		if (initLayoutPositioning(w, false)) {
			refreshLayoutParams(ctx, pkg);
		} else if (force) {
			if (initLayoutPositioning(w, true)) {
				WindowManager.LayoutParams params = w.getAttributes();
				params.x = layout_x;
				params.y = layout_y;
				params.width = layout_width;
				params.height = layout_height;
				params.alpha = layout_alpha;
				params.gravity = Gravity.LEFT | Gravity.TOP;
				w.setAttributes(params);
			}
		}
	}
	private static void refreshLayoutParams(Context ctx, String pkg) {
		Intent intent = new Intent(Common.REFRESH_APP_LAYOUT);
		intent.putExtra(INTENT_APP_PKG, pkg);
		intent.setPackage(pkg);
		// set package so this is broadcasted only to our own package
		ctx.sendBroadcast(intent);
	}
	/* (End) Layout Position Method Helpers */

	private static void changeFocusApp(Activity a) throws Throwable {
		Intent i = new Intent(Common.CHANGE_APP_FOCUS);
		i.putExtra(Common.INTENT_APP_TOKEN, a.getActivityToken());
		i.putExtra(Common.INTENT_APP_ID, a.getTaskId());
		a.sendBroadcast(i);
	}

	private static void updateView(Window mWindow, float x, float y) {
		WindowManager.LayoutParams params = mWindow.getAttributes();
		params.x = (int) x;
		params.y = (int) y;
		mWindow.setAttributes(params);
	}
}
