package com.zst.xposed.halo.floatingwindow.hooks;

import com.zst.xposed.halo.floatingwindow.Common;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class TestingSettingHook {
	
	public static void handleLoadPackage(LoadPackageParam lpp) {
		if (lpp.packageName.equals(Common.THIS_PACKAGE_NAME)) {
			Class<?> hookClass = XposedHelpers.findClass("com.zst.xposed.halo.floatingwindow.TestingActivity",
					lpp.classLoader);
			XposedBridge.hookAllMethods(hookClass, "initXposedLoaded",
					XC_MethodReplacement.returnConstant(Boolean.TRUE));
		}
	}
}
