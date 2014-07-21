package com.zst.xposed.halo.floatingwindow.hooks;

import static de.robv.android.xposed.XposedHelpers.findClass;

import java.util.ArrayList;
import java.util.LinkedHashSet;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.RemoteException;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.zst.xposed.halo.floatingwindow.Common;
import com.zst.xposed.halo.floatingwindow.MainXposed;
import com.zst.xposed.halo.floatingwindow.helpers.AeroSnap;
import com.zst.xposed.halo.floatingwindow.helpers.MultiWindowRecentsManager;
import com.zst.xposed.halo.floatingwindow.helpers.MultiWindowViewManager;
import com.zst.xposed.halo.floatingwindow.helpers.Util;
import com.zst.xposed.halo.floatingwindow.hooks.ipc.XHFWInterface;
import com.zst.xposed.halo.floatingwindow.hooks.ipc.XHFWService;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class SystemUIMultiWindow {
	
	/* This class is to manage the multiwindow slider that you get in the
	 * middle of the screen with 2 split screen apps.
	 * Since we are dealing with movable and resizable windows, we make use
	 * of Aero Snap to deal with this. */
	
	private static final int MOVE_MAX_RANGE = 10;
	
	/* System Values */
	private static Context mContext;
	private static XHFWInterface mXHFWService;
	
	// Dragger Views
	private static MultiWindowViewManager mViewManager;
	private static MultiWindowRecentsManager mRecentsManager;
	
	// App Snap Lists
	private static LinkedHashSet<String> mTopList = new LinkedHashSet<String>();
	private static LinkedHashSet<String> mBottomList = new LinkedHashSet<String>();
	private static LinkedHashSet<String> mLeftList = new LinkedHashSet<String>();
	private static LinkedHashSet<String> mRightList = new LinkedHashSet<String>();
	
	// Window Management Values
	private static boolean isSplitView;
	private static boolean mTopBottomSplit;
	private static boolean mIsFingerDraggingBar;
	private static int mPixelsFromEdge = -1;
	private static float mPixelsFromSideX;
	private static float mPixelsFromSideY;
	private static boolean mUseOldDraggerLocation;
	private static int mColor;
	
	public static void handleLoadPackage(LoadPackageParam lpparam) {
		if (!lpparam.packageName.equals("com.android.systemui")) return;
		
		try {
			final Class<?> hookClass = findClass("com.android.systemui.SystemUIService",
					lpparam.classLoader);
			XposedBridge.hookAllMethods(hookClass, "onCreate", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					final Service thiz = (Service) param.thisObject;
					mContext = thiz.getApplicationContext();
					mXHFWService = XHFWService.retrieveService(mContext);
					mViewManager = new MultiWindowViewManager(mContext, MainXposed.sModRes,
							Util.realDp(24, mContext));
					// TODO option to change size
					
					mContext.registerReceiver(BROADCAST_RECEIVER,
							new IntentFilter(Common.SHOW_MULTIWINDOW_DRAGGER));
					mContext.registerReceiver(APP_TOUCH_RECEIVER,
							new IntentFilter(Common.SEND_MULTIWINDOW_APP_FOCUS));
					mContext.registerReceiver(new BroadcastReceiver() {
						@Override
						public void onReceive(Context context, Intent intent) {
							swipeToNextApp(intent.getStringExtra(Common.INTENT_APP_ID), // pkg name
									intent.getIntExtra(Common.INTENT_APP_SNAP, AeroSnap.SNAP_NONE),
									intent.getBooleanExtra(Common.INTENT_APP_EXTRA, false)); // direction
						}
					}, new IntentFilter(Common.SEND_MULTIWINDOW_SWIPE));
					
					mRecentsManager = new MultiWindowRecentsManager(mContext) {
						@Override
						public void onRemoveApp(String pkg_name) {
							mTopList.remove(pkg_name);
							mBottomList.remove(pkg_name);
							mLeftList.remove(pkg_name);
							mRightList.remove(pkg_name);
							showDragger(mViewManager.mPreviousFocusAppTopBottomSplit);
						}
					};
				}
			});
		} catch (Throwable e) {
			XposedBridge.log(Common.LOG_TAG + "Movable / SystemUIMultiWindow");
			XposedBridge.log(e);
		}
	}
	
	private final static BroadcastReceiver BROADCAST_RECEIVER = new BroadcastReceiver() {
		@Override
		public void onReceive(Context ctx, Intent intent) {
			String pkg_name = intent.getStringExtra(Common.INTENT_APP_ID);
			int snap_side = intent.getIntExtra(Common.INTENT_APP_SNAP, AeroSnap.SNAP_NONE);
			mUseOldDraggerLocation = intent.getBooleanExtra(Common.INTENT_APP_EXTRA, false);
			mColor = intent.getIntExtra(Common.KEY_WINDOW_RESIZING_AERO_SNAP_SPLITBAR_COLOR, 0);
			// to tell the dragger that it was accidentally
			// removed and it should reappear at the same location
			
			if (AeroSnap.UNKNOWN == snap_side) {
				ActivityManager am = (ActivityManager) mContext
						.getSystemService(Context.ACTIVITY_SERVICE);
				for (RunningTaskInfo info : am.getRunningTasks(50)) {
					if (info.topActivity.getPackageName().equals(pkg_name)) {
						if (info.numActivities > 0) {
							return;
						}
					}
				}
			}
			mTopList.remove(pkg_name);
			mBottomList.remove(pkg_name);
			mLeftList.remove(pkg_name);
			mRightList.remove(pkg_name);
			// Clean pkg name from the list if it is not already removed
			
			switch (snap_side) {
			case AeroSnap.SNAP_TOP:
				mTopList.add(pkg_name);
				break;
			case AeroSnap.SNAP_BOTTOM:
				mBottomList.add(pkg_name);
				break;
			case AeroSnap.SNAP_LEFT:
				mLeftList.add(pkg_name);
				break;
			case AeroSnap.SNAP_RIGHT:
				mRightList.add(pkg_name);
				break;
			case AeroSnap.SNAP_NONE:
				hideDragger();
				return;
			}
			if (checkIfDraggerHideNeeded(snap_side)) {
				hideDragger();
			} else {
				checkIfDraggerShowNeeded(snap_side);
			}
		}
	};
	
	// receives the current app's snap and change the focus indicator to point to the app.
	private final static BroadcastReceiver APP_TOUCH_RECEIVER = new BroadcastReceiver() {
		@Override
		public void onReceive(Context ctx, Intent intent) {
			mViewManager.setFocusedAppPosition(
					intent.getIntExtra(Common.INTENT_APP_SNAP, AeroSnap.SNAP_NONE),
					mPixelsFromEdge, mTopBottomSplit);
		}
	};

	private static final View.OnClickListener DRAGGER_MENU = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if (mIsFingerDraggingBar) return;
			
			MultiWindowViewManager.MWPopupButtons popup = 
					mViewManager.new MWPopupButtons(mViewManager.mViewContent) {
				@Override
				public void onCloseButton() {
					try {
						mXHFWService.removeAppTask(mXHFWService.getLastTaskId(), 0x0);
					} catch (RemoteException e) {
					}
					// mAm.removeTask(SystemUIReceiver.mLastTaskId, 0x0);
					// The last touched should be the one focused.
				}
				@Override
				public void onRecentsButton() {
					mRecentsManager.display(mViewManager.mViewContent);
					LinkedHashSet<String> list = null;
					switch (mViewManager.mPreviousFocusAppSide) {
					case AeroSnap.SNAP_TOP:
						list = mTopList;
						break;
					case AeroSnap.SNAP_BOTTOM:
						list = mBottomList;
						break;
					case AeroSnap.SNAP_LEFT:
						list = mLeftList;
						break;
					case AeroSnap.SNAP_RIGHT:
						list = mRightList;
						break;
					}
					mRecentsManager.refreshList(list);
				}
				@Override
				public void onSwapButton() {
					swapWindowPosition();
				}
				@Override
				public void onResetButton() {
					resetDraggerViewPosition();
				}
			};
			popup.display();
	    }   
	};
	
	private static final View.OnTouchListener DRAG_LISTENER = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (!isSplitView) {
				hideDragger();
				return false;
			}
			switch (event.getAction()){
			case MotionEvent.ACTION_DOWN:
				mViewManager.createTouchedView(mTopBottomSplit);
				break;
			case MotionEvent.ACTION_MOVE:
				if (moveRangeAboveLimit(event)) {
					mIsFingerDraggingBar = true;
				}
				mPixelsFromEdge = (int) (mTopBottomSplit ? event.getRawY() : event.getRawX());
				mPixelsFromEdge = mViewManager.adjustPixelsFromEdge(mPixelsFromEdge, mTopBottomSplit);
				mPixelsFromSideX = event.getRawX();
				mPixelsFromSideY = event.getRawY();
				mViewManager.setDraggerViewPosition(mTopBottomSplit, mPixelsFromEdge, 
						event.getRawX(), event.getRawY());
				mViewManager.setTouchedViewPosition(event.getRawX(), event.getRawY());
				break;
			case MotionEvent.ACTION_UP:
				mIsFingerDraggingBar = false;
				sendWindowInfo(mTopBottomSplit, mPixelsFromEdge, false);
				mViewManager.removeTouchedView();
				mViewManager.refreshFocusedAppPosition(mPixelsFromEdge, false);
				break;
			}
			return false;
		}
	};
	
	// check if the current window snapping is suitable for our dragger
	private static void checkIfDraggerShowNeeded(int current_app_snap) {
		if (mTopList.size() > 0 && mBottomList.size() > 0) {
			if (current_app_snap == AeroSnap.SNAP_TOP ||
				current_app_snap == AeroSnap.SNAP_BOTTOM) {
				// If the current app is left or right, it means the detected
				// apps in the lists are below the current app. 
				mTopBottomSplit = true;
				showDragger(true);
			}			
		} else if (mLeftList.size() > 0 && mRightList.size() > 0) {
			if (current_app_snap == AeroSnap.SNAP_LEFT ||
				current_app_snap == AeroSnap.SNAP_RIGHT) {
				// If the current app is top or bottom, it means the detected
				// apps in the lists are below the current app. 
				mTopBottomSplit = false;
				showDragger(false);
			}
		} else {
			return;
		}
		Intent intent = new Intent(Common.SEND_MULTIWINDOW_APP_FOCUS);
		intent.putExtra(Common.INTENT_APP_SNAP, current_app_snap);
		mContext.sendBroadcast(intent);
	}
	
	// Check if a non-spit view that corresponds to the dragger is showing
	private static boolean checkIfDraggerHideNeeded(int snap_side) {
		if (!isSplitView) return false;
		if (mTopBottomSplit) {
			if ((snap_side == AeroSnap.SNAP_BOTTOM) ||
				(snap_side == AeroSnap.SNAP_TOP)) {
				// It corresponds, no hide is needed
				return false;
			}
		} else {
			if ((snap_side == AeroSnap.SNAP_LEFT) ||
				(snap_side == AeroSnap.SNAP_RIGHT)) {
				// It corresponds, no hide is needed
				return false;
			}	
		}
		// If it doesn't correspond, we will reach here
		return true;
	}
	
	private static void showDragger(boolean top_bottom) {
		isSplitView = true;
		
		if (!(mUseOldDraggerLocation && mViewManager.mViewContent != null)) {
			// if we need to use old location, don't recreate the view again
			mViewManager.setColor(mColor);
			mViewManager.createDraggerView();
			mViewManager.mViewContent.setOnTouchListener(DRAG_LISTENER);
			mViewManager.mViewContent.setOnClickListener(DRAGGER_MENU);
		}
		
		if (mUseOldDraggerLocation && mPixelsFromEdge != -1) {
			if (top_bottom) {
				mViewManager.mContentParamz.y = (int) (mPixelsFromEdge - 
						(0.5f * mViewManager.mCircleDiameter));
				mViewManager.mContentParamz.x = (int) (mPixelsFromSideX - 
						(0.5f * mViewManager.mCircleDiameter));
			} else {
				mViewManager.mContentParamz.x = (int) (mPixelsFromEdge - 
						(0.5f * mViewManager.mCircleDiameter));
				mViewManager.mContentParamz.y = (int) (mPixelsFromSideY - 
						(0.5f * mViewManager.mCircleDiameter));
			}
			mUseOldDraggerLocation = false;
		} else {
			if (top_bottom) {
				mPixelsFromEdge = mViewManager.mScreenHeight / 2;
			} else {
				mPixelsFromEdge = mViewManager.mScreenWidth / 2;
			}
		}
		
		mViewManager.updateDraggerView(true);
	}
	
	private static void hideDragger() {
		isSplitView = false;
		mTopBottomSplit = false;
		mViewManager.removeDraggerView();
		mViewManager.removeFocusedAppOutline();
	}
	
	private static void swapWindowPosition() {
		sendWindowInfo(mTopBottomSplit, mPixelsFromEdge, true);
		// Tell apps to swap positions
		if (mTopBottomSplit) {
			final LinkedHashSet<String> old_top = mTopList;
			final LinkedHashSet<String> old_bottom = mBottomList;
			mTopList = old_bottom;
			mBottomList = old_top;
		} else {
			final LinkedHashSet<String> old_left = mLeftList;
			final LinkedHashSet<String> old_right = mRightList;
			mLeftList = old_right;
			mRightList = old_left;
		}
		// Tell outline to swap focus
		mViewManager.refreshFocusedAppPosition(mPixelsFromEdge, true);
	}
	
	public static void resetDraggerViewPosition() {
		mPixelsFromEdge = (mTopBottomSplit ? 
				(mViewManager.mScreenHeight / 2) : (mViewManager.mScreenWidth / 2));
		mViewManager.resetDraggerViewPosition();
		mViewManager.refreshFocusedAppPosition(mPixelsFromEdge, false);
		mViewManager.updateDraggerView(false);
		sendWindowInfo(mTopBottomSplit, mPixelsFromEdge, false);
	}
	
	private static void sendWindowInfo(boolean top_bottom, int pixels, boolean swap) {
		Intent intent = new Intent(Common.SEND_MULTIWINDOW_INFO);
		intent.putExtra(Common.INTENT_APP_SNAP, top_bottom); 
		// Top-Bottom or Left-Right App Splitting?
		intent.putExtra(Common.INTENT_APP_PARAMS, pixels); 
		// pixels from top/left where the dragger is
		intent.putExtra(Common.INTENT_APP_EXTRA, 0);
		// TODO: REMOVE - previously was the extra space so app is not overlapped
		// by dragger bar, but it is now unused after the new design
		intent.putExtra(Common.INTENT_APP_SWAP, swap);
		// tell app to swap position
		intent.putExtra(Common.INTENT_APP_TIME, System.currentTimeMillis());
		mContext.sendBroadcast(intent);
	}
	
	static float[] mPreviousRange = new float[2];
	private static boolean moveRangeAboveLimit(MotionEvent event) {
		final float x = event.getRawX();
		final float y = event.getRawY();
		
		boolean returnValue = false;
		if (Math.abs(mPreviousRange[0] - x) > MOVE_MAX_RANGE)
			returnValue = true;
		if (Math.abs(mPreviousRange[1] - y) > MOVE_MAX_RANGE)
			returnValue = true;

		return returnValue;
	}
	
	/**
	 * Switch to another app on swipe gesture
	 * @param current_app_pkg - Package name of App that received swipe gesture
	 * @param app_position - The Snap Position of the App that received the swipe gesture
	 * @param swiped_before - The swipe of the gesture from the left/top(before) or right/bottom(after)
	 * @return true if the swipe was successful
	 */
	private static boolean swipeToNextApp(String current_app_pkg, int app_position, boolean swiped_before) {
		ArrayList<String> list;
		
		switch (app_position) {
		case AeroSnap.SNAP_TOP:
			list = new ArrayList<String>(mTopList);
			break;
		case AeroSnap.SNAP_BOTTOM:
			list = new ArrayList<String>(mBottomList);
			break;
		case AeroSnap.SNAP_LEFT:
			list = new ArrayList<String>(mLeftList);
			break;
		case AeroSnap.SNAP_RIGHT:
			list = new ArrayList<String>(mRightList);
			break;
		case AeroSnap.SNAP_NONE:
		default:
			return false;
		}

		final int index = list.indexOf(current_app_pkg);
		if (index == -1) 
			return false;
		
		String next_app_pkg;
		
		if (!swiped_before) {
			// swiped to right/bottom
			if (index + 1 < list.size()) {
				next_app_pkg = list.get(index + 1);
			} else {
				next_app_pkg = list.get(0);
			}
		} else {
			// swiped to left/top
			if (index - 1 >= 0) {
				next_app_pkg = list.get(index - 1);
			} else {
				next_app_pkg = list.get(list.size() - 1);
			}
		}
		
		try {
			Intent next_intent = mContext.getPackageManager()
					.getLaunchIntentForPackage(next_app_pkg);
			next_intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			next_intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			next_intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			mContext.startActivity(next_intent);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
}
