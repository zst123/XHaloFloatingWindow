package com.zst.xposed.halo.floatingwindow.helpers;

import java.util.HashMap;
import java.util.LinkedHashSet;

import com.zst.xposed.halo.floatingwindow.MainXposed;
import com.zst.xposed.halo.floatingwindow.R;

import de.robv.android.xposed.XposedHelpers;
import android.animation.LayoutTransition;
import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

public abstract class MultiWindowRecentsManager extends PopupWindow {
	final LayoutInflater mLayoutInflater;
	final Context mContext;
	final LinearLayout mView;
	final ActivityManager mActivityManager;
	final PackageManager mPackageManager;
	HashMap<String, Integer> mPersistentIdList;
	boolean isLoadingList;
	
	public MultiWindowRecentsManager(Context context) {
		this(context, (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE),
				LayoutInflater.from(context), context.getPackageManager());
	}
	
	public MultiWindowRecentsManager(Context context, ActivityManager am, LayoutInflater inflator,
			PackageManager pm) {
		super(context);
		mContext = context;
		mLayoutInflater = inflator;
		mActivityManager = am;
		mPackageManager = pm;
		
		mView = new LinearLayout(context);
		mView.setOrientation(LinearLayout.VERTICAL);
		mView.setBackgroundColor(0xFFdddddd);
		
		LayoutTransition lt = new LayoutTransition();
	    mView.setLayoutTransition(lt);
	    
		FrameLayout frame = new FrameLayout(context);
		frame.addView(mView);
		
		final int paddings_sides = Util.dp(8, mContext);
		frame.setPadding(paddings_sides, 0, paddings_sides, 0);
		
		setContentView(frame);
		
		XposedHelpers.callMethod(this, "setWindowLayoutType", WindowManager.LayoutParams.TYPE_PHONE);
		// setWindowLayoutType(WindowManager.LayoutParams.TYPE_PHONE);
		setWindowLayoutMode(ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		setAnimationStyle(0);
		setOutsideTouchable(true);
		setFocusable(true);
	}
	
	public void refreshList(final LinkedHashSet<String> list) {
		if (isLoadingList || list == null) return;
		
		mView.removeAllViews();
		mView.addView(createTitle());
		new Thread(new Runnable() {
			@Override
			public void run() {
				isLoadingList = true;
				for (final String pkg : list) {
					try {
						final ApplicationInfo ai = mPackageManager.getApplicationInfo(pkg, 0);
						final Drawable icon = ai.loadIcon(mPackageManager);
						final String label = ai.loadLabel(mPackageManager).toString();
						new Handler(mContext.getMainLooper()).post(new Runnable() {
							@Override
							public void run() {
								mView.addView(createItemView(pkg, icon, label));
							}
						});
					} catch (Exception e) {
					}
				}
				isLoadingList = false;
			}
		}).start();
	}
	
	private View createItemView(final String pkg, Drawable icon, String text) {
		final Drawable drawable_bg = MainXposed.sModRes.getDrawable(R.drawable.bg_card_ui);
		final Drawable drawable_close = MainXposed.sModRes.getDrawable(R.drawable.blacklist_cancel);
		
		final XmlResourceParser parser = MainXposed.sModRes.getLayout(R.layout.multiwindow_recents_item);
		final View v = mLayoutInflater.inflate(parser, null);
		View bg_view = v.findViewById(android.R.id.background);
		ImageView kill_button = (ImageView) v.findViewById(android.R.id.button1);
		ImageView icon_view = (ImageView) v.findViewById(android.R.id.icon);
		TextView label_view = (TextView) v.findViewById(android.R.id.text1);
		
		Util.setBackgroundDrawable(bg_view, drawable_bg);
		kill_button.setImageDrawable(drawable_close);
		icon_view.setImageDrawable(icon);
		label_view.setText(text);
		
		kill_button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View clicked_view) {
				removeApp(pkg);
				mView.removeView(v);
			}
		});
		
		v.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					v.setAlpha(0.7f);
					break;
				case MotionEvent.ACTION_MOVE:
					break;
				default:
					v.setAlpha(1f);
				}
				return false;
			}
		});
		v.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startApp(pkg);
				dismiss();
			}
		});
		return v;
	}
	
	private View createTitle() {
		// TODO use xml
		TextView ab = new TextView(mContext);
		ab.setBackgroundColor(0xFF222222);
		ab.setGravity(Gravity.CENTER);
		ab.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18.0f);
		ab.setText("Switch Applications");
		ab.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Util.dp(
				48, mContext)));
		return ab;
	}
	
	private void removeApp(final String pkg) {
		XposedHelpers.callMethod(mActivityManager, "removeTask", getRunningId(pkg), 0x0);
		// mActivityManager.removeTask(getRunningId(pkg), 0x0);
		onRemoveApp(pkg);
	}
	
	public abstract void onRemoveApp(String pkg);
	
	private int getRunningId(String app_pkg) {
		for (RecentTaskInfo info : mActivityManager.getRecentTasks(50,
				ActivityManager.RECENT_IGNORE_UNAVAILABLE)) {
			if (info.baseIntent != null) {
				final String name = info.baseIntent.getPackage();
				if (!TextUtils.isEmpty(name) && name.equals(app_pkg)) {
					return info.id;
				}
			}
		}
		return -1;
	}
	
	private void startApp(String pkg) {
		try {
			Intent i = mContext.getPackageManager().getLaunchIntentForPackage(pkg);
			i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			mContext.startActivity(i);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void display(View parent) {
		showAtLocation(parent, Gravity.CENTER, 0, 0);
		
		AlphaAnimation animation = new AlphaAnimation(0.0f, 1.0f);
		animation.setDuration(400);
		mView.startAnimation(animation);
	}
	
	@Override
	public void dismiss() {
		AlphaAnimation animation = new AlphaAnimation(1.0f, 0.0f);
		animation.setStartOffset(200);
		animation.setDuration(400);
		animation.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
			}
			
			@Override
			public void onAnimationRepeat(Animation animation) {
			}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				MultiWindowRecentsManager.super.dismiss();
			}
		});
		mView.startAnimation(animation);
	}
	
	/*
	private Bitmap getThumbnail(final String pkg, boolean reset_cache) {
		if (reset_cache) {
			mPersistentIdList = null;
		}
		if (mPersistentIdList == null) {
			mPersistentIdList = new HashMap<String, Integer>();
		}
		
		int persistentId = 0;
		
		if (mPersistentIdList.containsKey(pkg)) {
			persistentId = mPersistentIdList.get(pkg);
		} else {
			for (RecentTaskInfo info : mActivityManager.getRecentTasks(50,
					ActivityManager.RECENT_IGNORE_UNAVAILABLE)) {
				if (info.baseIntent != null) {
					final String name = info.baseIntent.getPackage();
					final int id = info.persistentId;
					if (!TextUtils.isEmpty(name)) {
						mPersistentIdList.put(name, id);
						if (name.equals(pkg)) {
							persistentId = id;
							break;
						}
					}
				}
			}
		}
		try {
			return mActivityManager.getTaskThumbnails(persistentId).mainThumbnail;
		} catch (Exception e) {
			return null;
		}
	}
	*/
}
