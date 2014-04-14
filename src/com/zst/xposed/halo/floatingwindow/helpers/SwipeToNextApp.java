package com.zst.xposed.halo.floatingwindow.helpers;

import com.zst.xposed.halo.floatingwindow.Common;

import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;

public class SwipeToNextApp {
	
	// FIXME : Make the swiping work with Split Bar disabled too.
	
	static float mTouchStartingX;
	static float mTouchMovePrecent;
	static boolean mTouchDirectionLeft;
	
	private final static int GRADIENT_COLORS[] = { 0xB1bdbdbd , 0 };
	private final static GradientDrawable sLeft = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, GRADIENT_COLORS);
	private final static GradientDrawable sRight = new GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT, GRADIENT_COLORS);
	
	/**
	 * @param param
	 * @param activity - Activity of the Touch Event
	 * @param event - MotionEvent of the Touch Event
	 * @param view - ImageView to show our gradient swipe feedback
	 */
	public static void onTouchEvent(MethodHookParam param, Activity activity, MotionEvent event, ImageView view) {
		if (event.getPointerCount() >= 3)
			return;
		
		switch (event.getActionMasked()) {
		case MotionEvent.ACTION_DOWN:
			mTouchStartingX = event.getRawX();  		
			break;
		case MotionEvent.ACTION_MOVE:
			if (event.getPointerCount() == 2) {
				float amount_move = mTouchStartingX - event.getRawX();
				mTouchDirectionLeft = amount_move < 0;
				mTouchMovePrecent = Math.abs(amount_move / activity.getResources().getDisplayMetrics().widthPixels);
				if (mTouchMovePrecent >= 0.5f) {
					param.setResult(Boolean.TRUE);
					// Consume the touch event by returning true.
					view.setImageDrawable(putAlpha(mTouchDirectionLeft ? sLeft : sRight, 1.0f));
				} else {
					view.setImageDrawable(putAlpha(mTouchDirectionLeft ? sLeft : sRight, (mTouchMovePrecent / 0.5f)));
				}
				
				if (mTouchMovePrecent >= 0.3f) {
					param.setResult(Boolean.TRUE);
				}				
			}
			break;
		case MotionEvent.ACTION_UP:
			view.setImageDrawable(null);
			if (mTouchMovePrecent > 0.5f) {
				appsSendSwipe(activity, mTouchDirectionLeft);
				param.setResult(Boolean.TRUE);
				// Consume the touch event by returning true.
			}
			mTouchMovePrecent = 0.0f;
			mTouchStartingX = 0.0f;
		}
	}
	
	private static Drawable putAlpha(Drawable d, float alpha_percentage) {
		if (alpha_percentage > 1.0f) {
			alpha_percentage = 1.0f;
		}
		if (alpha_percentage < 0.0f) {
			alpha_percentage = 0.0f;
		}
		d.setAlpha((int) (alpha_percentage * 255));
		return d;
	}
	
	/**
	 * Swipe the app left or right to switch apps
	 * @param context - App Context
	 * @param swipe_before - Direction of swipe <br>
	 * 						 [Is the app swiped left (before; true) or right (after; false) ]
	 */
	public static void appsSendSwipe(final Context context, final boolean swipe_before) {
		Intent i = new Intent(Common.SEND_MULTIWINDOW_SWIPE);
		i.putExtra(Common.INTENT_APP_EXTRA, swipe_before);
		i.putExtra(Common.INTENT_APP_ID, context.getPackageName());
		i.putExtra(Common.INTENT_APP_SNAP, AeroSnap.isSnapped() ? AeroSnap.mSnap : 0);
		context.sendBroadcast(i);
	}
}
