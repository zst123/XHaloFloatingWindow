package com.zst.xposed.halo.floatingwindow.hooks;

import static de.robv.android.xposed.XposedHelpers.findClass;

import java.util.ArrayList;
import java.util.LinkedHashSet;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.XmlResourceParser;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupMenu;

import com.zst.xposed.halo.floatingwindow.Common;
import com.zst.xposed.halo.floatingwindow.MainXposed;
import com.zst.xposed.halo.floatingwindow.R;
import com.zst.xposed.halo.floatingwindow.helpers.AeroSnap;
import com.zst.xposed.halo.floatingwindow.helpers.Util;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class SystemUIMultiWindow {
	
	/* This class is to manage the multiwindow slider that you get in the
	 * middle of the screen with 2 split screen apps.
	 * Since we are dealing with movable and resizable windows, we make use
	 * of Aero Snap to deal with this. */
	
	private static final int MOVE_MAX_RANGE = 10;
	private static final int MIN_SIZE = 40;
	private static final int COLOR_PRESSED = 0xBB3d464d;
	private static final int COLOR_DEFAULT = 0xFF2f3238;
	
	/* System Values */
	private static Context mContext;
	private static WindowManager mWm;
	private static LayoutInflater mInflater;
	
	// Dragger Views
	private static View mViewContent;
	private static View mViewDragger;
	private static View mViewFocusIndicator;
	private static Drawable mIndicatorDrawable;
	private static WindowManager.LayoutParams mParamz;
	
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
	private static int mScreenHeight;
	private static int mScreenWidth;
	private static boolean mUseOldDraggerLocation;
	
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
					mWm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
					mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					
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
						
					},
							new IntentFilter(Common.SEND_MULTIWINDOW_SWIPE));
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
			// to tell the dragger that it was accidentally
			// removed and it should reappear at the same location
			
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
			float angle = 0;
			switch (intent.getIntExtra(Common.INTENT_APP_SNAP, AeroSnap.SNAP_NONE)) {
			case AeroSnap.SNAP_TOP:
				angle = 270;
				break;
			case AeroSnap.SNAP_BOTTOM:
				angle = 90;
				break;
			case AeroSnap.SNAP_LEFT:
				angle = 180;
				break;
			case AeroSnap.SNAP_RIGHT:
				angle = 0;
				break;
			case AeroSnap.SNAP_NONE:
				return;
			}
			if (mIndicatorDrawable == null) {
				mIndicatorDrawable = ctx.getResources().getDrawable(
						android.R.drawable.ic_media_play);
			}
			if (mViewFocusIndicator != null) {
				mViewFocusIndicator.setBackgroundDrawable(Util.getRotateDrawable(
						mIndicatorDrawable, angle));
			}
		}
	};

	private static final View.OnLongClickListener LONGPRESS_MENU = new View.OnLongClickListener() {
		@Override
		public boolean onLongClick(View v) {
			if (mIsFingerDraggingBar) return false;
			
	    	PopupMenu popup = new PopupMenu(mContext, mViewFocusIndicator);
			popup.getMenu().add("Swap Windows");
			popup.getMenu().add("Reset Positions");
			// TODO: Remove hardcode, add to strings.xml
			
			popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
				public boolean onMenuItemClick(MenuItem item) {
					if (item.getTitle().equals("Swap Windows")) {
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
					} else if (item.getTitle().equals("Reset Positions")) {
						mPixelsFromEdge = (mTopBottomSplit ? (mScreenHeight / 2) : (mScreenWidth / 2));
						mParamz.x = mTopBottomSplit ? 0 : mPixelsFromEdge;
						mParamz.y = mTopBottomSplit ? mPixelsFromEdge : 0;
						mWm.updateViewLayout(mViewContent, mParamz);
						sendWindowInfo(mTopBottomSplit, mPixelsFromEdge, false);
						// Reset dragger to middle
					} else {
						return false;
					}
					return true;
				}
			});
			popup.show();
			return false;
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
				mViewDragger.setBackgroundColor(COLOR_PRESSED);
				break;
			case MotionEvent.ACTION_MOVE:
				if (moveRangeAboveLimit(event)) {
					mIsFingerDraggingBar = true;
				}
				mPixelsFromEdge = (int) (mTopBottomSplit ? event.getRawY() : event.getRawX());
				mPixelsFromEdge = adjustPixelsFromEdge(mPixelsFromEdge, mTopBottomSplit);
				if (mTopBottomSplit) {
					//top-bottom
					mParamz.x = 0;
					mParamz.y = mPixelsFromEdge;
				} else {
					//left-right
					mParamz.x = mPixelsFromEdge;
					mParamz.y = 0;
				}
				mWm.updateViewLayout(mViewContent, mParamz);
				break;
			case MotionEvent.ACTION_UP:
				mIsFingerDraggingBar = false;
				sendWindowInfo(mTopBottomSplit, mPixelsFromEdge, false);
				mViewDragger.setBackgroundColor(COLOR_DEFAULT);
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
		}
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
		try {
			// try removing in case it wasn't already
			// so as to prevent duplicated bars
			mWm.removeView(mViewContent);
		} catch (Exception e) {
			// it is already removed
		}
		isSplitView = true;
		
		DisplayMetrics metrics = new DisplayMetrics();
		Display display = mWm.getDefaultDisplay();
		display.getMetrics(metrics);
		mScreenWidth = metrics.widthPixels;
		mScreenHeight = metrics.heightPixels;
				
		mParamz = new WindowManager.LayoutParams(
				top_bottom ? WindowManager.LayoutParams.MATCH_PARENT : WindowManager.LayoutParams.WRAP_CONTENT,
				top_bottom ? WindowManager.LayoutParams.WRAP_CONTENT : WindowManager.LayoutParams.MATCH_PARENT,
				WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
				0 | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
					WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM |
					WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
					WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
					WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
				PixelFormat.TRANSLUCENT);
		mParamz.gravity = Gravity.TOP | Gravity.LEFT;
		mParamz.privateFlags |= 0x00000040; //PRIVATE_FLAG_NO_MOVE_ANIMATION
		mParamz.x = top_bottom ? 0 : (mScreenWidth / 2);
		mParamz.y = top_bottom ? (mScreenHeight / 2) : 0;
		
		if (mUseOldDraggerLocation && mPixelsFromEdge != -1) {
			mParamz.x = top_bottom ? 0 : mPixelsFromEdge;
			mParamz.y = top_bottom ? mPixelsFromEdge : 0;
			mUseOldDraggerLocation = false;
		}
		
		XmlResourceParser parser = MainXposed.sModRes
				.getLayout(R.layout.movable_multiwindow_dragger);
		mViewContent = mInflater.inflate(parser, null);
			
		// hide the button of the other orientation
		mViewDragger = mViewContent.findViewById(top_bottom ? android.R.id.button1
				: android.R.id.button2);
		mViewDragger.setBackgroundColor(COLOR_DEFAULT);
		mViewDragger.setOnTouchListener(DRAG_LISTENER);
		mViewDragger.setOnLongClickListener(LONGPRESS_MENU);
		
		mViewFocusIndicator = mViewContent.findViewById(android.R.id.hint);
		mViewFocusIndicator.setBackgroundResource(0);
		
		final View other_view_dragger = mViewContent
				.findViewById((!top_bottom) ? android.R.id.button1 : android.R.id.button2);
		other_view_dragger.setVisibility(View.GONE);
		
		mWm.addView(mViewContent, mParamz);
	}
	
	private static void hideDragger() {
		isSplitView = false;
		mTopBottomSplit = false;
		try {
			mWm.removeView(mViewContent);
		} catch (Exception e) {
			// it is already removed
		}
	}
	
	// Check if the new dragger position is too close to the edge
	private static int adjustPixelsFromEdge(int value, boolean top_bottom) {
		if (value < MIN_SIZE) {
			return MIN_SIZE;
		} // if it is less than the minimum size, adjust it as it is too small
		
		int dragger_thickness = top_bottom ? mViewDragger.getHeight() : mViewDragger.getWidth();
		// must take thickness into account also.
		
		if (top_bottom) {
			if ((mScreenHeight - value - dragger_thickness) < MIN_SIZE) {
				return (mScreenHeight - MIN_SIZE - dragger_thickness);
				// if it is less than minimum size (opposite edge), adjust
			}
		} else {
			if ((mScreenWidth - value - dragger_thickness) < MIN_SIZE) {
				return (mScreenWidth - MIN_SIZE - dragger_thickness);
			}
		}
		final int offset_value = value - (dragger_thickness / 2);
		// calculate the position at the MIDDLE of the dragger and not the SIDE
		return (offset_value < MIN_SIZE) ? MIN_SIZE : offset_value;
	}
	
	private static void sendWindowInfo(boolean top_bottom, int pixels, boolean swap) {
		Intent intent = new Intent(Common.SEND_MULTIWINDOW_INFO);
		intent.putExtra(Common.INTENT_APP_SNAP, top_bottom); 
		// Top-Bottom or Left-Right App Splitting?
		intent.putExtra(Common.INTENT_APP_PARAMS, pixels); 
		// pixels from top/left where the dragger is
		intent.putExtra(Common.INTENT_APP_EXTRA,
				top_bottom ? mViewDragger.getHeight() : mViewDragger.getWidth()); 
		// extra space so app is not overlapped by dragger bar
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
