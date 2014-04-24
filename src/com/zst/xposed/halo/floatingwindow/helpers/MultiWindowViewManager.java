package com.zst.xposed.halo.floatingwindow.helpers;

import com.zst.xposed.halo.floatingwindow.R;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class MultiWindowViewManager {
	private static final int SIZE_MINIMUM = 40;

	public final WindowManager mWM;
	public final Context mContext;
	public final Drawable mTouchDrawableTB;
	public final Drawable mTouchDrawableLR;
	public final int mCircleDiameter;
	
	// Main Circle Dragger
	public WindowManager.LayoutParams mContentParamz;
	public ImageView mViewContent;
	
	// Circle Touch Indicator
	public WindowManager.LayoutParams mTouchedParamz;
	public FrameLayout mViewTouched;
	
	// App Focus Outline
	public View mViewFocusOutline;
	public View outline;
	boolean mPreviousFocusAppTopBottomSplit;
	int mPreviousFocusAppSide;

	// General
	public int mColor;
	public int mScreenHeight;
	public int mScreenWidth;
	
	public MultiWindowViewManager(Context context, WindowManager wm, Resources res, int circle_size) {
		mContext = context;
		mWM = wm;
		mTouchDrawableTB = res.getDrawable(R.drawable.multiwindow_dragger_press_ud);
		mTouchDrawableLR = res.getDrawable(R.drawable.multiwindow_dragger_press_lr);
		mCircleDiameter = circle_size;
	}
	
	public View createDraggerView() {
		removeDraggerView();
		// try removing in case it wasn't already
		// so as to prevent duplicated views
		
		mViewContent = new ImageView(mContext);
		mViewContent.setImageDrawable(Util.makeCircle(mColor, mCircleDiameter));
		
		DisplayMetrics metrics = new DisplayMetrics();
		Display display = mWM.getDefaultDisplay();
		display.getMetrics(metrics);
		mScreenWidth = metrics.widthPixels;
		mScreenHeight = metrics.heightPixels;
				
		mContentParamz = new WindowManager.LayoutParams(
				mCircleDiameter,
				mCircleDiameter,
				WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
				0 | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
					WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM |
					WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
					WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
					WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
				PixelFormat.TRANSLUCENT);
		mContentParamz.gravity = Gravity.TOP | Gravity.LEFT;
		mContentParamz.privateFlags |= 0x00000040; //PRIVATE_FLAG_NO_MOVE_ANIMATION
		mContentParamz.x = (mScreenWidth / 2) - (mCircleDiameter / 2);
		mContentParamz.y = (mScreenHeight / 2) - (mCircleDiameter / 2);
		mViewContent.setLayoutParams(mContentParamz);
		return mViewContent;
	}
	
	public void updateDraggerView(boolean add) {
		if (add) {
			try {
				mWM.addView(mViewContent, mContentParamz);
			} catch (Exception e) {
				// it is already added
			}
		} else {
			try {
				mWM.updateViewLayout(mViewContent, mContentParamz);
			} catch (Exception e) {
				updateDraggerView(true);
				// it is not added yet
			}
		}
	}
	public void resetDraggerViewPosition() {
		mContentParamz.x = (mScreenWidth / 2) - (mCircleDiameter / 2);
		mContentParamz.y = (mScreenHeight / 2) - (mCircleDiameter / 2);
		updateDraggerView(false);
	}
	
	public void setDraggerViewPosition(boolean top_bottom, int px_from_edge, float x, float y) {
		if (top_bottom) {
			mContentParamz.x = adjustPixelsFromEdge(
					(int) (x - (0.5f * mViewContent.getWidth())),
					!top_bottom);
			mContentParamz.y = (int) (px_from_edge - (0.5f * mViewContent.getHeight()));
		} else {
			mContentParamz.y = adjustPixelsFromEdge(
					(int) (y - (0.5f * mViewContent.getHeight())),
					!top_bottom); //
			mContentParamz.x =  (int) (px_from_edge - (0.5f * mViewContent.getWidth()));
		}
		updateDraggerView(false);
	}
	
	public void removeDraggerView() {
		try {
			mWM.removeView(mViewContent);
		} catch (Exception e) {
			// it is already removed
		}
	}
	
	public void createTouchedView(boolean top_bottom) {
		removeTouchedView();
		// try removing in case it wasn't already
		// so as to prevent duplicated views
		
		ImageView iv = new ImageView(mContext);
		iv.setImageDrawable(top_bottom ? mTouchDrawableTB : mTouchDrawableLR);
		//TODO colorize the drawable
		
		mViewTouched = new FrameLayout(mContext);
		mViewTouched.addView(iv);
		mViewTouched.setTag(iv);
		
		mTouchedParamz = new WindowManager.LayoutParams(
				mCircleDiameter * 5,
				mCircleDiameter * 5,
				WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
				0 | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
					WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM |
					WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
					WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
					WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
				PixelFormat.TRANSLUCENT);
		mTouchedParamz.gravity = Gravity.TOP | Gravity.LEFT;
		mTouchedParamz.privateFlags |= 0x00000040; //PRIVATE_FLAG_NO_MOVE_ANIMATION
		mTouchedParamz.x = (mScreenWidth / 2) - (mCircleDiameter / 2);
		mTouchedParamz.y = (mScreenHeight / 2) - (mCircleDiameter / 2);
		mViewTouched.setLayoutParams(mTouchedParamz);
	}
	
	public void setTouchedViewPosition(float x, float y) {
		mTouchedParamz.x = (int) (x - (mTouchedParamz.width / 2));
		mTouchedParamz.y = (int) (y - (mTouchedParamz.height / 2));
		try {
			mWM.updateViewLayout(mViewTouched, mTouchedParamz);
		} catch (Exception e) {
			try {
				AlphaAnimation anim = new AlphaAnimation(0, 1);
				anim.setDuration(500);
				if (mViewTouched.getTag() instanceof ImageView) {
					((ImageView) mViewTouched.getTag()).startAnimation(anim);
				}
				mWM.addView(mViewTouched, mTouchedParamz);
			} catch (Exception e2) {
				// it is already removed
			}
		}
	}
	
	public void removeTouchedView() {
		try {
			mWM.removeView(mViewTouched);
		} catch (Exception e) {
			// it is already removed
		}
	}
	
	public void refreshFocusedAppPosition(int pixels_from_edge, boolean swap_side) {
		if (mPreviousFocusAppSide == AeroSnap.SNAP_NONE) return;
		
		if (swap_side) {
			int new_side = mPreviousFocusAppSide;
			switch (mPreviousFocusAppSide) {
			case AeroSnap.SNAP_TOP:
				new_side = AeroSnap.SNAP_BOTTOM;
				break;
			case AeroSnap.SNAP_BOTTOM:
				new_side = AeroSnap.SNAP_TOP;
				break;
			case AeroSnap.SNAP_LEFT:
				new_side = AeroSnap.SNAP_RIGHT;
				break;
			case AeroSnap.SNAP_RIGHT:
				new_side = AeroSnap.SNAP_LEFT;
				break;
			}
			setFocusedAppPosition(new_side, pixels_from_edge,
					mPreviousFocusAppTopBottomSplit);
		} else {
			setFocusedAppPosition(mPreviousFocusAppSide, pixels_from_edge,
					mPreviousFocusAppTopBottomSplit);
		}
	}
	

	public void setFocusedAppPosition(int side, int pixels_from_edge, boolean top_bottom_split) {
		mPreviousFocusAppTopBottomSplit = top_bottom_split;
		mPreviousFocusAppSide = side;
		
		final int outline_thickness = Util.dp(6, mContext);
		// TODO make option to change thickness
		if (mViewFocusOutline == null) {
			mViewFocusOutline = new View(mContext);
		}
		
		Util.setBackgroundDrawable(mViewFocusOutline,
				Util.makeOutline(mColor, outline_thickness));
		
		WindowManager.LayoutParams params = new WindowManager.LayoutParams();
		params.flags = 0 | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
				WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM |
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
				WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
				WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
				WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
		params.format = PixelFormat.TRANSLUCENT;
		params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
		switch (side) {
		case AeroSnap.SNAP_TOP:
			if (!top_bottom_split) {
				removeFocusedAppOutline();
				return;
			}
			params.width = WindowManager.LayoutParams.MATCH_PARENT;
			params.height = pixels_from_edge;
			params.gravity = Gravity.TOP | Gravity.LEFT;
			break;
		case AeroSnap.SNAP_RIGHT:
			if (top_bottom_split) {
				removeFocusedAppOutline();
				return;
			}
			params.height = WindowManager.LayoutParams.MATCH_PARENT;
			params.width = (int) ((mScreenWidth - pixels_from_edge) + (outline_thickness * 0.5f));
			params.gravity = Gravity.TOP | Gravity.RIGHT;
			break;
		case AeroSnap.SNAP_LEFT:
			if (top_bottom_split) {
				removeFocusedAppOutline();
				return;
			}
			params.height = WindowManager.LayoutParams.MATCH_PARENT;
			params.width = pixels_from_edge;
			params.gravity = Gravity.TOP | Gravity.LEFT;
			break;
		case AeroSnap.SNAP_BOTTOM:
			if (!top_bottom_split) {
				removeFocusedAppOutline();
				return;
			}
			params.width = WindowManager.LayoutParams.MATCH_PARENT;
			params.height = (int) ((mScreenHeight - pixels_from_edge) + (outline_thickness * 0.5f));
			params.gravity = Gravity.BOTTOM | Gravity.LEFT;
			break;
		case AeroSnap.SNAP_NONE:
		default:
			removeFocusedAppOutline();
			return;
		}
		try {
			mWM.updateViewLayout(mViewFocusOutline, params);
		} catch (IllegalArgumentException e) {
			// view is not attached
			mWM.addView(mViewFocusOutline, params);
		}
	}
	
	public void removeFocusedAppOutline() {
		mPreviousFocusAppSide = AeroSnap.SNAP_NONE;
		try {
			mWM.removeView(mViewFocusOutline);
		} catch (Exception e) {
			// it is already removed
		}
	}
	
	// Check if the new dragger position is too close to the edge
	public int adjustPixelsFromEdge(int value, boolean top_bottom) {
		if (value < SIZE_MINIMUM) {
			return SIZE_MINIMUM;
		} // if it is less than the minimum size, adjust it as it is too small
		
		final int dragger_thickness = 0;
		// XXX: REMOVE - previously the width to adjust the 2nd window so that the
		// dragger bar doesn't overlap the window, but now unused in the new design
		
		if (top_bottom) {
			if ((mScreenHeight - value - dragger_thickness) < SIZE_MINIMUM) {
				return (mScreenHeight - SIZE_MINIMUM - dragger_thickness);
				// if it is less than minimum size (opposite edge), adjust
			}
		} else {
			if ((mScreenWidth - value - dragger_thickness) < SIZE_MINIMUM) {
				return (mScreenWidth - SIZE_MINIMUM - dragger_thickness);
			}
		}
		final int offset_value = value - (dragger_thickness / 2);
		// calculate the position at the MIDDLE of the dragger and not the SIDE
		return (offset_value < SIZE_MINIMUM) ? SIZE_MINIMUM : offset_value;
	}
	
	public void setColor(int color) {
		mColor = color;
	}
}
