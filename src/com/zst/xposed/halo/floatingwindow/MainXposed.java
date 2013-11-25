package com.zst.xposed.halo.floatingwindow;

import com.zst.xposed.halo.floatingwindow.hooks.HaloFloating;
import com.zst.xposed.halo.floatingwindow.hooks.MovableWindow;
import com.zst.xposed.halo.floatingwindow.hooks.NotificationShadeHook;

import android.content.res.XModuleResources;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class MainXposed implements IXposedHookLoadPackage, IXposedHookZygoteInit,
		IXposedHookInitPackageResources {
	
	public static XModuleResources sModRes;
	static String MODULE_PATH = null;
	static XSharedPreferences mPref;
	
	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		mPref = new XSharedPreferences(Common.THIS_PACKAGE_NAME, Common.PREFERENCE_MAIN_FILE);
		MODULE_PATH = startupParam.modulePath;
	}
	
	@Override
	public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
		MovableWindow.handleLoadPackage(lpparam, mPref);
		HaloFloating.handleLoadPackage(lpparam, mPref);
		NotificationShadeHook.hook(lpparam);
	}

	@Override
	public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {
		sModRes = XModuleResources.createInstance(MODULE_PATH, resparam.res);
		MovableWindow.handleInitPackageResources(sModRes);
	}
	
}
