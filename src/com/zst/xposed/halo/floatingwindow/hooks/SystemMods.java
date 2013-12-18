package com.zst.xposed.halo.floatingwindow.hooks;

import static de.robv.android.xposed.XposedHelpers.findClass;

import com.zst.xposed.halo.floatingwindow.Common;

import android.app.ActivityManager;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class SystemMods {
	
	public static void handleLoadPackage(LoadPackageParam llpp, XSharedPreferences pref)
			throws Throwable {
		if (pref.getBoolean(Common.KEY_SYSTEM_PREVENT_HOME_TO_FRONT,
				Common.DEFAULT_SYSTEM_PREVENT_HOME_TO_FRONT)) {
			hookActivityManagerNative(llpp);
		}
	}
	
	/* Force Apps to never bring a home to front */
	public static void hookActivityManagerNative(final LoadPackageParam lpp) {
		XposedBridge.hookAllMethods(findClass("android.app.ActivityManager", lpp.classLoader),
				"moveTaskToFront", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if ((Integer) param.args[1] == ActivityManager.MOVE_TASK_WITH_HOME) {
					param.args[1] = ActivityManager.MOVE_TASK_NO_USER_ACTION;
				}
			}
		});
	}
	
}
