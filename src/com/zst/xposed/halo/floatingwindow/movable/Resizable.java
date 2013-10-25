


package com.zst.xposed.halo.floatingwindow.movable;
 
import com.zst.xposed.halo.floatingwindow.MovableWindow;

import android.content.Context;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
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
                minSize = MovableWindow.dp(100, context);
                //Placed DPI method here so we don't keep calling the same method every move.
                //Just during initializing and reuse value.
        }
        

        @Override
        public boolean onTouch(View v, MotionEvent event) {
        	switch (event.getAction()){
        	
        	case MotionEvent.ACTION_DOWN:
            	window.setGravity(Gravity.LEFT | Gravity.TOP);
        		param = window.getAttributes();
                oldX = Math.round(event.getRawX());
                oldY = Math.round(event.getRawY());
                oldW = param.width;
                oldH = param.height;
                distance_from_top = param.y;
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
                window.setAttributes(param);

              //I split the if statements because if say the width is at it's minimum that shouldn't keep the height from adjusting
                //which may not be at it's minimum. Basically width and height should be independent
                return false;
        	default:
        		return false;
        	}
        }
}
