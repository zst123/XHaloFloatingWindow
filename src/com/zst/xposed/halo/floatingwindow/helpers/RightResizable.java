package com.zst.xposed.halo.floatingwindow.helpers;

import com.zst.xposed.halo.floatingwindow.hooks.MovableWindow;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;

public class RightResizable implements View.OnTouchListener {
	
	final Window window;
	final Context context;
	final int minSize;
	
	LayoutParams param;
	int distance_from_left;
	int distance_from_top;
	
	public RightResizable(Window window) {
		this.window = window;
		this.context = window.getContext();
		// Convert 100dp to px equivalent from the context
		final float scale = window.getContext().getResources().getDisplayMetrics().density;
		minSize = (int) (100 * scale + 0.5f);
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
		
		case MotionEvent.ACTION_DOWN:
			param = window.getAttributes();
			distance_from_left = param.x;
			distance_from_top = param.y;
			break;
		
		case MotionEvent.ACTION_MOVE:
			final int newX = Math.round(event.getRawX());
			final int newY = Math.round(event.getRawY());
			
			int calculatedW = newX - distance_from_left;
			int calculatedH = newY - distance_from_top;
			
			if (calculatedW < minSize) {
				calculatedW = minSize;
			}
			if (calculatedH < minSize) {
				calculatedH = minSize;
			}
			window.setLayout(calculatedW, calculatedH);
			MovableWindow.initAndRefreshLayoutParams(window, context,
					context.getApplicationInfo().packageName);
			if (MovableWindow.mAeroSnap != null) {
				MovableWindow.mAeroSnap.restoreOldPosition();
			}
			if (MovableWindow.mMaximizeChangeTitleBarVisibility) {
				MovableWindow.mOverlayView.setTitleBarVisibility(true);
			}
			break;
		}
		return false;
	}
}
