package com.zst.xposed.halo.floatingwindow.hooks;

import com.zst.xposed.halo.floatingwindow.Common;
import com.zst.xposed.halo.floatingwindow.R;
import com.zst.xposed.halo.floatingwindow.helpers.Movable;
import com.zst.xposed.halo.floatingwindow.helpers.Resizable;
import com.zst.xposed.halo.floatingwindow.helpers.RightResizable;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.XModuleResources;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Build;
import android.view.Gravity;
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
	
	static final String INTENT_APP_PKG = "pkg";
	
	static XSharedPreferences mPref;
	static XModuleResources mModRes;
	/* App ActionBar Moving Values */
	private static Float screenX;
	private static Float screenY;
	private static Float viewX;
	private static Float viewY;
	private static Float leftFromScreen;
	private static Float topFromScreen;
	
	static Activity activity; // Current app activity
	static boolean isHoloFloat = false; // Current app has floating flag?
	static boolean mMovableWindow;
	
	static ImageView quadrant;
	static ImageView triangle;
	static View overlayView;
	
	/* Corner Button Actions Constants*/
	static final int ACTION_CLICK_TRIANGLE = 0x0;
	static final int ACTION_LONGPRESS_TRIANGLE = 0x1;
	static final int ACTION_CLICK_QUADRANT = 0x2;
	static final int ACTION_LONGPRESS_QUADRANT = 0x3;
	
	public static void handleLoadPackage(LoadPackageParam l, XSharedPreferences p, XModuleResources res) throws Throwable {
		mModRes = res;
		mPref = p;
		
		activityHook();
		inject_dispatchTouchEvent();
		
		try {
			injectTriangle(l);
		} catch (Exception e) {
			XposedBridge.log(Common.LOG_TAG + "Movable / inject_DecorView_generateLayout");
			XposedBridge.log(e);
		}
	}
	
	private static void activityHook(){
		XposedBridge.hookAllMethods(Activity.class, "onCreate", new XC_MethodHook() {
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				activity = (Activity) param.thisObject;
				isHoloFloat = (activity.getIntent().getFlags() & Common.FLAG_FLOATING_WINDOW)
						== Common.FLAG_FLOATING_WINDOW;
				mMovableWindow = mPref.getBoolean(Common.KEY_MOVABLE_WINDOW,
						Common.DEFAULT_MOVABLE_WINDOW);
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
		
		XposedBridge.hookAllMethods(Activity.class, "onDestroy", new XC_MethodHook() {
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				unregisterLayoutBroadcastReceiver(((Activity) param.thisObject).getWindow());
			}
		});
	}
	
	private static void injectTriangle(final LoadPackageParam lpparam)
			throws Throwable {
		XposedBridge.hookAllMethods(Activity.class, "onStart", new XC_MethodHook() {
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (!isHoloFloat) return;
				mPref.reload();
				if (!mMovableWindow) return;
				activity = (Activity) param.thisObject;
				Window window = (Window) activity.getWindow();
				
				registerLayoutBroadcastReceiver(window);
				setLayoutPositioning(window);
				
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
				
				final Activity current_activity = activity;
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
				
				boolean triangle_enabled = mPref.getBoolean(Common.KEY_WINDOW_TRIANGLE_ENABLE,
						Common.DEFAULT_WINDOW_TRIANGLE_ENABLE);
				if (triangle_enabled) {
					if (mPref.getBoolean(Common.KEY_WINDOW_TRIANGLE_RESIZE_ENABLED,
							Common.DEFAULT_WINDOW_TRIANGLE_RESIZE_ENABLED)) {
						Resizable resize = new Resizable(context, window);
						triangle.setOnTouchListener(resize);
					}
					triangle.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							cornerButtonClickAction(ACTION_CLICK_TRIANGLE, current_activity);
						}
					});
					
					triangle.setOnLongClickListener(new View.OnLongClickListener() {
						@Override
						public boolean onLongClick(View v) {
							cornerButtonClickAction(ACTION_LONGPRESS_TRIANGLE, current_activity);
							return true;
						}
					});
				} else {
					triangle.getLayoutParams().width = 0;
					triangle.getLayoutParams().height = 0;
				}
				
				boolean quadrant_enabled = mPref.getBoolean(Common.KEY_WINDOW_QUADRANT_ENABLE,
						Common.DEFAULT_WINDOW_QUADRANT_ENABLE);
				if (quadrant_enabled) {
					if (mPref.getBoolean(Common.KEY_WINDOW_QUADRANT_RESIZE_ENABLED,
							Common.DEFAULT_WINDOW_QUADRANT_RESIZE_ENABLED)) {
						RightResizable right_resize = new RightResizable(window);
						quadrant.setOnTouchListener(right_resize);
					}
					
					quadrant.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							cornerButtonClickAction(ACTION_CLICK_QUADRANT, current_activity);
						}
					});
					
					quadrant.setOnLongClickListener(new View.OnLongClickListener() {
						@Override
						public boolean onLongClick(View v) {
							cornerButtonClickAction(ACTION_LONGPRESS_QUADRANT, current_activity);
							return true;
						}
					});
				} else {
					quadrant.getLayoutParams().width = 0;
					quadrant.getLayoutParams().height = 0;
				}
				
				setDragActionBarVisibility(false);
				initActionBar(activity);
				
				if (mPref.getBoolean(Common.KEY_WINDOW_BORDER_ENABLED,
						Common.DEFAULT_WINDOW_BORDER_ENABLED)) {
					final int color = Color.parseColor("#" + mPref.getString(
							Common.KEY_WINDOW_BORDER_COLOR, Common.DEFAULT_WINDOW_BORDER_COLOR));
					final int thickness = mPref.getInt(Common.KEY_WINDOW_BORDER_THICKNESS,
							Common.DEFAULT_WINDOW_BORDER_THICKNESS);
					setWindowBorder(color, thickness);
				}
			}
		});
	}
	
	private static void setWindowBorder(int color, int thickness) {
		if (thickness == 0) {
			overlayView.setBackgroundResource(0);
		} else {
			// make the shape a drawable
			ShapeDrawable rectShapeDrawable = new ShapeDrawable(new RectShape());
			Paint paint = rectShapeDrawable.getPaint();
			paint.setColor(color);
			paint.setStyle(Paint.Style.STROKE);
			paint.setStrokeWidth(thickness);
			overlayView.setBackgroundDrawable(rectShapeDrawable);
		}
	}
	
	private static void cornerButtonClickAction(int type_of_action, Activity activity) {
		String index = "0";
		switch (type_of_action) {
		case ACTION_CLICK_TRIANGLE:
			index = mPref.getString(Common.KEY_WINDOW_TRIANGLE_CLICK_ACTION,
					Common.DEFAULT_WINDOW_TRIANGLE_CLICK_ACTION);
			break;
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
			if (Build.VERSION.SDK_INT >= 16) {
				activity.finishAffinity();
			} else {
				activity.finish();
			}
			break;
		case 3: //Transparency Dialog
			showTransparencyDialogVisibility(activity.getWindow());
			break;
		case 4: // Hide Entire App
			activity.moveTaskToBack(true);
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
		
		final TextView dtm_title = (TextView) overlayView.findViewById(R.id.textView1);
		dtm_title.setText(mModRes.getString(R.string.dnm_title));
		final TextView trans_title = (TextView) overlayView.findViewById(R.id.movable_textView2);
		trans_title.setText(mModRes.getString(R.string.dnm_transparency));
		
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
				final String item1 = mModRes.getString(R.string.dnm_transparency);
				final String item2 = mModRes.getString(R.string.dnm_close_app);
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
				if (!mMovableWindow) return;
				
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
						initAndRefreshLayoutParams(a.getWindow(), a.getBaseContext(), activity.getPackageName());
					}
					break;
				}
			}
		});
	}
	
	/* (Start) Layout Position Method Helpers */
	static boolean layout_moved;
	static int layout_x;
	static int layout_y;
	static int layout_width;
	static int layout_height;
	static float layout_alpha;

	private static boolean initLayoutPositioning(Window window) {
		if (!mPref.getBoolean(Common.KEY_WINDOW_MOVING_RETAIN_START_POSITION,
				Common.DEFAULT_WINDOW_MOVING_RETAIN_START_POSITION)) 
			return false;

		final WindowManager.LayoutParams params = window.getAttributes();
		layout_moved = true;
		layout_x = params.x;
		layout_y = params.y;
		layout_width = params.width;
		layout_height = params.height;
		layout_alpha = params.alpha;
		return true;
	}
	
	private static void setLayoutPositioning(Window window) {
		if (!layout_moved) return;
		
		if (!mPref.getBoolean(Common.KEY_WINDOW_MOVING_RETAIN_START_POSITION,
				Common.DEFAULT_WINDOW_MOVING_RETAIN_START_POSITION)) 
			return;
		
		WindowManager.LayoutParams params = window.getAttributes();
		params.x = layout_x;
		params.y = layout_y;
		params.width = layout_width;
		params.height = layout_height;
		params.alpha = layout_alpha;
		params.gravity = Gravity.LEFT | Gravity.TOP;
		window.setAttributes(params);
	}
	
	private static void registerLayoutBroadcastReceiver(final Window window) {
		if (!(mPref.getBoolean(Common.KEY_WINDOW_MOVING_RETAIN_START_POSITION,
				Common.DEFAULT_WINDOW_MOVING_RETAIN_START_POSITION) ||
			mPref.getBoolean(Common.KEY_WINDOW_MOVING_CONSTANT_POSITION,
				Common.DEFAULT_WINDOW_MOVING_CONSTANT_POSITION)))
			return;
		
		BroadcastReceiver br = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (intent.getStringExtra(INTENT_APP_PKG).equals(
						window.getContext().getApplicationInfo().packageName)) {
					setLayoutPositioning(window);
				}
			}
		};
		IntentFilter filters = new IntentFilter();
		filters.addAction(Common.REFRESH_APP_LAYOUT);
		window.getContext().registerReceiver(br, filters);
		window.getDecorView().setTagInternal(Common.LAYOUT_RECEIVER_TAG, br);
	}
	
	private static void unregisterLayoutBroadcastReceiver(Window window) {
		if (!(mPref.getBoolean(Common.KEY_WINDOW_MOVING_RETAIN_START_POSITION,
				Common.DEFAULT_WINDOW_MOVING_RETAIN_START_POSITION) ||
			mPref.getBoolean(Common.KEY_WINDOW_MOVING_CONSTANT_POSITION,
				Common.DEFAULT_WINDOW_MOVING_CONSTANT_POSITION)))
			return;
		
		try {
			BroadcastReceiver br = (BroadcastReceiver) window.getDecorView().getTag(
					Common.LAYOUT_RECEIVER_TAG);
			window.getContext().unregisterReceiver(br);
		} catch (Exception e) {
		}
	}
	
	public static void initAndRefreshLayoutParams(Window w, Context ctx, String pkg) {
		if (initLayoutPositioning(w)) {
			refreshLayoutParams(ctx, pkg);
		}
	}
	private static void refreshLayoutParams(Context ctx, String pkg) {
		Intent intent = new Intent(Common.REFRESH_APP_LAYOUT); 
		intent.putExtra(INTENT_APP_PKG, pkg);
		ctx.sendBroadcast(intent);
	}
	/* (End) Layout Position Method Helpers */
	
	private static void changeFocusApp(Activity a) throws Throwable {
		Intent i = new Intent(Common.CHANGE_APP_FOCUS);
		i.putExtra(Common.INTENT_APP_TOKEN, a.getActivityToken());
		i.putExtra(Common.INTENT_APP_ID, a.getTaskId());
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
