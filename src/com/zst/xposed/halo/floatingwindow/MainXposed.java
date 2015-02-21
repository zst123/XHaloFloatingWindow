package com.zst.xposed.halo.floatingwindow;

import com.zst.xposed.halo.floatingwindow.hooks.ActionBarColorHook;
import com.zst.xposed.halo.floatingwindow.hooks.HaloFloating;
import com.zst.xposed.halo.floatingwindow.hooks.MovableWindow;
import com.zst.xposed.halo.floatingwindow.hooks.NotificationShadeHook;
import com.zst.xposed.halo.floatingwindow.hooks.RecentAppsHook;
import com.zst.xposed.halo.floatingwindow.hooks.StatusbarTaskbar;
import com.zst.xposed.halo.floatingwindow.hooks.SystemMods;
import com.zst.xposed.halo.floatingwindow.hooks.SystemUIMultiWindow;
import com.zst.xposed.halo.floatingwindow.hooks.SystemUIOutliner;
import com.zst.xposed.halo.floatingwindow.hooks.TestingSettingHook;
import com.zst.xposed.halo.floatingwindow.hooks.ipc.XHFWService;

import android.content.res.XModuleResources;
import android.os.Build;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class MainXposed implements IXposedHookLoadPackage, IXposedHookZygoteInit {
	
	public static XModuleResources sModRes;
	// TODO make local
	public XSharedPreferences mPref;
	public XSharedPreferences mBlacklist;
	public XSharedPreferences mWhitelist;
	
	/* Hook References */
	public MovableWindow hookMovableWindow;
	public HaloFloating hookHaloFloating;
	public ActionBarColorHook hookActionBarColor;
	
	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		mPref = new XSharedPreferences(Common.THIS_PACKAGE_NAME, Common.PREFERENCE_MAIN_FILE);
		mBlacklist = new XSharedPreferences(Common.THIS_PACKAGE_NAME, Common.PREFERENCE_BLACKLIST_FILE);
		mWhitelist = new XSharedPreferences(Common.THIS_PACKAGE_NAME, Common.PREFERENCE_WHITELIST_FILE);
		sModRes = XModuleResources.createInstance(startupParam.modulePath, null);
		
		//SystemUI
		NotificationShadeHook.zygote(sModRes);
		RecentAppsHook.initZygote(sModRes);
	}
	
	@Override
	public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
		// XHFW
		TestingSettingHook.handleLoadPackage(lpparam);
		
		// SystemUI Mods
		if (Build.VERSION.SDK_INT >= 20) { // Lollipop
			// Lollipop totally revamped SystemUI
			// TODO: move old SystemUI hooks to new package
			// TODO: forward port SystemUI hooks
		} else { // Kitkat and below
			NotificationShadeHook.hook(lpparam, mPref);
			RecentAppsHook.handleLoadPackage(lpparam, mPref);
		}
		StatusbarTaskbar.handleLoadPackage(lpparam, mPref);
		
		// SystemUI MultiWindow
		SystemUIOutliner.handleLoadPackage(lpparam);
		SystemUIMultiWindow.handleLoadPackage(lpparam);
		
		// Android
		try {
			SystemMods.handleLoadPackage(lpparam, mPref);
		} catch (Throwable e) {
			XposedBridge.log(Common.LOG_TAG + "(MainXposed // SystemMods)");
			XposedBridge.log(e);
		}
		try {
			XHFWService.initZygote(lpparam);
		} catch (Throwable e) {
			XposedBridge.log(Common.LOG_TAG + "(MainXposed // XHFWService)");
			XposedBridge.log(e);
		}
		
		// App
		hookMovableWindow = new MovableWindow(this, lpparam);
		hookHaloFloating = new HaloFloating(this, lpparam, mPref);
		hookActionBarColor = new ActionBarColorHook(this, lpparam, mPref);
	}

	public boolean isBlacklisted(String pkg) {
		mBlacklist.reload();
		return mBlacklist.contains(pkg);
	}
	
	public boolean isWhitelisted(String pkg) {
		mWhitelist.reload();
		return mWhitelist.contains(pkg);
	}
	
	public int getBlackWhiteListOption() {
		mPref.reload();
		return Integer.parseInt(mPref.getString(Common.KEY_WHITEBLACKLIST_OPTIONS, Common.DEFAULT_WHITEBLACKLIST_OPTIONS));
	}
}
