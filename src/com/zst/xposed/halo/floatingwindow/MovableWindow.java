package com.zst.xposed.halo.floatingwindow;

import static de.robv.android.xposed.XposedHelpers.findClass;

import com.zst.xposed.halo.floatingwindow.movable.Movable;
import com.zst.xposed.halo.floatingwindow.movable.Resizable;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.XModuleResources;
import android.content.res.XmlResourceParser;
import android.os.Binder;
import android.os.IBinder;
import android.os.ServiceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.IWindowManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class MovableWindow implements IXposedHookLoadPackage,IXposedHookZygoteInit,IXposedHookInitPackageResources {
	private static final String CHANGE_APP_FOCUS = "com.zst.xposed.halo.floatingwindow.CHANGE_APP_FOCUS";
	static XModuleResources modRes;
	private static Float screenX ;
	private static Float screenY ;
	private static Float viewX ;
	private static Float viewY ;
	private static Float leftFromScreen ;
	private static Float topFromScreen ;

	private static IWindowManager wm;
	private static ActivityManager am;
	private static Context mSystemContext; //System Lockscreen Context
	private static Activity activity; //Current app activity
	static boolean isHoloFloat = false; //Current app has floating flag?
	
	static ImageView triangle;
	static View overlayView;
	private static String MODULE_PATH = null;
	private static XSharedPreferences pref;
	
	@Override
	public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {
		modRes = XModuleResources.createInstance(MODULE_PATH, resparam.res);
	}

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		MODULE_PATH = startupParam.modulePath;		
		pref = new XSharedPreferences(Res.MY_PACKAGE_NAME,Res.MY_PACKAGE_NAME);
	}
	
	@Override
	public void handleLoadPackage(LoadPackageParam l) throws Throwable {
		focusChangeContextFinder(l);
		onCreateHook();
		inject_dispatchTouchEvent();

		try{
			inject_DecorView_generateLayout(l);
		} catch (Exception e) { 
			XposedBridge.log("XHaloFloatingWindow-ERROR(DecorView): " + e.toString());
		}
		}

	private static void focusChangeContextFinder(LoadPackageParam l) throws Throwable{
		if (! l.packageName.equals("com.android.systemui")) return;
		Class<?> hookClass = findClass("com.android.systemui.SystemUIService", l.classLoader);
		XposedBridge.hookAllMethods(hookClass,"onCreate",new XC_MethodHook(){
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				Service thiz = (Service) param.thisObject;
				mSystemContext = thiz.getApplicationContext();
				//Gets SystemUI Context which has 
				IntentFilter filters = new IntentFilter();
				filters.addAction(CHANGE_APP_FOCUS);
				mSystemContext.registerReceiver(mIntentReceiver, filters, null, null);
 			}
		});
	}

	private final static BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	String action = intent.getAction();
        	if (!CHANGE_APP_FOCUS.equals(action)) return;
        	
        	wm = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
        	am = (ActivityManager) mSystemContext.getSystemService(Context.ACTIVITY_SERVICE);
        	
        	IBinder token = (IBinder)intent.getExtra("token");
        	int taskId = intent.getIntExtra("id", 0);
        	
        	try{
        		wm.setFocusedApp(token, false);	
        	} catch (Exception e) {	
        		Log.d("test1","CANNOT CHANGE APP FOCUS", e);
        	}
        	
        	final long origId = Binder.clearCallingIdentity();	
        	try {	
        		am.moveTaskToFront(taskId, ActivityManager.MOVE_TASK_NO_USER_ACTION);        		
        	} catch (Exception e) {	
        		Log.e("test1", "Cannot move the activity to front", e);	
        	}	
        	Binder.restoreCallingIdentity(origId);	
			//Using "messy" boradcast intent since wm and am needs system-specific permission
			
        }
    };
	
	private static void onCreateHook(){
		XposedBridge.hookAllMethods(Activity.class, "onCreate", new XC_MethodHook(){
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable { 
				activity =  (Activity)param.thisObject;
				isHoloFloat = (activity.getIntent().getFlags() 
						& Res.FLAG_FLOATING_WINDOW) == Res.FLAG_FLOATING_WINDOW;
			}
		});	
		XposedBridge.hookAllMethods(Activity.class, "onResume", new XC_MethodHook(){
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable { 
				if (!isHoloFloat) return;
				if (overlayView != null){
					FrameLayout decorView = (FrameLayout) activity.getWindow().peekDecorView().getRootView();
					decorView.bringChildToFront(overlayView);
				}
			}
		});	
		
	}
	
	
	public static void inject_DecorView_generateLayout(final LoadPackageParam lpparam) throws Throwable{
		//Class<?> hookClass = findClass("com.android.internal.policy.impl.PhoneWindow", lpparam.classLoader);
		//XposedBridge.hookAllMethods(hookClass, "generateLayout",  new XC_MethodHook() { 
		XposedBridge.hookAllMethods(Activity.class, "onStart", new XC_MethodHook() { 
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (!isHoloFloat) return;
				pref.reload();
				if (!pref.getBoolean(Res.KEY_MOVABLE_WINDOW, Res.DEFAULT_MOVABLE_WINDOW)) return;
				Activity thiss =  (Activity)param.thisObject;
				Window window = (Window) thiss.getWindow();
				//Window window = (Window) param.thisObject;
				Context context = window.getContext();
					
				FrameLayout decorView = (FrameLayout) window.peekDecorView().getRootView();
				if (decorView == null) return;
				
				XmlResourceParser parser = modRes.getLayout(R.layout.movable_window);
				overlayView = window.getLayoutInflater().inflate(parser, null);
				
				ViewGroup.LayoutParams paramz = new ViewGroup.LayoutParams
						(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
				
				decorView.addView(overlayView, -1, paramz);
										
				triangle = (ImageView)overlayView.findViewById(R.id.movable_corner);
				triangle.setBackground(modRes.getDrawable(R.drawable.movable_corner));

				Resizable resize = new Resizable(context, window);
				triangle.setOnTouchListener(resize);
				
				triangle.setOnLongClickListener(new View.OnLongClickListener(){
					@Override
					public boolean onLongClick(View v) {
						setDragActionBarVisibility(true);
						return true;
					}
				});
				
				setDragActionBarVisibility(false);
				initActionBar(activity);
			}
		});
	}
	
	//Show and hide the action bar we injected for dragging
	private static void setDragActionBarVisibility(boolean visible){
		View header = overlayView.findViewById(R.id.movable_action_bar);
		header.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
		triangle.setVisibility(visible ? View.INVISIBLE : View.VISIBLE);
	}
	
	private static void showTransparencyDialogVisibility(final Window win){
		final View bg = overlayView.findViewById(R.id.movable_bg);		
		final TextView number = (TextView)overlayView.findViewById(R.id.movable_textView8);
		final SeekBar t = (SeekBar)overlayView.findViewById(R.id.movable_seekBar1);
		
		float oldValue = win.getAttributes().alpha;
		t.setProgress((int) (oldValue*100)-10);
		t.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override public void onStopTrackingTouch(SeekBar seekBar) { }
			@Override public void onStartTrackingTouch(SeekBar seekBar) { }
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				int newProgress = (progress + 10);
				number.setText(newProgress + "%");

				WindowManager.LayoutParams params = win.getAttributes(); 
				params.alpha = newProgress * 0.01f;
				win.setAttributes(params);
			}
		});
		
		bg.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View paramView, MotionEvent paramMotionEvent) {
				bg.setVisibility(View.INVISIBLE);
				return true;
			}
		});
		
		bg.setVisibility(View.VISIBLE);
	}
	
	private static void initActionBar(final Activity a){
		View header = overlayView.findViewById(R.id.movable_action_bar);
		Movable moveable = new Movable(a.getWindow());
		header.setOnTouchListener(moveable);
		
		ImageButton done = (ImageButton)overlayView.findViewById(R.id.movable_done);
		done.setImageDrawable(modRes.getDrawable(R.drawable.movable_done));
		done.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setDragActionBarVisibility(false);
			}
		});
		
		final ImageButton overflow = (ImageButton)overlayView.findViewById(R.id.movable_overflow);
		overflow.setImageDrawable(modRes.getDrawable(R.drawable.movable_overflow));
		overflow.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final String item1 = "Transparency";
				final String item2 = "Close App";
				
			    PopupMenu popupMenu = new PopupMenu(overflow.getContext(), (View)overflow); 
			    Menu menu = popupMenu.getMenu();
			    menu.add(item1);
			    menu.add(item2);
			    
			    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						if (item.getTitle().equals(item1)){
							showTransparencyDialogVisibility(a.getWindow());
						}
						if (item.getTitle().equals(item2)){
							a.finish();
						}
						return false;
					}
				});
			    popupMenu.show();
			}
		});
	}

	
	private static void inject_dispatchTouchEvent() throws Throwable{
		XposedBridge.hookAllMethods(Activity.class, "dispatchTouchEvent", new XC_MethodHook(){
			protected void afterHookedMethod(MethodHookParam param) throws Throwable { 
				pref.reload();
				if (!pref.getBoolean(Res.KEY_MOVABLE_WINDOW, Res.DEFAULT_MOVABLE_WINDOW)) return;
				
				Activity a =  (Activity)param.thisObject;
				boolean isHoloFloat = (a.getIntent().getFlags() & Res.FLAG_FLOATING_WINDOW) == Res.FLAG_FLOATING_WINDOW;
				if (!isHoloFloat) return;
				
				MotionEvent event = (MotionEvent) param.args[0] ;
				switch (event.getAction()){
				
				case MotionEvent.ACTION_OUTSIDE:
					param.setResult(Boolean.FALSE); //False so android passes touch to behind app
					break;
					
				case MotionEvent.ACTION_DOWN:
					viewX = event.getX();
					viewY = event.getY();
					changeFocusApp(a);
					break;
				case MotionEvent.ACTION_MOVE:
					ActionBar ab = a.getActionBar();
					int height = (ab != null) ? ab.getHeight() : dp(48,a.getApplicationContext());
					
					if (viewY < height){
						screenX = event.getRawX();
						screenY = event.getRawY();
						leftFromScreen = (screenX - viewX);
						topFromScreen = (screenY - viewY);
						Window mWindow = a.getWindow();
						mWindow.setGravity(Gravity.LEFT | Gravity.TOP);
						updateView(mWindow, leftFromScreen, topFromScreen);
					}
					break;
				}
			}		
		}); 
	}	
	
	private static void changeFocusApp(Activity a) throws Throwable{
		Intent i = new Intent(CHANGE_APP_FOCUS);
		i.putExtra("token", a.getActivityToken());
		i.putExtra("id", a.getTaskId());
		a.sendBroadcast(i);
	}
	
	private static void updateView(Window mWindow, float x , float y){
		WindowManager.LayoutParams params = mWindow.getAttributes(); 
		params.x = (int)x;	
		params.y = (int)y;
		mWindow.setAttributes(params);
	}
	
	public static int dp(int dp, Context c){ //convert dp to px
		float scale = c.getResources().getDisplayMetrics().density;
		int pixel = (int) (dp*scale + 0.5f);
		return pixel;
	}
}
