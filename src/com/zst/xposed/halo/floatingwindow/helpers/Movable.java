package com.zst.xposed.halo.floatingwindow.helpers;
 
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
 
public class Movable implements View.OnTouchListener {
        Window mWindow;
        LayoutParams param;
        private static Float screenX ;
    	private static Float screenY ;
    	private static Float viewX ;
    	private static Float viewY ;
    	private static Float leftFromScreen ;
    	private static Float topFromScreen ;
    	private View offsetView;
       
        public Movable(Window window){
                mWindow=window;
        		param = mWindow.getAttributes(); 

        }
        
        public Movable(Window window, View v){
        	this(window);
        	offsetView = v;
        }
        
        @Override
        public boolean onTouch(View v, MotionEvent event) {
        	switch (event.getAction()){
        	case MotionEvent.ACTION_DOWN:
        		viewX = event.getX();
    			viewY = event.getY();
        		if (offsetView != null) {
        			int[] location = {0,0};
        			offsetView.getLocationInWindow(location);
        			viewX = viewX + location[0];
        			viewY = viewY + location[1];
        		}
                return true;
        	case MotionEvent.ACTION_MOVE:
        		screenX = event.getRawX();
        		screenY = event.getRawY();
        		leftFromScreen = (screenX - viewX);
        		topFromScreen = (screenY - viewY);
        		mWindow.setGravity(Gravity.LEFT | Gravity.TOP);
        		updateView(mWindow, leftFromScreen, topFromScreen);
        		return true;
        	case MotionEvent.ACTION_UP:
        		break;
        	}
        	return false;
        }
        private void updateView(Window mWindow, float x , float y){
    		param.x = (int)x;	
    		param.y = (int)y;
    		mWindow.setAttributes(param);
    	}
}
