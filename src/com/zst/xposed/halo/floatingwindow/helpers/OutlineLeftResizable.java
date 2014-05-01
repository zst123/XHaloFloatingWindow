package com.zst.xposed.halo.floatingwindow.helpers;

import com.zst.xposed.halo.floatingwindow.Common;
import com.zst.xposed.halo.floatingwindow.hooks.MovableWindow;

import android.content.Context;
import android.content.Intent;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager.LayoutParams;

public class OutlineLeftResizable implements View.OnTouchListener {
	
	Window window;
	Context context;
	LayoutParams param;
	
	final int minSize;
	
	int oldX, oldY, oldW, oldH;
	int futureX, futureW, futureH;
	int distance_from_top;
	
	public OutlineLeftResizable(Context context, Window window) {
		this.context = context;
		this.window = window;
		final float scale = context.getResources().getDisplayMetrics().density;
		minSize = (int) (100 * scale + 0.5f);
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
		
		case MotionEvent.ACTION_DOWN:
			param = window.getAttributes();
			oldX = Math.round(event.getRawX() - event.getX());
			oldY = Math.round(event.getRawY() - event.getY());
			oldW = param.width;
			oldH = param.height;
			distance_from_top = param.y;
			if (oldW == ViewGroup.LayoutParams.MATCH_PARENT
					|| oldH == ViewGroup.LayoutParams.MATCH_PARENT) {
				DisplayMetrics metrics = context.getResources().getDisplayMetrics();
				oldW = (int) (metrics.widthPixels * 0.95f);
				oldH = (int) (metrics.heightPixels * 0.95f);
			}
			break;
		
		case MotionEvent.ACTION_MOVE:
			int newX = Math.round(event.getRawX());
			int newY = Math.round(event.getRawY());
			
			int calculatedW = oldW + (oldX - newX);
			int calculatedH = newY - distance_from_top;
			
			if (calculatedW > minSize) {
				futureW = calculatedW;
				futureX = newX;
			}
			if (calculatedH > minSize) {
				futureH = calculatedH;
			}
			broadcast(true);
			if (MovableWindow.mAeroSnap != null) {
				MovableWindow.mAeroSnap.restoreOldPosition();
			}
			if (MovableWindow.mMaximizeChangeTitleBarVisibility) {
				MovableWindow.mOverlayView.setTitleBarVisibility(true);
			}
			break;
		case MotionEvent.ACTION_UP:
			param.x = futureX;
			param.y = distance_from_top;
			param.width = futureW;
			param.height = futureH;
			window.getCallback().onWindowAttributesChanged(param);
			broadcast(false);
			MovableWindow.initAndRefreshLayoutParams(window, context,
					context.getApplicationInfo().packageName);
			break;
		
		}
		return false;
	}
	
	private void broadcast(boolean show) {
		Intent i = new Intent(Common.SHOW_OUTLINE);
		if (show) {
			int[] array = { futureX, distance_from_top, futureH, futureW };
			i.putExtra(Common.INTENT_APP_PARAMS, array);
		}
		context.sendBroadcast(i);
	}
}
