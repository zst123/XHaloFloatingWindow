package com.zst.xposed.halo.floatingwindow.helpers;

import com.zst.xposed.halo.floatingwindow.Common;
import com.zst.xposed.halo.floatingwindow.hooks.MovableWindow;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

public class MultiWindowAppManager {
	
	/* System Objects */
	private static Window mWindow;
	private static WindowManager mWindowManager;
	
	/* Values */
	private static int mColor;
	private static boolean mEnabled;
	private static boolean mHasAskedForHide;
	private static int mSnappedSide;
	// Current snapped sides are saved here
	private static int mCachedSnappedSide;
	// Current snapped sides are saved unless it is SNAP_NONE
	
	/* This class is a helper to send info of the current app to the SystemUI
	 * receiver. The SystemUI receiver will calculate the rest for us. */
	
	public static void setEnabled(boolean enable, int color) {
		mEnabled = enable;
		mColor = color;
	}
	
	public static void setWindow(Window w) {
		mWindow = w;
	}
	
	public static void appsRegisterListener(final Context context, boolean register) {
		if (!mEnabled) return;
		if (register) {
			context.registerReceiver(BROADCAST_RECEIVER, new IntentFilter(
					Common.SEND_MULTIWINDOW_INFO));
			mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
			appsSignalLaunch(context);
		} else {
			new Handler(context.getMainLooper()).postDelayed(new Runnable() {
				@Override
				public void run() {
					mHasAskedForHide = true;
					Intent intent = new Intent(Common.SHOW_MULTIWINDOW_DRAGGER);
					intent.putExtra(Common.INTENT_APP_SNAP, AeroSnap.UNKNOWN);
					intent.putExtra(Common.INTENT_APP_ID, context.getPackageName());
					context.sendBroadcast(intent);
				}
			}, 500);
			// Delay hiding the app so the system ActivityManager has enough
			// time to remove the current app from the running apps list
		}
	}
	
	static long mPreviousTimeOfReceiver;
	final static BroadcastReceiver BROADCAST_RECEIVER = new BroadcastReceiver() {
		@Override
		public synchronized void onReceive(Context ctx, Intent intent) {
			if (!mEnabled) return;
			
			if (!AeroSnap.isSnapped()) {
				return;
			}
			
			long current_time = intent.getLongExtra(Common.INTENT_APP_TIME, -1);
			if (current_time == mPreviousTimeOfReceiver) {
				// This broadcast receiver was called twice. Skip.
				return;
			}
			mPreviousTimeOfReceiver = current_time;
			
			int pixels_from_edge = intent.getIntExtra(Common.INTENT_APP_PARAMS, 100);
			int pixels_offset_from_dragger = intent.getIntExtra(Common.INTENT_APP_EXTRA, 10);
			// extra space so app is not overlapped by dragger bar
			boolean top_bottom = intent.getBooleanExtra(Common.INTENT_APP_SNAP, true);
			
			if (intent.getBooleanExtra(Common.INTENT_APP_SWAP, false)) {
				// We must swap positions
				if (top_bottom) {
					if (mSnappedSide == AeroSnap.SNAP_TOP) {
						mSnappedSide = AeroSnap.SNAP_BOTTOM;
						mCachedSnappedSide = AeroSnap.SNAP_BOTTOM;
						scaleWindow(true, false, pixels_from_edge + pixels_offset_from_dragger);
					} else if (mSnappedSide == AeroSnap.SNAP_BOTTOM) {
						mSnappedSide = AeroSnap.SNAP_TOP;
						mCachedSnappedSide = AeroSnap.SNAP_TOP;
						scaleWindow(true, true, pixels_from_edge);
					}
				} else {
					if (mSnappedSide == AeroSnap.SNAP_LEFT) {
						mSnappedSide = AeroSnap.SNAP_RIGHT;
						mCachedSnappedSide = AeroSnap.SNAP_RIGHT;
						scaleWindow(false, false, pixels_from_edge + pixels_offset_from_dragger);
					} else if (mSnappedSide == AeroSnap.SNAP_RIGHT) {
						mSnappedSide = AeroSnap.SNAP_LEFT;
						mCachedSnappedSide = AeroSnap.SNAP_LEFT;
						scaleWindow(false, true, pixels_from_edge);
					}
				}
				return;
			}
			
			if (top_bottom) {
				// Check if we are top or bottom app.
				if (mSnappedSide == AeroSnap.SNAP_TOP) {
					scaleWindow(true, true, pixels_from_edge);
				} else if (mSnappedSide == AeroSnap.SNAP_BOTTOM) {
					scaleWindow(true, false, pixels_from_edge + pixels_offset_from_dragger);
				}
			} else {
				// Check if we are left or right app.
				if (mSnappedSide == AeroSnap.SNAP_LEFT) {
					scaleWindow(false, true, pixels_from_edge);
				} else if (mSnappedSide == AeroSnap.SNAP_RIGHT) {
					scaleWindow(false, false, pixels_from_edge + pixels_offset_from_dragger);
				}
			}
		}
	};
	
	public static void appsSignalHideDragger(Context context) {
		Intent intent = new Intent(Common.SHOW_MULTIWINDOW_DRAGGER);
		intent.putExtra(Common.INTENT_APP_SNAP, AeroSnap.SNAP_NONE);
		intent.putExtra(Common.INTENT_APP_ID, context.getPackageName());
		context.sendBroadcast(intent);
		mSnappedSide = AeroSnap.SNAP_NONE;
		// save the snap because it will be overriden soon.
	}
	
	public static void appsSignalShowDragger(Context context, int snap_side) {
		if (!mEnabled) return;
		appsSignalShowDragger(context, snap_side, false);
	}
	
	private static void appsSignalShowDragger(Context context, int snap_side, boolean cached) {
		Intent intent = new Intent(Common.SHOW_MULTIWINDOW_DRAGGER);
		intent.putExtra(Common.INTENT_APP_SNAP, snap_side);
		intent.putExtra(Common.INTENT_APP_ID, context.getPackageName());
		if (cached) {
			intent.putExtra(Common.INTENT_APP_EXTRA, true);
			// tell dragger it is cached and try to use old location of bar.
		}
		intent.putExtra(Common.KEY_WINDOW_RESIZING_AERO_SNAP_SPLITBAR_COLOR, mColor);
		context.sendBroadcast(intent);
		mSnappedSide = snap_side;
		mCachedSnappedSide = snap_side;
		// save the snap because it will be overriden soon.
	}
	
	public static void appsSignalLaunch(Context context) {
		if (!mEnabled) return;
		if (!AeroSnap.isSnapped()) {
			appsSignalHideDragger(context);
		}
	}
	
	// Tell us that the app is touched
	public static void appsTouchSignal(Context context) {
		if (!mEnabled) return;
		
		if (mHasAskedForHide) {
			// the bar is now hidden, but the app is still open
			appsSignalShowDragger(context, mCachedSnappedSide, true);
			mHasAskedForHide = false;
		}
		
		Intent intent = new Intent(Common.SEND_MULTIWINDOW_APP_FOCUS);
		intent.putExtra(Common.INTENT_APP_SNAP, mSnappedSide);
		context.sendBroadcast(intent);
		// Send currently focused app's position
	}
	
	private static void scaleWindow(boolean top_bottom, boolean closest_to_edge,
			int pixels_from_edge) {
		DisplayMetrics metrics = new DisplayMetrics();
		Display display = mWindowManager.getDefaultDisplay();
		display.getMetrics(metrics);
		
		WindowManager.LayoutParams lpp = mWindow.getAttributes();
		if (top_bottom) {
			// Portrait = Top-Bottom Split
			lpp.width = metrics.widthPixels;
			lpp.height = closest_to_edge ? pixels_from_edge
					: (metrics.heightPixels - pixels_from_edge);
			lpp.gravity = Gravity.TOP | Gravity.LEFT;
			lpp.x = 0;
			lpp.y = closest_to_edge ? 0 : pixels_from_edge;
		} else {
			// Landscape = Left-Right Split
			lpp.width = closest_to_edge ? pixels_from_edge
					: (metrics.widthPixels - pixels_from_edge);
			lpp.height = metrics.heightPixels;
			lpp.gravity = Gravity.TOP | Gravity.LEFT;
			lpp.x = closest_to_edge ? 0 : pixels_from_edge;
			lpp.y = 0;
		}
		mWindow.setAttributes(lpp);
		refreshLayout();
	}
	
	private static void refreshLayout() {
		MovableWindow.initAndRefreshLayoutParams(mWindow, mWindow.getContext(), mWindow
				.getContext().getPackageName());
	}
}
