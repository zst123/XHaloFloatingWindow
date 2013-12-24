package com.zst.xposed.halo.floatingwindow.hooks;

import static de.robv.android.xposed.XposedHelpers.findClass;
import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.os.ServiceManager;
import android.util.Log;
import android.view.IWindowManager;

import com.zst.xposed.halo.floatingwindow.Common;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class SystemUIReceiver {
	
	private static IWindowManager iWindowManager;
	private static ActivityManager iActivityManager;
	private static Context mSystemContext; // SystemUI Context
	
	public static void handleLoadPackage(LoadPackageParam lpp) {
		if (!lpp.packageName.equals("com.android.systemui")) return;
		
		try {
			focusChangeContextFinder(lpp);
		} catch (Throwable e) {
			XposedBridge.log(Common.LOG_TAG + "Movable / focusChangeContextFinder");
			XposedBridge.log(e);
		}
	}
	
	private static void focusChangeContextFinder(LoadPackageParam l) throws Throwable {
		Class<?> hookClass = findClass("com.android.systemui.SystemUIService", l.classLoader);
		XposedBridge.hookAllMethods(hookClass, "onCreate", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				Service thiz = (Service) param.thisObject;
				mSystemContext = thiz.getApplicationContext();
				// Gets SystemUI Context which has the permissions
				IntentFilter filters = new IntentFilter();
				filters.addAction(Common.CHANGE_APP_FOCUS);
				mSystemContext.registerReceiver(mIntentReceiver, filters, null, null);
			}
		});
	}
	
	final static BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			IBinder token = (IBinder) intent.getExtra(Common.INTENT_APP_TOKEN);
			int taskId = intent.getIntExtra(Common.INTENT_APP_ID, 0);
			
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
			
			String notification_hide = intent.getStringExtra(Common.INTENT_APP_NOTIFICATION_HIDE);
			if (notification_hide != null) {
				context.sendBroadcast(new Intent(notification_hide));
			}
		}
	};
	
}
