package com.zst.xposed.halo.floatingwindow.helpers;

import com.zst.xposed.halo.floatingwindow.hooks.MovableWindow;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
 
public class Resizable implements View.OnTouchListener {
       
        Window window;
        Context context;
        LayoutParams param;
        int oldX;
        int oldY;
        int oldW;
        int oldH;
        int distance_from_top;
        int minSize;
        
        public Resizable(Context context, Window window){
                this.context = context;
                this.window = window;
                // Convert 100dp to px equivalent from the context
                final float scale = context.getResources().getDisplayMetrics().density;
                minSize = (int) (100 * scale + 0.5f);
        }
        

        @Override
        public boolean onTouch(View v, MotionEvent event) {
        	switch (event.getAction()){
        	
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
                return false;
                
        	case MotionEvent.ACTION_MOVE:
        		int newX = Math.round(event.getRawX());
                int newY = Math.round(event.getRawY());
                
                int calculatedW = oldW + (oldX - newX);
                int calculatedH = newY - distance_from_top;
                
                if (calculatedW > minSize){
                        param.width = calculatedW;
                        param.x = newX;
                }
                if(calculatedH > minSize){
                        param.height = calculatedH;
                        //Commented y because we never change it so no reason to reset it
                        //param.y = distance_from_top;
                }
                window.getCallback().onWindowAttributesChanged(param);
                MovableWindow.initAndRefreshLayoutParams(window, context,
    					context.getApplicationInfo().packageName);
                if (MovableWindow.mAeroSnap != null) {
    				MovableWindow.mAeroSnap.restoreOldPosition();
    			}
                if (MovableWindow.mMaximizeChangeTitleBarVisibility) {
    				MovableWindow.mOverlayView.setTitleBarVisibility(true);
    			}
              //I split the if statements because if say the width is at it's minimum that shouldn't keep the height from adjusting
                //which may not be at it's minimum. Basically width and height should be independent
                return false;
        	default:
        		return false;
        	}
        }
}
