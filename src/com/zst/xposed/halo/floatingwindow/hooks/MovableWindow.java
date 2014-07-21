package com.zst.xposed.halo.floatingwindow.hooks;

import com.zst.xposed.halo.floatingwindow.Common;
import com.zst.xposed.halo.floatingwindow.MainXposed;
import com.zst.xposed.halo.floatingwindow.R;
import com.zst.xposed.halo.floatingwindow.helpers.AeroSnap;
import com.zst.xposed.halo.floatingwindow.helpers.MovableOverlayView;
import com.zst.xposed.halo.floatingwindow.helpers.MultiWindowAppManager;
import com.zst.xposed.halo.floatingwindow.helpers.SwipeToNextApp;
import com.zst.xposed.halo.floatingwindow.helpers.Util;
import com.zst.xposed.halo.floatingwindow.hooks.ipc.XHFWService;
import com.zst.xposed.halo.floatingwindow.hooks.ipc.XHFWInterface;
import android.annotation.SuppressLint;
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
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
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
	static final int ID_NOTIFICATION_RESTORE = 22222222;
	static final String INTENT_APP_PKG = "pkg";

	final MainXposed mMainXposed;
	final Resources mModRes;
	final XSharedPreferences mPref;
	
	/* App ActionBar Moving Values */
	private Float screenX;
	private Float screenY;
	private Float viewX;
	private Float viewY;
	private Float leftFromScreen;
	private Float topFromScreen;

	Activity activity; // Current app activity
	boolean isHoloFloat = false; // Current app has floating flag?
	boolean mMovableWindow;
	boolean mActionBarDraggable;
	boolean mMinimizeToStatusbar;
	
	static boolean mRetainStartPosition;
	static boolean mConstantMovePosition;
	static int mPreviousOrientation;
	
	public static MovableOverlayView mOverlayView;
	public static boolean mMaximizeChangeTitleBarVisibility;

	/* AeroSnap*/
	public static AeroSnap mAeroSnap;
	public static boolean mAeroSnapChangeTitleBarVisibility;
	boolean mAeroSnapEnabled;
	int mAeroSnapDelay;
	boolean mAeroSnapSwipeApp;
	int mPreviousForceAeroSnap;


	@SuppressWarnings("static-access")
	public MovableWindow(MainXposed main, LoadPackageParam lpparam) throws Throwable {
		mMainXposed = main;
		mModRes = main.sModRes;
		mPref = main.mPref;
		
		activityHook();
		inject_dispatchTouchEvent();

		try {
			injectTriangle(lpparam);
		} catch (Exception e) {
			XposedBridge.log(Common.LOG_TAG + "Movable / inject_DecorView_generateLayout");
			XposedBridge.log(e);
		}
	}

	private void activityHook(){
		/* Initialize all the preference variables here.
		 */
		XposedBridge.hookAllMethods(Activity.class, "onCreate", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				activity = (Activity) param.thisObject;
				isHoloFloat = (activity.getIntent().getFlags() & Common.FLAG_FLOATING_WINDOW)
						== Common.FLAG_FLOATING_WINDOW;
				try {
					XHFWInterface inf = XHFWService.retrieveService(activity);
					//TODO reuse this
					// inf.setApp(activity.getBaseContext().toString(), activity.getPackageName(), isHoloFloat,
						//	activity.getIntent().getIntExtra(Common.EXTRA_SNAP_SIDE, AeroSnap.SNAP_NONE));
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (!isHoloFloat) return;
				
				mPref.reload();
				mMovableWindow = mPref.getBoolean(Common.KEY_MOVABLE_WINDOW,
						Common.DEFAULT_MOVABLE_WINDOW);
				if (!mMovableWindow) return;
				
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
				
				mAeroSnapSwipeApp = mPref.getBoolean(Common.KEY_WINDOW_RESIZING_AERO_SNAP_SWIPE_APP,
						Common.DEFAULT_WINDOW_RESIZING_AERO_SNAP_SWIPE_APP);
				mAeroSnapChangeTitleBarVisibility = mPref.getBoolean(Common.KEY_WINDOW_RESIZING_AERO_SNAP_TITLEBAR_HIDE,
						Common.DEFAULT_WINDOW_RESIZING_AERO_SNAP_TITLEBAR_HIDE);
				
				mMaximizeChangeTitleBarVisibility = mPref.getBoolean(Common.KEY_WINDOW_TITLEBAR_MAXIMIZE_HIDE,
						Common.DEFAULT_WINDOW_TITLEBAR_MAXIMIZE_HIDE);
				
				boolean splitbar_enabled = mAeroSnapEnabled ? mPref.getBoolean(Common.KEY_WINDOW_RESIZING_AERO_SNAP_SPLITBAR_ENABLED,
						Common.DEFAULT_WINDOW_RESIZING_AERO_SNAP_SPLITBAR_ENABLED) : false;
				int splitbar_color = Color.parseColor("#" + mPref.getString(Common.KEY_WINDOW_RESIZING_AERO_SNAP_SPLITBAR_COLOR,
						Common.DEFAULT_WINDOW_RESIZING_AERO_SNAP_SPLITBAR_COLOR));

				MultiWindowAppManager.setEnabled(splitbar_enabled, splitbar_color);
				
				checkIfInitialSnapNeeded(false);
			}
		});

		// re-initialize the variables when resuming as they may get replaced by another activity.
		XposedBridge.hookAllMethods(Activity.class, "onResume", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				activity = (Activity) param.thisObject;
				if (!isHoloFloat) {
					MultiWindowAppManager.appsSignalHideDragger(activity);
					// Signal to the dragger that a non-halo window is
					// currently shown and we should hide the bar now
					return;
				}
				if (mMovableWindow) {
					FrameLayout decor_view = (FrameLayout) activity.getWindow()
							.peekDecorView().getRootView();
					mOverlayView = (MovableOverlayView) decor_view.getTag(Common.LAYOUT_OVERLAY_TAG);
					decor_view.bringChildToFront(mOverlayView);
					mMainXposed.hookActionBarColor.setTitleBar(mOverlayView);
					
					activity.sendBroadcast(new Intent(Common.REMOVE_NOTIFICATION_RESTORE
							+ activity.getPackageName()));
					// send broadcast so the notification will be hidden if we 
					// un-minimize the app without using the notification itself
					
					checkIfInitialSnapNeeded(true);
				}
				
				MultiWindowAppManager.setWindow(activity.getWindow());
				// register listener for multiwindow dragger
				MultiWindowAppManager.appsRegisterListener(activity, true);
			}
		});

		// unregister the receiver for syncing window position
		XposedBridge.hookAllMethods(Activity.class, "onDestroy", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if (!isHoloFloat) return;
				unregisterLayoutBroadcastReceiver(((Activity) param.thisObject).getWindow());
				MultiWindowAppManager.appsRegisterListener((Activity) param.thisObject, false);
				// hide the resizing outline
				activity.sendBroadcast(new Intent(Common.SHOW_OUTLINE));
			}
		});
	}

	private void injectTriangle(final LoadPackageParam lpparam)
			throws Throwable {
		XposedBridge.hookAllMethods(Activity.class, "onStart", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (!isHoloFloat) return;
				if (!mMovableWindow) return;
				activity = (Activity) param.thisObject;
				Window window = (Window) activity.getWindow();

				// register the receiver for syncing window position
				registerLayoutBroadcastReceiver(activity, window);
				// set layout position from previous activity if available
				setLayoutPositioning(activity, window);

				FrameLayout decorView = (FrameLayout) window.peekDecorView().getRootView();
				if (decorView == null) return;
				// make sure the titlebar/drag-to-move-bar is not behind the statusbar
				decorView.setFitsSystemWindows(true);
				try {
					// disable resizing animation to speed up scaling (doesn't work on all roms)
					XposedHelpers.callMethod(decorView, "hackTurnOffWindowResizeAnim", true);
				} catch (Throwable e) {
				}

				mOverlayView = (MovableOverlayView) decorView.getTag(Common.LAYOUT_OVERLAY_TAG);
				for (int i = 0; i < decorView.getChildCount(); ++i) {
					final View child = decorView.getChildAt(i);
					if (child instanceof MovableOverlayView && mOverlayView != child) {
						// If our tag is different or null, then the
						// view we found should be removed by now.
						decorView.removeView(decorView.getChildAt(i));
						break;
					}
				}
				
				if (mOverlayView == null) {
					mOverlayView = new MovableOverlayView(mMainXposed, activity, mModRes, mPref, mAeroSnap);
					decorView.addView(mOverlayView, -1, MovableOverlayView.getParams());
					setTagInternalForView(decorView, Common.LAYOUT_OVERLAY_TAG, mOverlayView);
				}
				// Add our overlay view
				
				boolean is_maximized = 
						window.getAttributes().width  == ViewGroup.LayoutParams.MATCH_PARENT ||
						window.getAttributes().height == ViewGroup.LayoutParams.MATCH_PARENT;
				if ((mMaximizeChangeTitleBarVisibility && is_maximized) ||
					(mAeroSnapChangeTitleBarVisibility && AeroSnap.isSnapped())) {
					mOverlayView.setTitleBarVisibility(false);
				}
				
				mMainXposed.hookActionBarColor.setTitleBar(mOverlayView);
			}
		});
	}

	// maximize and restore the window.
	public void maximizeApp(Activity activity) {
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
			if (mMaximizeChangeTitleBarVisibility) {
				mOverlayView.setTitleBarVisibility(true);
			}
		} else {
			saveNonMaximizedLayout(activity.getWindow());
			activity.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.MATCH_PARENT);
			if (mMaximizeChangeTitleBarVisibility) {
				mOverlayView.setTitleBarVisibility(false);
			}
		}
		// after that, send a broadcast to sync the position of the window
		initAndRefreshLayoutParams(activity.getWindow(), activity, activity.getPackageName());
	}
	
	private void checkIfInitialSnapNeeded(boolean apply) {
		boolean hasExtra;
		try {
			hasExtra = activity.getIntent().hasExtra(Common.EXTRA_SNAP_SIDE);
		} catch (Exception e) {
			hasExtra = false;
		}
		if (mMovableWindow && isHoloFloat && mAeroSnap != null &&
				hasExtra) {
			final int snap = activity.getIntent().getIntExtra(Common.EXTRA_SNAP_SIDE,
					AeroSnap.SNAP_NONE);
			if (mPreviousForceAeroSnap == snap) {
				return;
			}
			if (snap != AeroSnap.SNAP_NONE) {
				layout_moved = false;
				if (apply) {
					mAeroSnap.forceSnap(snap);
					mPreviousForceAeroSnap = snap;
				}
			} else {
				mPreviousForceAeroSnap = AeroSnap.SNAP_NONE;
			}
		}
	}

	// Send the app to the back, and show a notification to restore
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	public void minimizeAndShowNotification(final Activity ac) {
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

		Notification.Builder nb = new Notification.Builder(ac)
		        .setContentTitle(title)
		        .setContentText(mModRes.getString(R.string.dnm_minimize_notif_summary))
		        .setSmallIcon(app_info.icon)
		        .setAutoCancel(true)
		        .setContentIntent(intent)
		        .setOngoing(true);
		
		Notification n;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			n = nb.build();
		} else {
			n = nb.getNotification();
		}
		
		final NotificationManager notificationManager =
		  (NotificationManager) ac.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(ID_NOTIFICATION_RESTORE, n);

		ac.moveTaskToBack(true);

		ac.registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				notificationManager.cancel(ID_NOTIFICATION_RESTORE);
				changeFocusApp(ac);
				context.unregisterReceiver(this);
			}
		}, new IntentFilter(Common.REMOVE_NOTIFICATION_RESTORE + ac.getPackageName()));
	}

	// hook the touch events to move the window and have aero snap.
	private void inject_dispatchTouchEvent() throws Throwable {
		XposedBridge.hookAllMethods(Activity.class, "dispatchTouchEvent", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if (!isHoloFloat) return;
				if (!mMovableWindow) return;

				Activity a = (Activity) param.thisObject;
				MotionEvent event = (MotionEvent) param.args[0];
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					viewX = event.getX();
					viewY = event.getY();
					changeFocusApp(a);
					MultiWindowAppManager.appsTouchSignal(a);
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
				
				if (!mActionBarDraggable || viewY >= height) {
					if (mAeroSnapSwipeApp && AeroSnap.isSnapped())
						SwipeToNextApp.onTouchEvent(param, a, event, mOverlayView.mBorderOutline);
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

	public static void setLayoutPositioning(Activity activity, Window window) {
		if (!layout_moved) return;

		if (!mRetainStartPosition) return;

		if (activity != null) {
			activity.getIntent().removeExtra(Common.EXTRA_SNAP_SIDE);
		}

		WindowManager.LayoutParams params = window.getAttributes();
		params.x = layout_x;
		params.y = layout_y;
		params.width = layout_width;
		params.height = layout_height;
		params.alpha = layout_alpha;
		params.gravity = Gravity.LEFT | Gravity.TOP;
		window.setAttributes(params);
	}

	public static void registerLayoutBroadcastReceiver(final Activity activity,
			final Window window) {
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
					setLayoutPositioning(activity, window);
					
					boolean is_maximized = mMaximizeChangeTitleBarVisibility &&
							(window.getAttributes().width  == ViewGroup.LayoutParams.MATCH_PARENT ||
							window.getAttributes().height == ViewGroup.LayoutParams.MATCH_PARENT);
					boolean isAeroSnapped = mAeroSnapChangeTitleBarVisibility && AeroSnap.isSnapped();
					if (activity != null && (is_maximized || isAeroSnapped)) {
						FrameLayout decor_view = (FrameLayout) activity.getWindow()
								.peekDecorView().getRootView();
						mOverlayView = (MovableOverlayView) decor_view.getTag(Common.LAYOUT_OVERLAY_TAG);
						mOverlayView.setTitleBarVisibility(false);
					}
				}
			}
		};
		IntentFilter filters = new IntentFilter();
		filters.addAction(Common.REFRESH_APP_LAYOUT);
		filters.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
		window.getContext().registerReceiver(br, filters);
		setTagInternalForView(window.getDecorView(), Common.LAYOUT_RECEIVER_TAG, br);
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

	private static void changeFocusApp(Activity a) {
		try {
			XHFWInterface inf = XHFWService.retrieveService(a);
			//TODO reuse this
			inf.bringAppToFront(getActivityToken(a), a.getTaskId());
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private void updateView(Window mWindow, float x, float y) {
		WindowManager.LayoutParams params = mWindow.getAttributes();
		params.x = (int) x;
		params.y = (int) y;
		mWindow.setAttributes(params);
	}
	
	private static void setTagInternalForView(View view, int key, Object object) {
		Class<?>[] classes = { Integer.class, Object.class };
		XposedHelpers.callMethod(view, "setTagInternal", classes, key, object);
		// view.setTagInternal(key, object);
	}
	
	private static IBinder getActivityToken(Activity act) {
		return (IBinder) XposedHelpers.callMethod(act, "getActivityToken");
	}
	
	private void putIBinderIntoExtras(Intent i, String key, IBinder b) {
		Class<?>[] vv = { String.class, IBinder.class };
		XposedHelpers.callMethod(i, "putExtra", vv, key, b);
		// FIXME IMPORTANT: deprecated on jelly bean
	}
}
