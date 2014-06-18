package com.zst.xposed.halo.floatingwindow.helpers;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.zst.xposed.halo.floatingwindow.Common;
import com.zst.xposed.halo.floatingwindow.hooks.MovableWindow;

public class AeroSnap {
	
	public final static int SNAP_NONE = 0;
	public final static int SNAP_LEFT = 1;
	public final static int SNAP_TOP = 2;
	public final static int SNAP_RIGHT = 3;
	public final static int SNAP_BOTTOM = 4;
	
	public final static int UNKNOWN = -10000;
	final static int MOVE_MAX_RANGE = 10;
	
	final Window mWindow;
	final Handler mHandler;
	final Context mContext;
	final int mDelay;
	
	static boolean mSnapped;
	static int mSnap = SNAP_NONE;
	
	Runnable mRunnable;
	int mRange = 100;
	int mSensitivity = 50;
	int[] mSnapParam = new int[3]; // w,h,g
	int[] mOldParam = new int[2]; // w,h
	int mScreenHeight;
	int mScreenWidth;
	static int[] mOldLayout;
	boolean mTimeoutRunning;
	boolean mTimeoutDone;
	boolean mRestorePosition;
	boolean mChangedPreviousRange;
	float[] mPreviousRange = new float[2];
	
	/**
	 * An Aero Snap Class to check if the current pointer's coordinates
	 * are in range of the snap region.
	 */
	// TODO : Clean up codes, make it more customizable
	
	
	public AeroSnap(Window window, int delay) {
		mWindow = window;
		mContext = window.getContext();
		mHandler = new Handler();
		mDelay = delay;
		refreshScreenSize();
	}
	
	public void dispatchTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_UP:
			if (!mSnapped) {
				finishSnap(isValidSnap() && mTimeoutDone);
			}
			discardTimeout();
			mChangedPreviousRange = false;
			break;
		case MotionEvent.ACTION_DOWN:
			if (!mChangedPreviousRange) {
				mPreviousRange[0] = event.getRawX();
				mPreviousRange[1] = event.getRawY();
				mChangedPreviousRange = true;
			}
			refreshScreenSize();
			break;
		case MotionEvent.ACTION_MOVE:
			if (mRestorePosition && moveRangeAboveLimit(event)) {
				restoreOldPosition();
			}
			showSnap((int) event.getRawX(), (int) event.getRawY());
			
		}
	}
	
	// check if it is moved out of the snap and not just accidently moved a few px
	private boolean moveRangeAboveLimit(MotionEvent event) {
		final float x = event.getRawX();
		final float y = event.getRawY();
		
		boolean returnValue = false;
		if (Math.abs(mPreviousRange[0] - x) > MOVE_MAX_RANGE)
			returnValue = true;
		if (Math.abs(mPreviousRange[1] - y) > MOVE_MAX_RANGE)
			returnValue = true;

		return returnValue;
	}
	
	private void showSnap(int x, int y) {
		initSnappable(x, y);
		calculateSnap();
		
		if (isValidSnap()) {
			broadcastShowWithTimeout();
		} else {
			broadcastHide(mContext);
		}
		
	}
	
	// do the snap by setting the variables and hiding the snap preview
	private void finishSnap(boolean apply) {
		if (apply) {
			if (saveOldPosition()) {
				mRestorePosition = true;
			}
			WindowManager.LayoutParams lpp = mWindow.getAttributes();
			lpp.width = mSnapParam[0];
			lpp.height = mSnapParam[1];
			lpp.gravity = mSnapParam[2];
			lpp.x = (lpp.gravity == Gravity.RIGHT) ? (mScreenWidth / 2) : 0;
			lpp.y = (lpp.gravity == Gravity.BOTTOM) ? (mScreenHeight / 2) : 0;
			mWindow.setAttributes(lpp);
			MultiWindowAppManager.appsSignalShowDragger(mContext, mSnap);
			if (MovableWindow.mAeroSnapChangeTitleBarVisibility) {
				MovableWindow.mOverlayView.setTitleBarVisibility(false);
			}
		} else {
			mSnap = SNAP_NONE;
		}
		refreshLayout();
		broadcastHide(mContext);
	}
	
	/**
	 * Forces the window to snap to this side programatically without user input
	 * @param side - Side of the screen to snap to.
	 */
	public void forceSnap(int side) {
		if (side == SNAP_NONE) {
			restoreOldPosition();
			return;
		}
		if (isSnapped()) {
			restoreOldPositionWithoutRefresh();
			MultiWindowAppManager.appsSignalHideDragger(mContext);
		}
		mSnap = side;
		calculateSnap();
		finishSnap(true);
	}
	
	/**
	 * Initializes the current screen size with respect to rotation.
	 */
	private void refreshScreenSize() {
		final WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
		final DisplayMetrics metrics = new DisplayMetrics();
		wm.getDefaultDisplay().getMetrics(metrics);
		
		mScreenHeight = metrics.heightPixels;
		mScreenWidth = metrics.widthPixels;
	}
	
	/**
	 * Checks the range of the touch coordinates and set the respective side.
	 */
	private boolean initSnappable(int x, int y) {
		if ((Math.abs(mOldParam[0] - x) > mSensitivity) ||
			(Math.abs(mOldParam[1] - y) > mSensitivity)) {
			mOldParam[0] = x;
			mOldParam[1] = y;
			discardTimeout();
			return false;
		}
		mOldParam[0] = x;
		mOldParam[1] = y;
		
		if (x < mRange) {
			mSnap = SNAP_LEFT;
		} else if (x > (mScreenWidth - mRange)) {
			mSnap = SNAP_RIGHT;
		} else if (y < mRange) {
			mSnap = SNAP_TOP;
		} else if (y > (mScreenHeight - mRange)) {
			mSnap = SNAP_BOTTOM;
		} else {
			mSnap = SNAP_NONE;
			return false;
		}
		return true;
	}
	
	private boolean isValidSnap() {
		return 	(mSnapParam[0] != UNKNOWN) &&
				(mSnapParam[1] != UNKNOWN) &&
				(mSnapParam[2] != UNKNOWN);
	}
	
	// svae the position so we can restore it later
	private boolean saveOldPosition() {
		if (mRestorePosition) return true;
		if (mSnapped) {
			return (mSnap == SNAP_NONE) || (mTimeoutRunning);
		}
		mSnapped = true;
		final WindowManager.LayoutParams params = mWindow.getAttributes();
		int[] layout = { params.x, params.y, params.width, params.height };
		mOldLayout = layout;
		return true;
	}
	
	// undo the snap when moving the window out of the snap region
	public boolean restoreOldPosition() {
		if (!mSnapped) return false;
		restoreOldPositionWithoutRefresh();
		refreshLayout();
		MultiWindowAppManager.appsSignalHideDragger(mContext);
		return true;
	}
	
	public void restoreOldPositionWithoutRefresh() {
		if (!mSnapped) return;
		WindowManager.LayoutParams params = mWindow.getAttributes();
		params.x = mOldLayout[0];
		params.y = mOldLayout[1];
		params.width = mOldLayout[2];
		params.height = mOldLayout[3];
		params.gravity = Gravity.LEFT | Gravity.TOP;
		mWindow.setAttributes(params);
		mSnapped = false;
		mRestorePosition = false;
		if (MovableWindow.mAeroSnapChangeTitleBarVisibility) {
			MovableWindow.mOverlayView.setTitleBarVisibility(true);
		}
	}
	
	// create a snap positioning based on the range of our touch coordinates
	private void calculateSnap() {
		switch (mSnap) {
		case SNAP_LEFT:
			mSnapParam[0] = (mScreenWidth / 2) + 1;
			mSnapParam[1] = ViewGroup.LayoutParams.MATCH_PARENT;
			mSnapParam[2] = Gravity.TOP | Gravity.LEFT;
			break;
		case SNAP_RIGHT:
			mSnapParam[0] = (mScreenWidth / 2) + 1;
			mSnapParam[1] = ViewGroup.LayoutParams.MATCH_PARENT;
			mSnapParam[2] = Gravity.RIGHT;
			break;
		case SNAP_TOP:
			mSnapParam[0] = ViewGroup.LayoutParams.MATCH_PARENT;
			mSnapParam[1] = (mScreenHeight / 2) + 1;
			mSnapParam[2] = Gravity.TOP;
			break;
		case SNAP_BOTTOM:
			mSnapParam[0] = ViewGroup.LayoutParams.MATCH_PARENT;
			mSnapParam[1] = (mScreenHeight / 2) + 1;
			mSnapParam[2] = Gravity.BOTTOM;
			break;
		case SNAP_NONE:
			mSnapParam[0] = UNKNOWN;
			mSnapParam[1] = UNKNOWN;
			mSnapParam[2] = UNKNOWN;
		}
	}
	
	// send broadcast to sync the windows
	private void refreshLayout() {
		MovableWindow.initAndRefreshLayoutParams(mWindow, mContext, mContext.getPackageName(), true);
	}
	
	// stop the handler from continuing
	private void discardTimeout() {
		mTimeoutDone = false;
		mTimeoutRunning = false;
		mHandler.removeCallbacks(mRunnable);
	}
	
	// send broadcast after the snap delay
	private void broadcastShowWithTimeout() {
		if (mTimeoutRunning) return;
		if (mRunnable == null) {
			mRunnable = new Runnable() {
				@Override
				public void run() {
					broadcastShow(mContext,mSnapParam[0],mSnapParam[1],mSnapParam[2]);
					mHandler.postDelayed(new Runnable() {
						@Override
						public void run() {
							mTimeoutRunning = false;
							mTimeoutDone = true;
							// Delay to offset the lag because broadcastShow
							// will have some delay in inflating the view.
						}
					}, 250);
				}
			};
		}
		mTimeoutRunning = true;
		mHandler.postDelayed(mRunnable, mDelay);
	}
	
	private void broadcastShow(Context ctx, int w, int h, int g) {
		Intent i = new Intent(Common.SHOW_OUTLINE);
		int[] array = { w, h, g };
		i.putExtra(Common.INTENT_APP_SNAP, array);
		ctx.sendBroadcast(i);
	}
	
	private void broadcastHide(Context ctx) {
		ctx.sendBroadcast(new Intent(Common.SHOW_OUTLINE));
	}
	
	public static boolean isSnapped() {
		return mSnapped;
	}
}
