package com.zst.xposed.halo.floatingwindow.hooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.zst.xposed.halo.floatingwindow.Common;
import com.zst.xposed.halo.floatingwindow.R;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.widget.RemoteViews;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import static de.robv.android.xposed.XposedHelpers.findClass;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class StatusbarTaskbar {
	private static final int NOTIFICATION_ID_RUNNING = 0xABCDEF;
	private static final int NOTIFICATION_ID_PINNED = 0xFEDCBA;
	
	private static XSharedPreferences mStatusBarApps;
	private static NotificationManager mNotificationManager;
	private static List<RunningTaskInfo> mRunningAppsList;
	private static int mNumber;
	private static boolean mHideIcon;
	
	public static void handleLoadPackage(LoadPackageParam lpp, final XSharedPreferences main_pref) {
		if (!lpp.packageName.equals("com.android.systemui")) return;
		
		if (mStatusBarApps == null)
			mStatusBarApps = new XSharedPreferences(Common.THIS_PACKAGE_NAME,
					Common.PREFERENCE_STATUSBAR_LAUNCHER_FILE);
		
		if (!main_pref.getBoolean(Common.KEY_STATUSBAR_TASKBAR_ENABLED, Common.DEFAULT_STATUSBAR_TASKBAR_ENABLED))
			return;
		
		mNumber = main_pref.getInt(Common.KEY_STATUSBAR_TASKBAR_NUMBER, 
				Common.DEFAULT_STATUSBAR_TASKBAR_NUMBER);
		
		mHideIcon = main_pref.getBoolean(Common.KEY_STATUSBAR_TASKBAR_HIDE_ICON,
				Common.DEFAULT_STATUSBAR_TASKBAR_HIDE_ICON);
		
		final Class<?> hookClass = findClass("com.android.systemui.statusbar.phone.PhoneStatusBarView", lpp.classLoader);
		XposedBridge.hookAllConstructors(hookClass, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				final Context context = (Context) param.args[0];
				context.registerReceiver(new BroadcastReceiver() {
					@Override
					public void onReceive(Context ctx, Intent intent) {
						setup(context);
					}
				}, new IntentFilter(Common.STATUSBAR_TASKBAR_REFRESH), null, null);
				
				final PackageManager pm = context.getPackageManager();
				context.registerReceiver(new BroadcastReceiver() {
					@Override
					public void onReceive(Context ctx, Intent intent) {
						String package_name = intent.getStringExtra(Common.INTENT_APP_ID);
						intent = pm.getLaunchIntentForPackage(package_name);
						if (intent != null) {
							intent.addFlags(Common.FLAG_FLOATING_WINDOW);
							intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							context.startActivity(intent);
						}
					}
				}, new IntentFilter(Common.STATUSBAR_TASKBAR_LAUNCH), null, null);
				
				if (main_pref.getBoolean(Common.KEY_STATUSBAR_TASKBAR_RUNNING_APPS_ENABLED,
						Common.DEFAULT_STATUSBAR_TASKBAR_RUNNING_APPS_ENABLED)) {
					final Handler handler = new Handler(context.getMainLooper());
					final Runnable runnable = new Runnable() {
						@Override
						public void run() {
							ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
							mRunningAppsList = am.getRunningTasks(5);
							handler.postDelayed(this, 15000);
							refreshRunningApps(context);
						}
					};
					handler.postDelayed(runnable, 15000);
					
					final IntentFilter filter = new IntentFilter();
					filter.addAction(Intent.ACTION_SCREEN_ON);
					filter.addAction(Intent.ACTION_SCREEN_OFF);
					context.registerReceiver(new BroadcastReceiver() {
						@Override
						public void onReceive(Context ctx, Intent intent) {
							if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)){
								handler.removeCallbacks(runnable);
							} else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)){
								handler.postDelayed(runnable, 15000);
							}
						}
					}, filter, null, null);
				}
				clearNotifications(context, NOTIFICATION_ID_RUNNING);
				setup(context);
			}
		});
	}
	
	
	public static void setup(Context context) {
		if (context == null) return;
		
		clearNotifications(context, NOTIFICATION_ID_PINNED);
		
		RemoteViews view = new RemoteViews(Common.THIS_PACKAGE_NAME,
				R.layout.view_statusbar_taskbar_holder);
		ArrayList<Map.Entry<String, ?>> sorted_entries = getPinnedApps();
		int index = 100;
		for (Map.Entry<String, ?> entry : sorted_entries) {
			AppIconButton aib = new AppIconButton(context, index, entry.getKey());
			view.addView(R.id.taskbar_contents, aib.view);
			index++;
			if (index == mNumber + 100) {
				break;
			}
		}
		if (index == 100)
			return; // no items are added
		addNotification(context, view, NOTIFICATION_ID_PINNED);
		
	}
	
	public static void refreshRunningApps(Context context) {
		if (context == null) return;
		
		clearNotifications(context, NOTIFICATION_ID_RUNNING);
		
		RemoteViews rview = new RemoteViews(Common.THIS_PACKAGE_NAME,
				R.layout.view_statusbar_taskbar_holder);
		int index = 200;
		for (RunningTaskInfo item : mRunningAppsList) {
			AppIconButton aib = new AppIconButton(context, index,
					item.topActivity.getPackageName());
			rview.addView(R.id.taskbar_contents, aib.view);
			index++;
			if (index == mNumber + 200) {
				break;
			}
		}
		addNotification(context, rview, NOTIFICATION_ID_RUNNING);
	}
	
	@SuppressWarnings("deprecation")
	public static void addNotification(Context context, RemoteViews view, int id) {
		Notification.Builder nb = new Notification.Builder(context)
				.setContent(view)
				.setSmallIcon(mHideIcon ? android.R.drawable.list_selector_background :
					android.R.drawable.presence_invisible)
				.setOngoing(true)
				.setWhen(0);
		Notification notification;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			nb.setPriority(Notification.PRIORITY_MAX);
			notification = nb.build();
		} else {
			notification = nb.getNotification();
		}
		getNotificationManager(context).notify(id, notification);
	}
	
	public static void clearNotifications(Context context, int id) {
		if (context != null) {
			getNotificationManager(context).cancel(id);
		}
	}
	
	private static NotificationManager getNotificationManager(Context ctx) {
		if (mNotificationManager == null) {
			mNotificationManager = (NotificationManager) ctx
					.getSystemService(Context.NOTIFICATION_SERVICE);
		}
		return mNotificationManager;
	}
	
	private static ArrayList<Map.Entry<String, ?>> getPinnedApps() {
		mStatusBarApps.reload();
		Map<String, ?> apps = mStatusBarApps.getAll();
		ArrayList<Map.Entry<String, ?>> array = new ArrayList<Map.Entry<String, ?>>();
		for (Map.Entry<String, ?> entry : apps.entrySet()) {
			array.add(0, entry);
		}
		return array;
	}
	
	private static class AppIconButton {
		Drawable icon;
		RemoteViews view;
		
		public AppIconButton(Context context, int id_number, String package_name) {
			view = new RemoteViews(Common.THIS_PACKAGE_NAME,
					R.layout.view_statusbar_taskbar_icon);
			
			try {
				final PackageManager pm = context.getPackageManager();
				final ApplicationInfo ai = pm.getApplicationInfo(package_name, 0);
				icon = ai.loadIcon(pm);
			} catch (Exception e) {
				icon = context.getResources().getDrawable(android.R.drawable.sym_def_app_icon);
			}
			final Intent intent = new Intent(Common.STATUSBAR_TASKBAR_LAUNCH);
			intent.putExtra(Common.INTENT_APP_ID, package_name);
			// Send intent to launch a halo window. We cannot do this
			// directly as PendingIntent will ignore our setFlags
			final PendingIntent configPendingIntent = PendingIntent.getBroadcast(context, id_number, intent,
					PendingIntent.FLAG_UPDATE_CURRENT);
			// Thanks: http://stackoverflow.com/questions/3140072/
			// ID number is needed to prevent PendingIntents that are outstanding all at once.
			view.setOnClickPendingIntent(R.id.icon_button, configPendingIntent);
			view.setImageViewBitmap(R.id.icon_button, ((BitmapDrawable) icon).getBitmap());
		}
	}
}