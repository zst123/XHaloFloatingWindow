package com.zst.xposed.halo.floatingwindow.helpers;

import com.zst.xposed.halo.floatingwindow.Common;
import com.zst.xposed.halo.floatingwindow.MainXposed;
import com.zst.xposed.halo.floatingwindow.R;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

public class MultiWindowViewManager {
	private static final int SIZE_MINIMUM = 40;

	public final Context mContext;
	public final Drawable mTouchDrawableTB;
	public final Drawable mTouchDrawableLR;
	public final Drawable mTouchDrawableTBWhite;
	public final Drawable mTouchDrawableLRWhite;
	public final int mCircleDiameter;
	
	public WindowManager mWm;
	
	// Main Circle Dragger
	public WindowManager.LayoutParams mContentParamz;
	public ImageView mViewContent;
	
	// Circle Touch Indicator
	public WindowManager.LayoutParams mTouchedParamz;
	public FrameLayout mViewTouched;
	
	// App Focus Outline
	public View mViewFocusOutline;
	public View outline;
	public boolean mPreviousFocusAppTopBottomSplit;
	public int mPreviousFocusAppSide;

	// General
	public int mColor;
	public int mScreenHeight;
	public int mScreenWidth;
	
	public MultiWindowViewManager(Context context, Resources res, int circle_size) {
		mContext = context;
		mTouchDrawableTB = res.getDrawable(R.drawable.multiwindow_dragger_press_ud);
		mTouchDrawableLR = res.getDrawable(R.drawable.multiwindow_dragger_press_lr);
		mTouchDrawableTBWhite = res.getDrawable(R.drawable.multiwindow_dragger_press_ud_white);
		mTouchDrawableLRWhite = res.getDrawable(R.drawable.multiwindow_dragger_press_lr_white);
		mCircleDiameter = circle_size;
	}
	
	public WindowManager getWM() {
		if (mWm == null) {
			mWm = (WindowManager)
					mContext.getSystemService(Context.WINDOW_SERVICE);
		}
		return mWm;
	}
	
	public View createDraggerView() {
		removeDraggerView();
		// try removing in case it wasn't already
		// so as to prevent duplicated views
		
		mViewContent = new ImageView(mContext);
		mViewContent.setImageDrawable(Util.makeCircle(mColor, mCircleDiameter));
		
		DisplayMetrics metrics = new DisplayMetrics();
		Display display = getWM().getDefaultDisplay();
		display.getMetrics(metrics);
		mScreenWidth = metrics.widthPixels;
		mScreenHeight = metrics.heightPixels;
				
		mContentParamz = new WindowManager.LayoutParams(
				mCircleDiameter,
				mCircleDiameter,
				WindowManager.LayoutParams.TYPE_PHONE,
				0 | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
					WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM |
					WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
					WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
					WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
				PixelFormat.TRANSLUCENT);
		mContentParamz.gravity = Gravity.TOP | Gravity.LEFT;
		Util.addPrivateFlagNoMoveAnimationToLayoutParam(mContentParamz);
		mContentParamz.x = (mScreenWidth / 2) - (mCircleDiameter / 2);
		mContentParamz.y = (mScreenHeight / 2) - (mCircleDiameter / 2);
		mViewContent.setLayoutParams(mContentParamz);
		return mViewContent;
	}
	
	public void updateDraggerView(boolean add) {
		if (add) {
			try {
				getWM().addView(mViewContent, mContentParamz);
			} catch (Exception e) {
				// it is already added
			}
		} else {
			try {
				getWM().updateViewLayout(mViewContent, mContentParamz);
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
			getWM().removeView(mViewContent);
		} catch (Exception e) {
			// it is already removed
		}
	}
	
	public void createTouchedView(boolean top_bottom) {
		removeTouchedView();
		// try removing in case it wasn't already
		// so as to prevent duplicated views
		
		ImageView iv = new ImageView(mContext);
		if (mColor == Color.parseColor("#" + Common.DEFAULT_WINDOW_RESIZING_AERO_SNAP_SPLITBAR_COLOR)) {
			// It is holo blue, use default samsung image
			iv.setImageDrawable(top_bottom ? mTouchDrawableTB : mTouchDrawableLR);
		} else {
			// use white image
			iv.setImageDrawable(top_bottom ? mTouchDrawableTBWhite : mTouchDrawableLRWhite);
			iv.setColorFilter(mColor, PorterDuff.Mode.MULTIPLY);
		}
		
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
		Util.addPrivateFlagNoMoveAnimationToLayoutParam(mContentParamz);
		mTouchedParamz.x = (mScreenWidth / 2) - (mCircleDiameter / 2);
		mTouchedParamz.y = (mScreenHeight / 2) - (mCircleDiameter / 2);
		mViewTouched.setLayoutParams(mTouchedParamz);
	}
	
	public void setTouchedViewPosition(float x, float y) {
		mTouchedParamz.x = (int) (x - (mTouchedParamz.width / 2));
		mTouchedParamz.y = (int) (y - (mTouchedParamz.height / 2));
		try {
			getWM().updateViewLayout(mViewTouched, mTouchedParamz);
		} catch (Exception e) {
			try {
				AlphaAnimation anim = new AlphaAnimation(0, 1);
				anim.setDuration(500);
				if (mViewTouched.getTag() instanceof ImageView) {
					((ImageView) mViewTouched.getTag()).startAnimation(anim);
				}
				getWM().addView(mViewTouched, mTouchedParamz);
			} catch (Exception e2) {
				// it is already removed
			}
		}
	}
	
	public void removeTouchedView() {
		try {
			getWM().removeView(mViewTouched);
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
		params.type = WindowManager.LayoutParams.TYPE_PHONE;
		params.x = 0;
		params.y = 0;
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
			params.gravity = Gravity.TOP | Gravity.LEFT;
			params.y = pixels_from_edge;
			/* Immersive Mode work-around. Screen Height excludes the nav bar
			 * even if in immersive mode, thus, there will be a 48dp gap between
			 * the outline and the app. */
			break;
		case AeroSnap.SNAP_NONE:
		default:
			removeFocusedAppOutline();
			return;
		}
		try {
			getWM().updateViewLayout(mViewFocusOutline, params);
		} catch (IllegalArgumentException e) {
			// view is not attached
			getWM().addView(mViewFocusOutline, params);
		}
	}
	
	public void removeFocusedAppOutline() {
		mPreviousFocusAppSide = AeroSnap.SNAP_NONE;
		try {
			getWM().removeView(mViewFocusOutline);
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
	
	public abstract class MWPopupButtons extends PopupWindow {
		final View mParent;
		final LinearLayout mView;
		final int mSize;
		public MWPopupButtons(View parent) {
			super(mContext);
			mParent = parent;
			mSize = (int) (mCircleDiameter * 1.4f);
			
			mView = new LinearLayout(mContext);
			mView.setOrientation(LinearLayout.HORIZONTAL);
			
			mView.addView(createButton(R.drawable.multiwindow_tray_swap));
			mView.addView(createButton(R.drawable.multiwindow_tray_recents));
			mView.addView(createButton(R.drawable.multiwindow_tray_reset));
	        mView.addView(createButton(R.drawable.multiwindow_tray_close));
	        setContentView(mView);
	        
	        setWindowLayoutMode(ViewGroup.LayoutParams.WRAP_CONTENT,
	        		ViewGroup.LayoutParams.WRAP_CONTENT);
	        setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
	        setAnimationStyle(0);
	        setOutsideTouchable(true);
		}
		private ImageButton createButton(final int icon_id) {
			Drawable icon = MainXposed.sModRes.getDrawable(icon_id);
			ImageButton btn = new ImageButton(mContext);
			
			btn.setPadding(0, 0, 0, 0);
			btn.setLayoutParams(createParams());
			btn.setScaleType(ImageView.ScaleType.FIT_CENTER);
			btn.setImageDrawable(icon);
			btn.setColorFilter(getIconColor(mColor), PorterDuff.Mode.SRC_ATOP);
			Util.setBackgroundDrawable(btn, Util.makeCircle(mColor, mSize));
			
			btn.setOnTouchListener(new View.OnTouchListener() {
				boolean isClick;
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					switch (event.getActionMasked()){
					case MotionEvent.ACTION_DOWN:
						v.setAlpha(0.6f);
						isClick = true;
						break;
					case MotionEvent.ACTION_UP:
						if (isClick) {
							switch (icon_id) {
							case R.drawable.multiwindow_tray_swap:
								onSwapButton();
								break;
							case R.drawable.multiwindow_tray_close:
								onCloseButton();
								break;
							case R.drawable.multiwindow_tray_recents:
								onRecentsButton();
								break;
							case R.drawable.multiwindow_tray_reset:
								onResetButton();
							}
							isClick = false;
						}
						MWPopupButtons.this.dismiss();
						break;
					case MotionEvent.ACTION_MOVE:
						break;
					default:
						isClick = false;
						v.setAlpha(1.0f);
						break;
					}
					return false;
				}
			});
			return btn;
		}
		public abstract void onSwapButton();
		public abstract void onCloseButton();
		public abstract void onRecentsButton();
		public abstract void onResetButton();
		
		private LinearLayout.LayoutParams createParams() {
			LinearLayout.LayoutParams pm = new LinearLayout.LayoutParams(mSize, mSize, 0.5f);
			final int margin = mSize / 8;
			pm.leftMargin = margin;
			pm.rightMargin = margin;
			pm.topMargin = margin;
			pm.bottomMargin = margin;
			return pm;
		}
		
		public void display() {
			if (mPreviousFocusAppTopBottomSplit) {
				mView.setOrientation(LinearLayout.HORIZONTAL);
				switch (mPreviousFocusAppSide) {
				case AeroSnap.SNAP_TOP:
					showAtLocation(mParent, Gravity.TOP | Gravity.CENTER_HORIZONTAL,
							0, mCircleDiameter-mSize-mSize);
					break;
				case AeroSnap.SNAP_BOTTOM:
					showAtLocation(mParent, Gravity.TOP | Gravity.CENTER_HORIZONTAL,
							0, mSize - (mSize / 4));
					break;
				default:
					return;
				}
			} else {
				mView.setOrientation(LinearLayout.VERTICAL);
				switch (mPreviousFocusAppSide) {
				case AeroSnap.SNAP_LEFT:
					showAtLocation(mParent, Gravity.LEFT | Gravity.CENTER_VERTICAL,
							mCircleDiameter-mSize-mSize, 0);
					break;
				case AeroSnap.SNAP_RIGHT:
					showAtLocation(mParent, Gravity.LEFT | Gravity.CENTER_VERTICAL,
							mSize , 0);
					break;
				default:
					return;
				}
			}
			// animate
			ScaleAnimation scaleup = new ScaleAnimation(0.0f, 1.0f, 0.0f, 1.0f,
					ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
					ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
			scaleup.setDuration(250);

			for(int x = 0; x < mView.getChildCount(); x++) {
				mView.getChildAt(x).startAnimation(scaleup);
			}
		}
		
		@Override 
		public void dismiss() {
			ScaleAnimation scaledown = new ScaleAnimation(1.0f, 0.0f, 1.0f, 0.0f,
					ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
					ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
			scaledown.setStartOffset(250);
			scaledown.setDuration(250);
			scaledown.setAnimationListener(new Animation.AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {}
				@Override
				public void onAnimationRepeat(Animation animation) {}
				@Override
				public void onAnimationEnd(Animation animation) {
					new Handler(mContext.getMainLooper()).postDelayed(new Runnable() {
						@Override
						public void run() {
							MWPopupButtons.super.dismiss();
						}
					}, 100);
				}
			});
			scaledown.setFillAfter(true);
			for(int x = 0; x < mView.getChildCount(); x++) {
				mView.getChildAt(x).startAnimation(scaledown);
			}
		}
		
		private int getIconColor(int color) {
			float[] hsv = new float[3];
			Color.colorToHSV(color, hsv);
			float value = hsv[2];
			if (value > 0.9f) {
				return 0xff222222;
			} else {
				return Color.WHITE;
			}
		}
		
		
	}
}
