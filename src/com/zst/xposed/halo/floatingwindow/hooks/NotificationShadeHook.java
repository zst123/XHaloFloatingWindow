package com.zst.xposed.halo.floatingwindow.hooks;

import static de.robv.android.xposed.XposedHelpers.findClass;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.zst.xposed.halo.floatingwindow.Common;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.StatusBarManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class NotificationShadeHook {

	static boolean mLongPressEnabled;
	static boolean mSinglePressEnabled = true;
	
	public static void hook(final LoadPackageParam lpp, final XSharedPreferences pref) {
		if (!lpp.packageName.equals("com.android.systemui")) return;
		pref.reload();
		mLongPressEnabled = pref.getBoolean(Common.KEY_NOTIFICATION_LONGPRESS_OPTION,
				Common.DEFAULT_NOTIFICATION_LONGPRESS_OPTION);
		if (!mLongPressEnabled) return;
		mSinglePressEnabled = pref.getBoolean(Common.KEY_NOTIFICATION_SINGLE_CLICK_HALO,
				Common.DEFAULT_NOTIFICATION_SINGLE_CLICK_HALO);
		
		Class<?> baseStatusBar = findClass("com.android.systemui.statusbar.BaseStatusBar",
				lpp.classLoader);
		try {
			injectViewTag(baseStatusBar);
		} catch (Throwable e) {
			XposedBridge.log(Common.LOG_TAG + "(injectViewTag)");
			XposedBridge.log(e);
		}
		
		try {
			hookLongPressNotif(baseStatusBar);
		} catch (Throwable e) {
			XposedBridge.log(Common.LOG_TAG + "(NotificationHook)");
			XposedBridge.log(e);
		}
		
	}

	private static void hookLongPressNotif(Class<?> baseStatusBar) {
		
		XposedBridge.hookAllMethods(baseStatusBar, "getNotificationLongClicker",
				new XC_MethodReplacement() {
			protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
				final Object thiz = param.thisObject;
				final Context mContext = (Context) XposedHelpers.findField(
						thiz.getClass(), "mContext").get(thiz);
				return new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(final View v) {
						try {
							final Object entry = v.getTag();
							
							final Object sbn = entry.getClass().
									getDeclaredField(("notification")).get(entry);
							
							final String packageNameF = (String) sbn.getClass()
									.getDeclaredField(("pkg")).get(sbn);
							
							final Notification n = (Notification) sbn.getClass()
									.getDeclaredField(("notification")).get(sbn);
							
							final PendingIntent contentIntent = n.contentIntent;
							
							if (packageNameF == null) return false;
							if (v.getWindowToken() == null) return false;

							PopupMenu popup = new PopupMenu(mContext, v);
							popup.getMenu().add("App info");
							if (!mSinglePressEnabled) {
								popup.getMenu().add("Open in Halo");
							} else {
								popup.getMenu().add("Open Normally");
							}
							//TODO put in strings.xml
							popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
								public boolean onMenuItemClick(MenuItem item) {
									if (item.getTitle().equals("App info")) {
										Intent intent = new Intent(
												Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
												Uri.fromParts("package", packageNameF,
														null));
										intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
													mContext.startActivity(intent);
													closeNotificationShade(mContext);
									} else if (item.getTitle().equals("Open in Halo")) {
										launchFloating(contentIntent, mContext);
										closeNotificationShade(mContext);
									} else if (item.getTitle().equals("Open Normally")) {
										launch(new Intent(), contentIntent, mContext);
										closeNotificationShade(mContext);
									} else {
										return false;
									}
									return true;
								}
							});
							popup.show();
							return true;
						} catch (Exception e) {
							return false;
						}
					}
				};
			}
		});
		
	}
	
	private static void injectViewTag(Class<?> baseStatusBar) {
		XposedBridge.hookAllMethods(baseStatusBar, "inflateViews", new XC_MethodHook() {
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				final Object entry = param.args[0];
				Class<?> entryClazz = entry.getClass();
				Field fieldRow = entryClazz.getDeclaredField(("row"));
				View newRow = (View) fieldRow.get(entry);
				newRow.setTag(entry);
				if (mSinglePressEnabled) {
					View content = newRow.findViewById(newRow.getResources()
							.getIdentifier("content", "id", "com.android.systemui"));
					content.setOnClickListener(new View.OnClickListener(){
						@Override
						public void onClick(View v) {
							try {
								final Object sbn = entry.getClass()
										.getDeclaredField(("notification")).get(entry);
								final String packageNameF = (String) sbn.getClass()
										.getDeclaredField(("pkg")).get(sbn);
								final Notification n = (Notification) sbn.getClass()
										.getDeclaredField(("notification")).get(sbn);
								if (packageNameF == null) return;
								if (v.getWindowToken() == null) return;
								launchFloating(n.contentIntent, v.getContext());
								closeNotificationShade(v.getContext());
							} catch (Exception e) {
								android.widget.Toast.makeText(v.getContext(),
										"(XHFW) Error Opening Notification : " + e.toString(),
										android.widget.Toast.LENGTH_SHORT).show();
							}
						}
					});
				}
				fieldRow.set(entry, newRow);
			}
		});
	}
	
	private static void launchFloating(PendingIntent pIntent, Context mContext) { 
		Intent intent = new Intent();
		intent.addFlags(Common.FLAG_FLOATING_WINDOW);
		// intent.setFlags(intent.getFlags() &
		// ~Intent.FLAG_ACTIVITY_SINGLE_TOP);
		// intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
		// intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		// intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
		// intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		launch(intent, pIntent, mContext);
	}
	private static void launch(Intent intent, PendingIntent pIntent, Context mContext) { 
		try {
			android.app.ActivityManagerNative.getDefault().resumeAppSwitches();
			android.app.ActivityManagerNative.getDefault().dismissKeyguardOnNextActivity();
			pIntent.send(mContext, 0, intent);
		} catch (Exception e) {
			android.widget.Toast.makeText(mContext, "XHalo can't be opened : " + e.toString(),
					android.widget.Toast.LENGTH_SHORT).show();
		}
	}
	
	private static void closeNotificationShade(Context c) {
		final StatusBarManager statusBar = (StatusBarManager) c.getSystemService("statusbar");
		if (statusBar == null) return;
		try {
			statusBar.collapse();
		} catch (Throwable e) { // OEM's might remove this expand method.
			try { // 4.2.2 (later builds) changed method name
				Method showsb = statusBar.getClass().getMethod("collapsePanels");
				showsb.invoke(statusBar);
			} catch (Throwable e2) { // else No Hope! Just leave it :P
			}
		}
	}
	
}
