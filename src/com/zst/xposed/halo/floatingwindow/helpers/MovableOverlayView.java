package com.zst.xposed.halo.floatingwindow.helpers;

import com.zst.xposed.halo.floatingwindow.Common;
import com.zst.xposed.halo.floatingwindow.MainXposed;
import com.zst.xposed.halo.floatingwindow.R;
import com.zst.xposed.halo.floatingwindow.preferences.TitleBarSettingsActivity;

import de.robv.android.xposed.XposedHelpers;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

@SuppressLint("ViewConstructor")
// We only create this view programatically, so the default
// constructor used by XML inflating is not needed

public class MovableOverlayView extends RelativeLayout {
	
	public static final int ID_OVERLAY_VIEW = 1000000;
	
	/* Corner Button Actions Constants */
	private static final int ACTION_CLICK_TRIANGLE = 0x0;
	private static final int ACTION_LONGPRESS_TRIANGLE = 0x1;
	private static final int ACTION_CLICK_QUADRANT = 0x2;
	private static final int ACTION_LONGPRESS_QUADRANT = 0x3;
	
	// (constants) App Objects
	private final MainXposed mMainXposed;
	private final Activity mActivity;
	private final Resources mResource;
	private final AeroSnap mAeroSnap;
	private final SharedPreferences mPref;
	
	// Views
	public final View mDragToMoveBar;
	public final View mQuadrant;
	public final View mTriangle;
	public final ImageView mBorderOutline;
	
	public RelativeLayout mTitleBarHeader;
	public ImageButton mTitleBarClose;
	public ImageButton mTitleBarMin;
	public ImageButton mTitleBarMax;
	public ImageButton mTitleBarMore;
	public TextView mTitleBarTitle;
	
	private View mTransparencyDialog;
	
	/* Title Bar */
	private final int mTitleBarHeight;
	private final int mTitleBarDivider;
	private final int mTitleBarIconType;
	private final boolean mLiveResizing;
	
	/**
	 * Create the overlay view for Movable and Resizable feature
	 * @param activity - the current activity
	 * @param resources - resource from the module
	 * @param pref - preference of the module
	 * @param aerosnap - an aerosnap instance
	 */
	public MovableOverlayView(MainXposed main, Activity activity, Resources resources,
			SharedPreferences pref, AeroSnap aerosnap) {
		super(activity);
		
		// Set the params
		mMainXposed = main;
		mActivity = activity;
		mResource = resources;
		mPref = pref;
		mAeroSnap = aerosnap;
		
		/* get the layout from our module. we cannot just use the R reference
		 * since the layout is from the module, not the current app we are
		 * modifying. thus, we use a parser */
		try {
			Context module_context = activity.createPackageContext(Common.THIS_PACKAGE_NAME,
					Context.CONTEXT_IGNORE_SECURITY);
			LayoutInflater.from(module_context).inflate(R.layout.movable_window, this);
		} catch (Exception e) {
			XmlResourceParser parser = resources.getLayout(R.layout.movable_window);
			activity.getWindow().getLayoutInflater().inflate(parser, this);
		}
		
		// Thanks to this post for some inspiration:
		// http://sriramramani.wordpress.com/2012/07/25/infamous-viewholder-pattern/
		
		setId(ID_OVERLAY_VIEW);
		setRootNamespace(false);
		
		mDragToMoveBar = findViewByIdHelper(this, R.id.movable_action_bar, "movable_action_bar");
		mTriangle = findViewByIdHelper(this, R.id.movable_corner, "movable_corner");
		mQuadrant = findViewByIdHelper(this, R.id.movable_quadrant, "movable_quadrant");
		mBorderOutline = (ImageView) findViewByIdHelper(this, R.id.movable_background, "movable_background");
		mBorderOutline.bringToFront();
		
		// set preferences values
		mTitleBarIconType = mPref.getInt(Common.KEY_WINDOW_TITLEBAR_ICON_TYPE,
				Common.DEFAULT_WINDOW_TITLEBAR_ICONS_TYPE);
		boolean titlebar_enabled = mTitleBarIconType != 0;
		boolean titlebar_separator_enabled = mPref.getBoolean(Common.KEY_WINDOW_TITLEBAR_SEPARATOR_ENABLED,
				Common.DEFAULT_WINDOW_TITLEBAR_SEPARATOR_ENABLED);
		mTitleBarHeight = !titlebar_enabled ? 0 : Util.realDp(
				mPref.getInt(Common.KEY_WINDOW_TITLEBAR_SIZE, Common.DEFAULT_WINDOW_TITLEBAR_SIZE),
				activity);
		mTitleBarDivider = !titlebar_separator_enabled ? 0 : Util.realDp(
				mPref.getInt(Common.KEY_WINDOW_TITLEBAR_SEPARATOR_SIZE,
				Common.DEFAULT_WINDOW_TITLEBAR_SEPARATOR_SIZE), activity);
		mLiveResizing = mPref.getBoolean(Common.KEY_WINDOW_RESIZING_LIVE_UPDATE,
				Common.DEFAULT_WINDOW_RESIZING_LIVE_UPDATE);
		
		// init stuff
		initCornersViews();
		
		setRootNamespace(true);
		// After initializing everything, set this to tell findViewById to skip
		// our layout. We do this to prevent id's conflicting with the current app.
	}
	
	private View findViewByIdHelper(View view, int id, String tag) {
		View v = view.findViewById(id);
		if (v == null) {
			v = findViewWithTag(view, tag);
		}
		return v;
    }
	
	private View findViewWithTag(View view, String text) {
		if (view.getTag() instanceof String) {
			if (((String) view.getTag()).equals(text))
				return view;
		}
		if (view instanceof ViewGroup) {
			final ViewGroup group = (ViewGroup) view;
			for (int i = 0; i < group.getChildCount(); ++i) {
				final View child = group.getChildAt(i);
				final View found = findViewWithTag(child, text);
				if (found != null)
					return found;
			}
		}
        return null;
    }
	
	/**
	 * Initializes the triangle and quadrant's transparency, color, size etc.
	 * @since When inflating, the system will find the drawables in the CURRENT
	 *        app, which will FAIL since the drawables are in the MODULE. So we
	 *        have no choice but to do this programatically
	 */
	private void initCornersViews() {
		Drawable triangle_background = mResource.getDrawable(R.drawable.movable_corner);
		Drawable quadrant_background = mResource.getDrawable(R.drawable.movable_quadrant);
		
		String color_triangle = mPref.getString(Common.KEY_WINDOW_TRIANGLE_COLOR,
				Common.DEFAULT_WINDOW_TRIANGLE_COLOR);
		if (!color_triangle.equals(Common.DEFAULT_WINDOW_TRIANGLE_COLOR)) { 
			triangle_background.setColorFilter(Color.parseColor("#" + color_triangle),
					Mode.MULTIPLY);
		}
		
		String color_quadrant = mPref.getString(Common.KEY_WINDOW_QUADRANT_COLOR,
				Common.DEFAULT_WINDOW_QUADRANT_COLOR);
		if (!color_quadrant.equals(Common.DEFAULT_WINDOW_QUADRANT_COLOR)) {
			quadrant_background.setColorFilter(Color.parseColor("#" + color_quadrant),
					Mode.MULTIPLY);
		}
		
		float triangle_alpha = mPref.getFloat(Common.KEY_WINDOW_TRIANGLE_ALPHA,
				Common.DEFAULT_WINDOW_TRIANGLE_ALPHA);
		triangle_background.setAlpha((int) (triangle_alpha * 255));
		
		float quadrant_alpha = mPref.getFloat(Common.KEY_WINDOW_QUADRANT_ALPHA,
				Common.DEFAULT_WINDOW_QUADRANT_ALPHA);
		quadrant_background.setAlpha((int) (quadrant_alpha * 255));
		
		Util.setBackgroundDrawable(mTriangle, triangle_background);
		Util.setBackgroundDrawable(mQuadrant, quadrant_background);
		
		int triangle_size = mPref.getInt(Common.KEY_WINDOW_TRIANGLE_SIZE,
				Common.DEFAULT_WINDOW_TRIANGLE_SIZE);
		mTriangle.getLayoutParams().width = triangle_size;
		mTriangle.getLayoutParams().height = triangle_size;
		
		int quadrant_size = mPref.getInt(Common.KEY_WINDOW_QUADRANT_SIZE,
				Common.DEFAULT_WINDOW_QUADRANT_SIZE);
		mQuadrant.getLayoutParams().width = quadrant_size;
		mQuadrant.getLayoutParams().height = quadrant_size;
		
		final boolean triangle_enabled = mPref.getBoolean(Common.KEY_WINDOW_TRIANGLE_ENABLE,
				Common.DEFAULT_WINDOW_TRIANGLE_ENABLE);
		if (triangle_enabled) {
			if (mPref.getBoolean(Common.KEY_WINDOW_TRIANGLE_RESIZE_ENABLED,
					Common.DEFAULT_WINDOW_TRIANGLE_RESIZE_ENABLED)) {
				if (mLiveResizing) {
					mTriangle.setOnTouchListener(new Resizable(mActivity, mActivity.getWindow()));
				} else {
					mTriangle.setOnTouchListener(new OutlineLeftResizable(mActivity, mActivity
							.getWindow()));
				}
			}
			
			if (mPref.getBoolean(Common.KEY_WINDOW_TRIANGLE_DRAGGING_ENABLED,
					Common.DEFAULT_WINDOW_TRIANGLE_DRAGGING_ENABLED)) {
				mTriangle.setOnTouchListener(new Movable(mActivity.getWindow(), mTriangle,
						mAeroSnap));
			}
			
			mTriangle.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					cornerButtonClickAction(ACTION_CLICK_TRIANGLE);
				}
			});
			
			mTriangle.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					cornerButtonClickAction(ACTION_LONGPRESS_TRIANGLE);
					return true;
				}
			});
		} else {
			mTriangle.getLayoutParams().width = 0;
			mTriangle.getLayoutParams().height = 0;
		}
		
		boolean quadrant_enabled = mPref.getBoolean(Common.KEY_WINDOW_QUADRANT_ENABLE,
				Common.DEFAULT_WINDOW_QUADRANT_ENABLE);
		if (quadrant_enabled) {
			if (mPref.getBoolean(Common.KEY_WINDOW_QUADRANT_RESIZE_ENABLED,
					Common.DEFAULT_WINDOW_QUADRANT_RESIZE_ENABLED)) {
				if (mLiveResizing) {
					mQuadrant.setOnTouchListener(new RightResizable(mActivity.getWindow()));
				} else {
					mQuadrant.setOnTouchListener(new OutlineRightResizable(mActivity.getWindow()));
				}
			}
			
			if (mPref.getBoolean(Common.KEY_WINDOW_QUADRANT_DRAGGING_ENABLED,
					Common.DEFAULT_WINDOW_QUADRANT_DRAGGING_ENABLED)) {
				mQuadrant.setOnTouchListener(new Movable(mActivity.getWindow(), mQuadrant,
						mAeroSnap));
			}
			
			mQuadrant.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					cornerButtonClickAction(ACTION_CLICK_QUADRANT);
				}
			});
			
			mQuadrant.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					cornerButtonClickAction(ACTION_LONGPRESS_QUADRANT);
					return true;
				}
			});
		} else {
			mQuadrant.getLayoutParams().width = 0;
			mQuadrant.getLayoutParams().height = 0;
		}
		
		setDragActionBarVisibility(false, true);
		initDragToMoveBar();
		
		if (mPref.getBoolean(Common.KEY_WINDOW_BORDER_ENABLED,
				Common.DEFAULT_WINDOW_BORDER_ENABLED)) {
			final int color = Color.parseColor("#" + mPref.getString(Common.KEY_WINDOW_BORDER_COLOR,
							Common.DEFAULT_WINDOW_BORDER_COLOR));
			final int thickness = mPref.getInt(Common.KEY_WINDOW_BORDER_THICKNESS,
					Common.DEFAULT_WINDOW_BORDER_THICKNESS);
			setWindowBorder(color, thickness);
			mMainXposed.hookActionBarColor.setBorderThickness(thickness);
		}
		
		initTitleBar();
	}
	
	// Corner Buttons (Triangle, Quadrant) Actions.
	private void cornerButtonClickAction(int type_of_action) {
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
			closeApp();
			break;
		case 3: // Transparency Dialog
			showTransparencyDialogVisibility();
			break;
		case 4: // Minimize / Hide Entire App
			mMainXposed.hookMovableWindow.minimizeAndShowNotification(mActivity);
			break;
		case 5: // Drag & Move Bar w/o hiding corner
			setDragActionBarVisibility(true, false);
			break;
		case 6: // Maximize App
			mMainXposed.hookMovableWindow.maximizeApp(mActivity);
			break;
		}
	}
	
	// Create the Titlebar
	private void initTitleBar() {
		final RelativeLayout header = (RelativeLayout) findViewByIdHelper(this,
				R.id.movable_titlebar, "movable_titlebar");
		
		if (mTitleBarHeight == 0) {
			removeView(header);
			return;
		}
		
		final View divider = findViewByIdHelper(header,
				R.id.movable_titlebar_line, "movable_titlebar_line");
		final TextView app_title = (TextView) findViewByIdHelper(header,
				R.id.movable_titlebar_appname, "movable_titlebar_appname");
		final ImageButton max_button = (ImageButton) findViewByIdHelper(header,
				R.id.movable_titlebar_max, "movable_titlebar_max");
		final ImageButton min_button = (ImageButton) findViewByIdHelper(header,
				R.id.movable_titlebar_min, "movable_titlebar_min");
		final ImageButton more_button = (ImageButton) findViewByIdHelper(header,
				R.id.movable_titlebar_more, "movable_titlebar_more");
		final ImageButton close_button = (ImageButton) findViewByIdHelper(header,
				R.id.movable_titlebar_close, "movable_titlebar_close");
	
		app_title.setText(mActivity.getApplicationInfo().loadLabel(mActivity.getPackageManager()));
		
		switch (mTitleBarIconType) {
		case TitleBarSettingsActivity.TITLEBAR_ICON_ORIGINAL:
			close_button.setImageDrawable(mResource.getDrawable(R.drawable.movable_title_close_old));
			max_button.setImageDrawable(mResource.getDrawable(R.drawable.movable_title_max_old));
			min_button.setImageDrawable(mResource.getDrawable(R.drawable.movable_title_min_old));
			more_button.setImageDrawable(mResource.getDrawable(R.drawable.movable_title_more_old));
			break;
		case TitleBarSettingsActivity.TITLEBAR_ICON_BachMinuetInG:
			close_button.setImageDrawable(mResource.getDrawable(R.drawable.movable_title_close));
			max_button.setImageDrawable(mResource.getDrawable(R.drawable.movable_title_max));
			min_button.setImageDrawable(mResource.getDrawable(R.drawable.movable_title_min));
			more_button.setImageDrawable(mResource.getDrawable(R.drawable.movable_title_more));
			break;
		}
		
		RelativeLayout.LayoutParams header_param = (LayoutParams) header.getLayoutParams();
		header_param.height = mTitleBarHeight;
		header.setLayoutParams(header_param);
		
		ViewGroup.LayoutParams divider_param = divider.getLayoutParams();
		divider_param.height = mTitleBarDivider;
		divider.setLayoutParams(divider_param);
		
		String color_str = mPref.getString(Common.KEY_WINDOW_TITLEBAR_SEPARATOR_COLOR,
				Common.DEFAULT_WINDOW_TITLEBAR_SEPARATOR_COLOR);
		divider.setBackgroundColor(Color.parseColor("#" + color_str));
		
		final String item1 = mResource.getString(R.string.dnm_transparency);
		final String menu_item4_sub1 = mResource.getString(R.string.dnm_snap_window_sub1);
		final String menu_item4_sub2 = mResource.getString(R.string.dnm_snap_window_sub2);
		final String menu_item4_sub3 = mResource.getString(R.string.dnm_snap_window_sub3);
		final String menu_item4_sub4 = mResource.getString(R.string.dnm_snap_window_sub4);

		final PopupMenu popupMenu = new PopupMenu(mActivity, more_button);
		final Menu menu = popupMenu.getMenu();
		menu.add(item1);
		menu.add(menu_item4_sub1);
		menu.add(menu_item4_sub2);
		menu.add(menu_item4_sub3);
		menu.add(menu_item4_sub4);
		popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				if (item.getTitle().equals(item1)) {
					showTransparencyDialogVisibility();
				} else if (item.getTitle().equals(menu_item4_sub1)) {
					mAeroSnap.forceSnap(AeroSnap.SNAP_TOP);
				} else if (item.getTitle().equals(menu_item4_sub2)) {
					mAeroSnap.forceSnap(AeroSnap.SNAP_BOTTOM);
				} else if (item.getTitle().equals(menu_item4_sub3)) {
					mAeroSnap.forceSnap(AeroSnap.SNAP_LEFT);
				} else if (item.getTitle().equals(menu_item4_sub4)) {
					mAeroSnap.forceSnap(AeroSnap.SNAP_RIGHT);
				}
				return false;
			}
		});
		
		final View.OnClickListener click = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String tag = (v.getTag() instanceof String) ? (String) v.getTag() : "";
				int id = v.getId();
				
				if (id == R.id.movable_titlebar_close || tag.equals("movable_titlebar_close")) {
					closeApp();
				} else if (id == R.id.movable_titlebar_max || tag.equals("movable_titlebar_max")) {
					mMainXposed.hookMovableWindow.maximizeApp(mActivity);
				} else if (id == R.id.movable_titlebar_min || tag.equals("movable_titlebar_min")) {
					mMainXposed.hookMovableWindow.minimizeAndShowNotification(mActivity);
				} else if (id == R.id.movable_titlebar_more || tag.equals("movable_titlebar_more")) {
					popupMenu.show();
				}
			}
		};
		close_button.setOnClickListener(click);
		max_button.setOnClickListener(click);
		min_button.setOnClickListener(click);
		more_button.setOnClickListener(click);
		header.setOnTouchListener(new Movable(mActivity.getWindow(), mAeroSnap, true));
		
		mTitleBarHeader = header;
		mTitleBarTitle = app_title;
		mTitleBarClose = close_button;
		mTitleBarMin = min_button;
		mTitleBarMax = max_button;
		mTitleBarMore = more_button;
		
		setTitleBarVisibility(true);
	}
	
	// Create the drag-to-move bar
	private void initDragToMoveBar() {
		mDragToMoveBar.setOnTouchListener(new Movable(mActivity.getWindow(), mAeroSnap, true));
		
		TextView dtm_title = (TextView) findViewByIdHelper(mDragToMoveBar,
				R.id.movable_dtm_title, "movable_dtm_title");
		dtm_title.setText(mResource.getString(R.string.dnm_title));
		
		final ImageButton done = (ImageButton) findViewByIdHelper(mDragToMoveBar,
				R.id.movable_done, "movable_done");
		done.setImageDrawable(mResource.getDrawable(R.drawable.movable_done));
		done.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setDragActionBarVisibility(false, true);
			}
		});
		
		final String menu_item1 = mResource.getString(R.string.dnm_transparency);
		final String menu_item3 = mResource.getString(R.string.dnm_minimize);
		final String menu_item2 = mResource.getString(R.string.dnm_close_app);
		final String menu_item4 = mResource.getString(R.string.dnm_snap_window);
		final String menu_item4_sub1 = mResource.getString(R.string.dnm_snap_window_sub1);
		final String menu_item4_sub2 = mResource.getString(R.string.dnm_snap_window_sub2);
		final String menu_item4_sub3 = mResource.getString(R.string.dnm_snap_window_sub3);
		final String menu_item4_sub4 = mResource.getString(R.string.dnm_snap_window_sub4);
		
		final ImageButton overflow = (ImageButton) findViewByIdHelper(mDragToMoveBar,
				R.id.movable_overflow, "movable_overflow");
		overflow.setImageDrawable(mResource.getDrawable(R.drawable.movable_overflow));
		
		final PopupMenu popupMenu = new PopupMenu(overflow.getContext(), overflow);
		Menu menu = popupMenu.getMenu();
		menu.add(menu_item1);
		menu.add(menu_item3);
		menu.add(menu_item2);
		
		SubMenu submenu_item4 = menu.addSubMenu(menu_item4);
		submenu_item4.add(menu_item4_sub1);
		submenu_item4.add(menu_item4_sub2);
		submenu_item4.add(menu_item4_sub3);
		submenu_item4.add(menu_item4_sub4);
		
		overflow.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						if (item.getTitle().equals(menu_item1)) {
							showTransparencyDialogVisibility();
						} else if (item.getTitle().equals(menu_item2)) {
							closeApp();
						} else if (item.getTitle().equals(menu_item3)) {
							mMainXposed.hookMovableWindow.minimizeAndShowNotification(mActivity);
						} else if (item.getTitle().equals(menu_item4_sub1)) {
							mAeroSnap.forceSnap(AeroSnap.SNAP_TOP);
						} else if (item.getTitle().equals(menu_item4_sub2)) {
							mAeroSnap.forceSnap(AeroSnap.SNAP_BOTTOM);
						} else if (item.getTitle().equals(menu_item4_sub3)) {
							mAeroSnap.forceSnap(AeroSnap.SNAP_LEFT);
						} else if (item.getTitle().equals(menu_item4_sub4)) {
							mAeroSnap.forceSnap(AeroSnap.SNAP_RIGHT);
						}
						return false;
					}
				});
				popupMenu.show();
			}
		});
	}
	
	private void showTransparencyDialogVisibility() {
		final RelativeLayout bg = (RelativeLayout) findViewByIdHelper(this, 
				R.id.movable_transparency_holder, "movable_transparency_holder");
		if (mTransparencyDialog == null) {
			XmlResourceParser parser = mResource.getLayout(R.layout.movable_dialog_transparency);
			mTransparencyDialog = mActivity.getWindow().getLayoutInflater().inflate(parser, bg);
			
			final TextView title = (TextView) mTransparencyDialog.findViewById(android.R.id.text1);
			final TextView numb = (TextView) mTransparencyDialog.findViewById(android.R.id.text2);
			final SeekBar bar = (SeekBar) mTransparencyDialog.findViewById(android.R.id.progress);

			title.setText(mResource.getString(R.string.dnm_transparency));
			
			final float current_alpha = mActivity.getWindow().getAttributes().alpha;
			numb.setText((int) (current_alpha * 100) + "%");
			bar.setProgress((int) (current_alpha * 100) - 10);
			bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					final int newProgress = (progress + 10);
					numb.setText(newProgress + "%");
					
					WindowManager.LayoutParams params = mActivity.getWindow().getAttributes();
					params.alpha = newProgress * 0.01f;
					mActivity.getWindow().setAttributes(params);
				}
				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {}
				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {}
			});
			
			bg.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					if (event.getAction() == MotionEvent.ACTION_UP) {
						bg.setVisibility(View.GONE);
					}
					return true;
				}
			});
		}
		bg.setVisibility(View.VISIBLE);
	}
	
	private void setDragActionBarVisibility(boolean visible, boolean with_corner) {
		mDragToMoveBar.setVisibility(visible ? View.VISIBLE : View.GONE);
		if (with_corner) {
			mTriangle.setVisibility(visible ? View.INVISIBLE : View.VISIBLE);
			mQuadrant.setVisibility(visible ? View.INVISIBLE : View.VISIBLE);
		}
	}
	
	public void setTitleBarVisibility(boolean visible) {
		if (mTitleBarHeader != null) {
			mTitleBarHeader.setVisibility(visible ? View.VISIBLE : View.GONE);
			
			final FrameLayout decorView = (FrameLayout) mActivity.getWindow().peekDecorView()
					.getRootView();
			final View child = decorView.getChildAt(0);
			FrameLayout.LayoutParams parammm = (FrameLayout.LayoutParams) child.getLayoutParams();
			parammm.setMargins(0, visible ? mTitleBarHeight : 0, 0, 0);
			child.setLayoutParams(parammm);
		}
	}
	
	public void setWindowBorder(int color, int thickness) {
		if (thickness == 0) {
			mBorderOutline.setBackgroundResource(0);
		} else {
			Util.setBackgroundDrawable(mBorderOutline, Util.makeOutline(color, thickness));
		}
	}
	
	public static final RelativeLayout.LayoutParams getParams() {
		final RelativeLayout.LayoutParams paramz = new RelativeLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		paramz.setMargins(0, 0, 0, 0);
		return paramz;
	}
	
	public void closeApp() {
		try {
			/* Work-around for bug:
			 * When closing a floating window using the titlebar
			 * while the keyboard is open, the floating window
			 * closes but the keyboard remains open on top of
			 * another fullscreen app.
			 */
			InputMethodManager imm = (InputMethodManager)
					mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(mActivity.getCurrentFocus().getWindowToken(), 0);
		} catch (Exception e) {
			//ignore
		}
		if (mPref.getBoolean(Common.KEY_WINDOW_TITLEBAR_SINGLE_WINDOW,
				Common.DEFAULT_WINDOW_TITLEBAR_SINGLE_WINDOW)
				&& Build.VERSION.SDK_INT >= 16) {
			mActivity.finishAffinity();
		} else {
			mActivity.finish();
		}
	}
	
	public void setRootNamespace(boolean isRoot) {
		XposedHelpers.callMethod(this, "setIsRootNamespace", isRoot);
	}
}
