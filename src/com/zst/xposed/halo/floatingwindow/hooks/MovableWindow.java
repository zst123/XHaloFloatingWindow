package com.zst.xposed.halo.floatingwindow.hooks;

import com.zst.xposed.halo.floatingwindow.Common;
import com.zst.xposed.halo.floatingwindow.R;
import com.zst.xposed.halo.floatingwindow.helpers.AeroSnap;
import com.zst.xposed.halo.floatingwindow.helpers.Movable;
import com.zst.xposed.halo.floatingwindow.helpers.OutlineLeftResizable;
import com.zst.xposed.halo.floatingwindow.helpers.OutlineRightResizable;
import com.zst.xposed.halo.floatingwindow.helpers.Resizable;
import com.zst.xposed.halo.floatingwindow.helpers.RightResizable;
import com.zst.xposed.halo.floatingwindow.helpers.Util;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.content.res.XModuleResources;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
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
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.SeekBar;
import android.widget.TextView;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
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
	static boolean mActionBarDraggable;
	static boolean mLiveResizing;
	static int mPreviousOrientation;
	
	/* AeroSnap*/
	static AeroSnap mAeroSnap;
	static boolean mAeroSnapEnabled;
	static int mAeroSnapDelay;
	
	/* Title Bar */
	static int mTitleBarHeight = Common.DEFAULT_WINDOW_TITLEBAR_SIZE;
	static int mTitleBarDivider = 2;
	
	static ImageView quadrant;
	static ImageView triangle;
	static View overlayView;
	
	static final int ID_OVERLAY_VIEW = 1000000;
	static final int ID_NOTIFICATION_RESTORE = 22222222;
	
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
		/* Initialize all the preference variables here.
		 * TODO: Place some of the variables used below in this hook to reduce
		 * preference.reload() calls */
		XposedBridge.hookAllMethods(Activity.class, "onCreate", new XC_MethodHook() {
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				activity = (Activity) param.thisObject;
				mPref.reload();
				isHoloFloat = (activity.getIntent().getFlags() & Common.FLAG_FLOATING_WINDOW)
						== Common.FLAG_FLOATING_WINDOW;
				mMovableWindow = mPref.getBoolean(Common.KEY_MOVABLE_WINDOW,
						Common.DEFAULT_MOVABLE_WINDOW);
				mActionBarDraggable = mPref.getBoolean(Common.KEY_WINDOW_ACTIONBAR_DRAGGING_ENABLED,
						Common.DEFAULT_WINDOW_ACTIONBAR_DRAGGING_ENABLED);
				
				boolean titlebar_enabled = mPref.getBoolean(Common.KEY_WINDOW_TITLEBAR_ENABLED,
						Common.DEFAULT_WINDOW_TITLEBAR_ENABLED);
				int titlebar_size = mPref.getInt(Common.KEY_WINDOW_TITLEBAR_SIZE,
						Common.DEFAULT_WINDOW_TITLEBAR_SIZE);
				mTitleBarHeight = titlebar_enabled ? Util.realDp(titlebar_size, activity) : 0;
				mTitleBarDivider = Util.realDp(2, activity);
				mPreviousOrientation = activity.getResources().getConfiguration().orientation;
				mLiveResizing = mPref.getBoolean(Common.KEY_WINDOW_RESIZING_LIVE_UPDATE,
						Common.DEFAULT_WINDOW_RESIZING_LIVE_UPDATE);
				
				mAeroSnapEnabled = mPref.getBoolean(Common.KEY_WINDOW_RESIZING_AERO_SNAP_ENABLED,
						Common.DEFAULT_WINDOW_RESIZING_AERO_SNAP_ENABLED);
				mAeroSnapDelay = mPref.getInt(Common.KEY_WINDOW_RESIZING_AERO_SNAP_DELAY,
						Common.DEFAULT_WINDOW_RESIZING_AERO_SNAP_DELAY);
				mAeroSnap = mAeroSnapEnabled ? new AeroSnap(activity.getWindow(), mAeroSnapDelay) : null;
				
			}
		});
		
		// re-initialize the variables when resuming as they may get replaced by another activity.
		XposedBridge.hookAllMethods(Activity.class, "onResume", new XC_MethodHook() {
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				activity = (Activity) param.thisObject;
				if (!isHoloFloat) return;
				if (overlayView != null) {
					FrameLayout decorView = (FrameLayout) activity.getWindow()
							.peekDecorView().getRootView();
					decorView.bringChildToFront(overlayView);
				}
				if (mMovableWindow) {
					overlayView = activity.findViewById(ID_OVERLAY_VIEW);
					triangle = (ImageView) overlayView.findViewById(R.id.movable_corner);
					quadrant = (ImageView) overlayView.findViewById(R.id.movable_quadrant);
				}
			}
		});
		
		// unregister the receiver for syncing window position
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
				
				// register the receiver for syncing window position
				registerLayoutBroadcastReceiver(window);
				// set layout position from previous activity if available
				setLayoutPositioning(window);
				
				Context context = window.getContext();
				
				FrameLayout decorView = (FrameLayout) window.peekDecorView().getRootView();
				if (decorView == null) return;
				// make sure the titlebar/drag-to-move-bar is not behind the statusbar
				decorView.setFitsSystemWindows(true);
				try {
					// disable resizing animation to speed up scaling (doesn't work on all roms)
					XposedHelpers.callMethod(decorView, "hackTurnOffWindowResizeAnim", true);
				} catch (Throwable e) {
				}
				
				XmlResourceParser parser = mModRes.getLayout(R.layout.movable_window);
				/* get the layout from our module. we cannot just use the R reference
				 * since the layout is from the module, not the current app we are modifying */
				overlayView = window.getLayoutInflater().inflate(parser, null);
				
				// set the id of our layout so we can find it again
				overlayView.setId(ID_OVERLAY_VIEW);
				
				RelativeLayout.LayoutParams paramz = new RelativeLayout.LayoutParams(
						ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
				paramz.setMargins(0, 0, 0, 0);
				
				decorView.addView(overlayView, -1, paramz);
				
				// Create the drawables, alpha, size for the triangle and quadrant.
				/* We do this programatically since when inflating, the system will
				 * find the drawables in the CURRENT app, which will FAIL since the
				 * drawables are in the MODULE */
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
						if (mLiveResizing) {
							triangle.setOnTouchListener(new Resizable(context, window));
						} else {
							triangle.setOnTouchListener(new OutlineLeftResizable(context, window));
						}
					}
					
					if (mPref.getBoolean(Common.KEY_WINDOW_TRIANGLE_DRAGGING_ENABLED,
							Common.DEFAULT_WINDOW_TRIANGLE_DRAGGING_ENABLED)) {
						triangle.setOnTouchListener(new Movable(current_activity.getWindow(), triangle,
								mAeroSnapEnabled, mAeroSnapDelay));
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
						if (mLiveResizing) {
							quadrant.setOnTouchListener(new RightResizable(window));
						} else {
							quadrant.setOnTouchListener(new OutlineRightResizable(window));
						}
					}
					
					if (mPref.getBoolean(Common.KEY_WINDOW_QUADRANT_DRAGGING_ENABLED,
							Common.DEFAULT_WINDOW_QUADRANT_DRAGGING_ENABLED)) {
						quadrant.setOnTouchListener(new Movable(current_activity.getWindow(), quadrant,
								mAeroSnapEnabled, mAeroSnapDelay));
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
				
				setDragActionBarVisibility(false, true);
				initActionBar(activity);
				
				if (mPref.getBoolean(Common.KEY_WINDOW_BORDER_ENABLED,
						Common.DEFAULT_WINDOW_BORDER_ENABLED)) {
					final int color = Color.parseColor("#" + mPref.getString(
							Common.KEY_WINDOW_BORDER_COLOR, Common.DEFAULT_WINDOW_BORDER_COLOR));
					final int thickness = mPref.getInt(Common.KEY_WINDOW_BORDER_THICKNESS,
							Common.DEFAULT_WINDOW_BORDER_THICKNESS);
					setWindowBorder(color, thickness);
				}

				initTitleBar(activity, decorView);
			}
		});
	}
	
	private static void setWindowBorder(int color, int thickness) {
		if (thickness == 0) {
			overlayView.setBackgroundResource(0);
		} else {
			overlayView.setBackgroundDrawable(Util.makeOutline(color, thickness));
		}
	}
	
	// Corner Buttons (Triangle, Quadrant) Actions.
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
			setDragActionBarVisibility(true, true);
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
		case 5: // Drag & Move Bar w/o hiding corner
			setDragActionBarVisibility(true, false);
			break;
		case 6: // Maximize App
			maximizeApp(activity);
			break;
		}
	}

	// maximize and restore the window.
	private static void maximizeApp(Activity activity) {
		if ((activity.getWindow().getAttributes().width  == ViewGroup.LayoutParams.MATCH_PARENT) ||
			(activity.getWindow().getAttributes().height == ViewGroup.LayoutParams.MATCH_PARENT)) {
			restoreNonMaximizedLayout(activity.getWindow());
		} else {
			saveNonMaximizedLayout(activity.getWindow());
			activity.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.MATCH_PARENT);
		}
		// after that, send a broadcast to sync the position of the window
		initAndRefreshLayoutParams(activity.getWindow(), activity, activity.getPackageName());
	}
	// Show and hide the action bar we injected for dragging
	private static void setDragActionBarVisibility(boolean visible, boolean with_corner) {
		View header = overlayView.findViewById(R.id.movable_action_bar);
		header.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
		if (!with_corner) return;
		triangle.setVisibility(visible ? View.INVISIBLE : View.VISIBLE);
		quadrant.setVisibility(visible ? View.INVISIBLE : View.VISIBLE);
	}
	
	private static void showTransparencyDialogVisibility(final Window win) {
		final View bg = overlayView.findViewById(R.id.movable_bg);
		final TextView number = (TextView) overlayView.findViewById(R.id.movable_textView8);
		final SeekBar t = (SeekBar) overlayView.findViewById(R.id.movable_seekBar1);
		
		float oldValue = win.getAttributes().alpha;
		number.setText((int) (oldValue * 100) + "%");
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
	
	// Create the Titlebar
	/* We do this programatically since when inflating, the system will
	 * find the drawables in the CURRENT app, which will FAIL since the
	 * drawables are in the MODULE */
	private static void initTitleBar(final Activity a, final FrameLayout decorView) {
		if (mTitleBarHeight == 0) 
			return;
		
		View child = decorView.getChildAt(0);
		FrameLayout.LayoutParams parammm = (FrameLayout.LayoutParams) child.getLayoutParams();
		parammm.setMargins(0, mTitleBarHeight, 0, 0);
		child.setLayoutParams(parammm);
		
		final RelativeLayout header = (RelativeLayout) overlayView.findViewById(R.id.movable_titlebar);
		final View divider = overlayView.findViewById(R.id.movable_titlebar_line);
		final TextView app_title = (TextView) overlayView.findViewById(R.id.movable_titlebar_appname);
		final ImageButton close_button = (ImageButton) overlayView.findViewById(R.id.movable_titlebar_close);
		final ImageButton max_button = (ImageButton) overlayView.findViewById(R.id.movable_titlebar_max);
		final ImageButton min_button = (ImageButton) overlayView.findViewById(R.id.movable_titlebar_min);
		final ImageButton more_button = (ImageButton) overlayView.findViewById(R.id.movable_titlebar_more);
		
		app_title.setText(a.getApplicationInfo().loadLabel(activity.getPackageManager()));
		close_button.setImageDrawable(mModRes.getDrawable(R.drawable.movable_title_close));
		max_button.setImageDrawable(mModRes.getDrawable(R.drawable.movable_title_max));
		min_button.setImageDrawable(mModRes.getDrawable(R.drawable.movable_title_min));
		more_button.setImageDrawable(mModRes.getDrawable(R.drawable.movable_title_more));
		
		RelativeLayout.LayoutParams header_param = (LayoutParams) header.getLayoutParams();
		header_param.height = mTitleBarHeight;
		header.setLayoutParams(header_param);
		
		ViewGroup.LayoutParams divider_param = divider.getLayoutParams();
		divider_param.height = mTitleBarDivider;
		divider.setLayoutParams(divider_param);
		
		final String item1 = mModRes.getString(R.string.dnm_transparency);
		final PopupMenu popupMenu = new PopupMenu(a, more_button);
		final Menu menu = popupMenu.getMenu();
		menu.add(item1);
		popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				if (item.getTitle().equals(item1)) {
					showTransparencyDialogVisibility(a.getWindow());
				} 
				return false;
			}
		});
		
		final View.OnClickListener click = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				switch (v.getId()) {
				case R.id.movable_titlebar_close:
					if (Build.VERSION.SDK_INT >= 16) {
						activity.finishAffinity();
					} else {
						activity.finish();
					}
					break;
				case R.id.movable_titlebar_max:
					maximizeApp(a);
					break;
				case R.id.movable_titlebar_min:
					minimizeAndShowNotification(a);
					break;
				case R.id.movable_titlebar_more:
					popupMenu.show();
					break;
				}
			}
		};
		close_button.setOnClickListener(click);
		max_button.setOnClickListener(click);
		min_button.setOnClickListener(click);
		more_button.setOnClickListener(click);	
		header.setOnTouchListener(new Movable(a.getWindow(), mAeroSnapEnabled, mAeroSnapDelay));
	}
	
	// Create the drag-to-move bar
	private static void initActionBar(final Activity a) {
		View header = overlayView.findViewById(R.id.movable_action_bar);
		Movable moveable = new Movable(a.getWindow(), mAeroSnapEnabled, mAeroSnapDelay);
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
				setDragActionBarVisibility(false, true);
			}
		});
		
		final ImageButton overflow = (ImageButton) overlayView.findViewById(R.id.movable_overflow);
		overflow.setImageDrawable(mModRes.getDrawable(R.drawable.movable_overflow));
		overflow.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final String item1 = mModRes.getString(R.string.dnm_transparency);
				final String item3 = mModRes.getString(R.string.dnm_minimize);
				final String item2 = mModRes.getString(R.string.dnm_close_app);
				PopupMenu popupMenu = new PopupMenu(overflow.getContext(), (View) overflow);
				Menu menu = popupMenu.getMenu();
				menu.add(item1);
				menu.add(item3);
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
						if (item.getTitle().equals(item3)) {
							minimizeAndShowNotification(a);
						}
						return false;
					}
				});
				popupMenu.show();
			}
		});
	}
	
	// Send the app to the back, and show a notification to restore
	private static void minimizeAndShowNotification(final Activity ac) {
		Intent i = new Intent(Common.REMOVE_NOTIFICATION_RESTORE + ac.getPackageName());
		ApplicationInfo app_info = ac.getApplication().getApplicationInfo();
		PendingIntent intent = PendingIntent.getBroadcast(ac, 0, i, 
				PendingIntent.FLAG_UPDATE_CURRENT);
		String title = String.format(mModRes.getString(R.string.dnm_minimize_notif_title),
        		app_info.loadLabel(ac.getPackageManager()));
		
		Notification n  = new Notification.Builder(ac)
		        .setContentTitle(title)
		        .setContentText(mModRes.getString(R.string.dnm_minimize_notif_summary))
		        .setSmallIcon(app_info.icon)
		        .setAutoCancel(true)
		        .setContentIntent(intent)
		        .setOngoing(true)
		        .getNotification();
				
		final NotificationManager notificationManager = 
		  (NotificationManager) ac.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(ID_NOTIFICATION_RESTORE, n); 
		
		ac.moveTaskToBack(true);
		
		ac.registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				notificationManager.cancel(ID_NOTIFICATION_RESTORE);
				Intent broadcast = new Intent(Common.CHANGE_APP_FOCUS);
				broadcast.putExtra(Common.INTENT_APP_TOKEN, ac.getActivityToken());
				broadcast.putExtra(Common.INTENT_APP_ID, ac.getTaskId());
				context.sendBroadcast(broadcast);
				context.unregisterReceiver(this);
			}
		}, new IntentFilter(Common.REMOVE_NOTIFICATION_RESTORE + ac.getPackageName()));
	}
	
	// hook the touch events to move the window and have aero snap.
	private static void inject_dispatchTouchEvent() throws Throwable {
		XposedBridge.hookAllMethods(Activity.class, "dispatchTouchEvent", new XC_MethodHook() {
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if (!mMovableWindow) return;
				
				Activity a = (Activity) param.thisObject;
				boolean isHoloFloat = (a.getIntent().getFlags() & Common.FLAG_FLOATING_WINDOW) == Common.FLAG_FLOATING_WINDOW;
				if (!isHoloFloat) return;
				
				MotionEvent event = (MotionEvent) param.args[0];
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					viewX = event.getX();
					viewY = event.getY();
					changeFocusApp(a);
					if (a.getWindow().getAttributes().gravity != (Gravity.LEFT | Gravity.TOP)) {
						// Fix First Resize moving into corner
						screenX = event.getRawX();
						screenY = event.getRawY();
						leftFromScreen = (screenX - viewX);
						topFromScreen = (screenY - viewY);
						a.getWindow().setGravity(Gravity.LEFT | Gravity.TOP);
						updateView(a.getWindow(), leftFromScreen, topFromScreen);
					}
					break;
				case MotionEvent.ACTION_MOVE:
					if (mActionBarDraggable) {					
					ActionBar ab = a.getActionBar();
					int height = (ab != null) ? ab.getHeight() : Util.dp(48, a.getApplicationContext());
					
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
					}
					break;
				}
				ActionBar ab = a.getActionBar();
				int height = (ab != null) ? ab.getHeight() : Util.dp(48, a.getApplicationContext());
				if (viewY < height && mAeroSnap != null && mActionBarDraggable) {
					mAeroSnap.dispatchTouchEvent(event);
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
	
	static int[] old_layout;

	private static boolean saveNonMaximizedLayout(Window window) {
		final WindowManager.LayoutParams params = window.getAttributes();
		int[] layout = { params.x, params.y, params.width, params.height };
		old_layout = layout;
		return true;
	}
	
	private static boolean restoreNonMaximizedLayout(Window window) {
		WindowManager.LayoutParams params = window.getAttributes();
		params.x = old_layout[0];
		params.y = old_layout[1];
		params.width = old_layout[2];
		params.height = old_layout[3];
		params.gravity = Gravity.LEFT | Gravity.TOP;
		window.setAttributes(params);
		return true;
	}

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
				if (intent.getAction().equals(Intent.ACTION_CONFIGURATION_CHANGED)) {
					Configuration config = window.getContext().getResources().getConfiguration();
					if (config.orientation != mPreviousOrientation) {
						WindowManager.LayoutParams paramz = window.getAttributes();
						final int old_x = paramz.x;
						final int old_y = paramz.y;
						final int old_height = paramz.height;
						final int old_width = paramz.width;
						paramz.x = old_y;
						paramz.y = old_x;
						paramz.width = old_height;
						paramz.height = old_width;
						window.setAttributes(paramz);
						mPreviousOrientation = config.orientation;
					}
					return;
				}
				if (intent.getStringExtra(INTENT_APP_PKG).equals(
						window.getContext().getApplicationInfo().packageName)) {
					setLayoutPositioning(window);
				}
			}
		};
		IntentFilter filters = new IntentFilter();
		filters.addAction(Common.REFRESH_APP_LAYOUT);
		filters.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
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
}
