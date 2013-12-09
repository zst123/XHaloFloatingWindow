package com.zst.xposed.halo.floatingwindow.helpers;

import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;

public class RightResizable implements View.OnTouchListener {
	
	final Window window;
	final int minSize;
	
	LayoutParams param;
	int oldW;
	int oldH;
	int distance_from_left;
	int distance_from_top;
	
	public RightResizable(Window window) {
		this.window = window;
		// Convert 100dp to px equivalent from the context
		final float scale = window.getContext().getResources().getDisplayMetrics().density;
		minSize = (int) (100 * scale + 0.5f);
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
		
		case MotionEvent.ACTION_DOWN:
			window.setGravity(Gravity.LEFT | Gravity.TOP);
			param = window.getAttributes();
			oldW = param.width;
			oldH = param.height;
			distance_from_left = param.x;
			distance_from_top = param.y;
			break;
		
		case MotionEvent.ACTION_MOVE:
			final int newX = Math.round(event.getRawX());
			final int newY = Math.round(event.getRawY());
			
			int calculatedW = newX - distance_from_left;
			int calculatedH = newY - distance_from_top;
			
			if (calculatedW > minSize) {
				param.width = calculatedW;
			}
			if (calculatedH > minSize) {
				param.height = calculatedH;
			}
			window.setAttributes(param);
			break;
		}
		return false;
	}
}
