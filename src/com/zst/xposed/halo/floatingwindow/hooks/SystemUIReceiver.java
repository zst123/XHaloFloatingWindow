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
import android.util.Log;

import com.zst.xposed.halo.floatingwindow.Common;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class SystemUIReceiver {
	
	private static Object iWindowManager;
	private static ActivityManager iActivityManager;
	private static Context mSystemContext; // SystemUI Context
	public static int mLastTaskId;
	
	/*
	 * Catch the method if a throwable appears so the SystemUI wouldn't
	 * continuously force-close
	 */
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
				IntentFilter filters = new IntentFilter();
				filters.addAction(Common.CHANGE_APP_FOCUS);
				mSystemContext.registerReceiver(mIntentReceiver, filters, null, null);
			}
		});
	}
	
	/*
	 * We use a broadcast receiver to change app focus since this requires System-specific
	 * permissions. I used SystemUI since I can't find a universal Android System context
	 * that is compatible with all devices
	 * */
	final static BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			IBinder token = (IBinder) XposedHelpers.callMethod(intent, "getExtra", Common.INTENT_APP_TOKEN);
			int taskId = intent.getIntExtra(Common.INTENT_APP_ID, 0);
			
			
			iWindowManager = getIWindowManagerProxy();
			iActivityManager = (ActivityManager) mSystemContext
					.getSystemService(Context.ACTIVITY_SERVICE);
			
			try {
				Class<?>[] classes = { IBinder.class, Boolean.class };
				XposedHelpers.callMethod(iWindowManager, "setFocusedApp", classes, token, false);
				// iWindowManager.setFocusedApp(token, false);
			} catch (Exception e) {
				XposedBridge.log(Common.LOG_TAG + "Cannot change App Focus");
				XposedBridge.log(e);
				Log.d("test1", "CANNOT CHANGE APP FOCUS", e);
			}
			
			mLastTaskId = taskId;
			final long origId = Binder.clearCallingIdentity();
			try {
				iActivityManager.moveTaskToFront(taskId, ActivityManager.MOVE_TASK_NO_USER_ACTION);
			} catch (Exception e) {
				XposedBridge.log(Common.LOG_TAG + "Cannot move task to front");
				XposedBridge.log(e);
				Log.e("test1", "Cannot move the activity to front", e);
			}
			Binder.restoreCallingIdentity(origId);
		}
	};
	
	// http://stackoverflow.com/questions/9604644/how-do-i-get-a-reference-to-connectivityservice-object
	static Object getIWindowManagerProxy() {
		Class<?> serviceManagerClass = XposedHelpers.findClass("android.os.ServiceManager", null);
		IBinder binderProxy = (IBinder) XposedHelpers.callStaticMethod(serviceManagerClass,
				"getService", "window");
		// ServiceManager.getService("window");
		
		/* Now use pass the ServiceManager BinderProxy to the 'asInterface'
		 *  method of the interface Stub inner class */
		Class<?> stubClass = XposedHelpers.findClass("android.view.IWindowManager$Stub", null);
		return XposedHelpers.callStaticMethod(stubClass, "asInterface",
				new Class[] { IBinder.class }, binderProxy);
		// IWindowManager.Stub.asInterface(binderPRoxy);
	}
}
