package com.zst.xposed.halo.floatingwindow.hooks;

import static de.robv.android.xposed.XposedHelpers.findClass;

import com.zst.xposed.halo.floatingwindow.Common;
import com.zst.xposed.halo.floatingwindow.R;
import com.zst.xposed.halo.floatingwindow.helpers.Movable;
import com.zst.xposed.halo.floatingwindow.helpers.Resizable;

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
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.Build;
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
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class MovableWindow {
	
	static final String INTENT_APP_TOKEN = "token";
	static final String INTENT_APP_ID = "id";
	
	static XSharedPreferences mPref;
	static XModuleResources mModRes;
	/* App ActionBar Moving Values */
	private static Float screenX;
	private static Float screenY;
	private static Float viewX;
	private static Float viewY;
	private static Float leftFromScreen;
	private static Float topFromScreen;
	
	private static IWindowManager iWindowManager;
	private static ActivityManager iActivityManager;
	private static Context mSystemContext; // SystemUI Context
	private static Activity activity; // Current app activity
	static boolean isHoloFloat = false; // Current app has floating flag?
	
	static ImageView quadrant;
	static ImageView triangle;
	static View overlayView;
	
	/* Corner Button Actions Constants*/
	static final int ACTION_LONGPRESS_TRIANGLE = 0x1;
	static final int ACTION_CLICK_QUADRANT = 0x2;
	static final int ACTION_LONGPRESS_QUADRANT = 0x3;
	
	public static void handleLoadPackage(LoadPackageParam l, XSharedPreferences p) throws Throwable {
		mPref = p;
		try {
		focusChangeContextFinder(l);
		} catch (Exception e) {
			XposedBridge.log(Common.LOG_TAG + "Movable / focusChangeContextFinder");
			XposedBridge.log(e);
		}
		
		activityHook();
		inject_dispatchTouchEvent();
		
		try {
			injectTriangle(l);
		} catch (Exception e) {
			XposedBridge.log(Common.LOG_TAG + "Movable / inject_DecorView_generateLayout");
			XposedBridge.log(e);
		}
	}
	
	public static void handleInitPackageResources(XModuleResources res) throws Throwable {
		mModRes = res;
	}
	
	private static void focusChangeContextFinder(LoadPackageParam l) throws Throwable {
		if (!l.packageName.equals("com.android.systemui")) return;
		Class<?> hookClass = findClass("com.android.systemui.SystemUIService", l.classLoader);
		XposedBridge.hookAllMethods(hookClass, "onCreate", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				Service thiz = (Service) param.thisObject;
				mSystemContext = thiz.getApplicationContext();
				// Gets SystemUI Context which has
				IntentFilter filters = new IntentFilter();
				filters.addAction(Common.CHANGE_APP_FOCUS);
				mSystemContext.registerReceiver(mIntentReceiver, filters, null, null);
			}
		});
	}
	
	final static BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			IBinder token = (IBinder) intent.getExtra(INTENT_APP_TOKEN);
			int taskId = intent.getIntExtra(INTENT_APP_ID, 0);
			
			iWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
			iActivityManager = (ActivityManager) mSystemContext
					.getSystemService(Context.ACTIVITY_SERVICE);
			
			try {
				iWindowManager.setFocusedApp(token, false);
			} catch (Exception e) {
				XposedBridge.log(Common.LOG_TAG + "Cannot change App Focus");
				XposedBridge.log(e);
				Log.d("test1", "CANNOT CHANGE APP FOCUS", e);
			}
			
			final long origId = Binder.clearCallingIdentity();
			try {
				iActivityManager.moveTaskToFront(taskId, ActivityManager.MOVE_TASK_NO_USER_ACTION);
			} catch (Exception e) {
				XposedBridge.log(Common.LOG_TAG + "Cannot move task to front");
				XposedBridge.log(e);
				Log.e("test1", "Cannot move the activity to front", e);
			}
			Binder.restoreCallingIdentity(origId);
			// Using "messy" boradcast intent since wm and am needs
			// system-specific permission
		}
	};
	
	private static void activityHook(){
		XposedBridge.hookAllMethods(Activity.class, "onCreate", new XC_MethodHook() {
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				activity = (Activity) param.thisObject;
				isHoloFloat = (activity.getIntent().getFlags() & Common.FLAG_FLOATING_WINDOW)
						== Common.FLAG_FLOATING_WINDOW;
			}
		});
		
		XposedBridge.hookAllMethods(Activity.class, "onResume", new XC_MethodHook() {
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (!isHoloFloat) return;
				if (overlayView != null) {
					FrameLayout decorView = (FrameLayout) activity.getWindow()
							.peekDecorView().getRootView();
					decorView.bringChildToFront(overlayView);
				}
			}
		});
		
	}
	
	private static void injectTriangle(final LoadPackageParam lpparam)
			throws Throwable {
		XposedBridge.hookAllMethods(Activity.class, "onStart", new XC_MethodHook() {
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (!isHoloFloat) return;
				mPref.reload();
				if (!mPref.getBoolean(Common.KEY_MOVABLE_WINDOW, Common.DEFAULT_MOVABLE_WINDOW)) return;
				activity = (Activity) param.thisObject;
				Window window = (Window) activity.getWindow();
				// Window window = (Window) param.thisObject;
				Context context = window.getContext();
				
				FrameLayout decorView = (FrameLayout) window.peekDecorView().getRootView();
				if (decorView == null) return;
				
				XmlResourceParser parser = mModRes.getLayout(R.layout.movable_window);
				overlayView = window.getLayoutInflater().inflate(parser, null);
				
				ViewGroup.LayoutParams paramz = new ViewGroup.LayoutParams(
						ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
				
				decorView.addView(overlayView, -1, paramz);
				
				String color_str = mPref.getString(Common.KEY_WINDOW_TRIANGLE_COLOR, Common.DEFAULT_WINDOW_TRIANGLE_COLOR);
				Drawable triangle_background = mModRes.getDrawable(R.drawable.movable_corner);
				if (!color_str.equals(Common.DEFAULT_WINDOW_TRIANGLE_COLOR)) { //If not white, apply
					triangle_background.setColorFilter(Color.parseColor("#" + color_str), Mode.MULTIPLY);
				}
				Drawable quadrant_background = mModRes.getDrawable(R.drawable.movable_quadrant);
				String color_quadrant = mPref.getString(Common.KEY_WINDOW_QUADRANT_COLOR, Common.DEFAULT_WINDOW_QUADRANT_COLOR);
				if (!color_quadrant.equals(Common.DEFAULT_WINDOW_QUADRANT_COLOR)) { //If not white, apply
					quadrant_background.setColorFilter(Color.parseColor("#" + color_quadrant), Mode.MULTIPLY);
				}
				float triangle_alpha = mPref.getFloat(Common.KEY_WINDOW_TRIANGLE_ALPHA, Common.DEFAULT_WINDOW_TRIANGLE_ALPHA);
				triangle_background.setAlpha((int)(triangle_alpha * 255));
				
				float quadrant_alpha = mPref.getFloat(Common.KEY_WINDOW_QUADRANT_ALPHA, Common.DEFAULT_WINDOW_QUADRANT_ALPHA);
				quadrant_background.setAlpha((int)(quadrant_alpha * 255));
				
				triangle = (ImageView) overlayView.findViewById(R.id.movable_corner);
				quadrant = (ImageView) overlayView.findViewById(R.id.movable_quadrant);
				
				if (Build.VERSION.SDK_INT >= 16) {
					triangle.setBackground(triangle_background);
					quadrant.setBackground(quadrant_background);
				} else {
					triangle.setBackgroundDrawable(triangle_background);
					quadrant.setBackgroundDrawable(quadrant_background);
				}
								
				int triangle_size = mPref.getInt(Common.KEY_WINDOW_TRIANGLE_SIZE, Common.DEFAULT_WINDOW_TRIANGLE_SIZE);
				triangle.getLayoutParams().width = triangle_size;
				triangle.getLayoutParams().height = triangle_size;
				
				int quadrant_size = mPref.getInt(Common.KEY_WINDOW_QUADRANT_SIZE, Common.DEFAULT_WINDOW_QUADRANT_SIZE);
				quadrant.getLayoutParams().width = quadrant_size;
				quadrant.getLayoutParams().height = quadrant_size;
				
				Resizable resize = new Resizable(context, window);
				triangle.setOnTouchListener(resize);
				triangle.setOnLongClickListener(new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						cornerButtonClickAction(ACTION_LONGPRESS_TRIANGLE);
						return true;
					}
				});
				
				boolean quadrant_enabled = mPref.getBoolean(Common.KEY_WINDOW_QUADRANT_ENABLE,
						Common.DEFAULT_WINDOW_QUADRANT_ENABLE);
				if (quadrant_enabled) {
					quadrant.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							cornerButtonClickAction(ACTION_CLICK_QUADRANT);
						}
					});
					
					quadrant.setOnLongClickListener(new View.OnLongClickListener() {
						@Override
						public boolean onLongClick(View v) {
							cornerButtonClickAction(ACTION_LONGPRESS_QUADRANT);
							return true;
						}
					});
				} else {
					quadrant.getLayoutParams().width = 0;
					quadrant.getLayoutParams().height = 0;
				}
				
				setDragActionBarVisibility(false);
				initActionBar(activity);
			}
		});
	}
	
	private static void cornerButtonClickAction(int type_of_action) {
		String index = "0";
		switch (type_of_action) {
		case ACTION_LONGPRESS_TRIANGLE:
			index = mPref.getString(Common.KEY_WINDOW_TRIANGLE_LONGPRESS_ACTION,
					Common.DEFAULT_WINDOW_TRIANGLE_LONGPRESS_ACTION);
			break;
		case ACTION_CLICK_QUADRANT:
			index = mPref.getString(Common.KEY_WINDOW_QUADRANT_CLICK_ACTION,
					Common.DEFAULT_WINDOW_QUADRANT_CLICK_ACTION);
			break;
		case ACTION_LONGPRESS_QUADRANT:
			index = mPref.getString(Common.KEY_WINDOW_QUADRANT_LONGPRESS_ACTION,
					Common.DEFAULT_WINDOW_QUADRANT_LONGPRESS_ACTION);
			break;
		}
		switch (Integer.parseInt(index)) {
		case 0: // Do Nothing
			break;
		case 1: // Drag & Move Bar
			setDragActionBarVisibility(true);
			break;
		case 2:
			activity.finishAffinity();
			break;
		}
	}

	// Show and hide the action bar we injected for dragging
	private static void setDragActionBarVisibility(boolean visible) {
		View header = overlayView.findViewById(R.id.movable_action_bar);
		header.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
		triangle.setVisibility(visible ? View.INVISIBLE : View.VISIBLE);
		quadrant.setVisibility(visible ? View.INVISIBLE : View.VISIBLE);
	}
	
	private static void showTransparencyDialogVisibility(final Window win) {
		final View bg = overlayView.findViewById(R.id.movable_bg);
		final TextView number = (TextView) overlayView.findViewById(R.id.movable_textView8);
		final SeekBar t = (SeekBar) overlayView.findViewById(R.id.movable_seekBar1);
		
		float oldValue = win.getAttributes().alpha;
		t.setProgress((int) (oldValue * 100) - 10);
		t.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}
			
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
	
	private static void initActionBar(final Activity a) {
		View header = overlayView.findViewById(R.id.movable_action_bar);
		Movable moveable = new Movable(a.getWindow());
		header.setOnTouchListener(moveable);
		
		ImageButton done = (ImageButton) overlayView.findViewById(R.id.movable_done);
		done.setImageDrawable(mModRes.getDrawable(R.drawable.movable_done));
		done.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setDragActionBarVisibility(false);
			}
		});
		
		final ImageButton overflow = (ImageButton) overlayView.findViewById(R.id.movable_overflow);
		overflow.setImageDrawable(mModRes.getDrawable(R.drawable.movable_overflow));
		overflow.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final String item1 = "Transparency";
				final String item2 = "Close App";
				//TODO strings.xml
				PopupMenu popupMenu = new PopupMenu(overflow.getContext(), (View) overflow);
				Menu menu = popupMenu.getMenu();
				menu.add(item1);
				menu.add(item2);
				
				popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						if (item.getTitle().equals(item1)) {
							showTransparencyDialogVisibility(a.getWindow());
						}
						if (item.getTitle().equals(item2)) {
							a.finish();
						}
						return false;
					}
				});
				popupMenu.show();
			}
		});
	}
	
	private static void inject_dispatchTouchEvent() throws Throwable {
		XposedBridge.hookAllMethods(Activity.class, "dispatchTouchEvent", new XC_MethodHook() {
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				mPref.reload();
				if (!mPref.getBoolean(Common.KEY_MOVABLE_WINDOW, Common.DEFAULT_MOVABLE_WINDOW)) return;
				
				Activity a = (Activity) param.thisObject;
				boolean isHoloFloat = (a.getIntent().getFlags() & Common.FLAG_FLOATING_WINDOW) == Common.FLAG_FLOATING_WINDOW;
				if (!isHoloFloat) return;
				
				MotionEvent event = (MotionEvent) param.args[0];
				switch (event.getAction()) {
				
				case MotionEvent.ACTION_OUTSIDE:
					param.setResult(Boolean.FALSE); 
					// False so android passes touch to behind app
					break;
				
				case MotionEvent.ACTION_DOWN:
					viewX = event.getX();
					viewY = event.getY();
					changeFocusApp(a);
					break;
				case MotionEvent.ACTION_MOVE:
					ActionBar ab = a.getActionBar();
					int height = (ab != null) ? ab.getHeight() : dp(48, a.getApplicationContext());
					
					if (viewY < height) {
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
	
	private static void changeFocusApp(Activity a) throws Throwable {
		Intent i = new Intent(Common.CHANGE_APP_FOCUS);
		i.putExtra(INTENT_APP_TOKEN, a.getActivityToken());
		i.putExtra(INTENT_APP_ID, a.getTaskId());
		a.sendBroadcast(i);
	}
	
	private static void updateView(Window mWindow, float x, float y) {
		WindowManager.LayoutParams params = mWindow.getAttributes();
		params.x = (int) x;
		params.y = (int) y;
		mWindow.setAttributes(params);
	}
	
	public static int dp(int dp, Context c) { // convert dp to px
		float scale = c.getResources().getDisplayMetrics().density;
		int pixel = (int) (dp * scale + 0.5f);
		return pixel;
	}
}
