package com.zst.xposed.halo.floatingwindow.hooks;

import static de.robv.android.xposed.XposedHelpers.findClass;

import java.lang.reflect.Field;
import java.util.ArrayList;

import android.app.Activity;
import android.app.ActivityThread;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.view.Window;

import com.zst.xposed.halo.floatingwindow.Common;
import com.zst.xposed.halo.floatingwindow.MainXposed;
import com.zst.xposed.halo.floatingwindow.helpers.LayoutScaling;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XCallback;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class HaloFloating {
	
	static XSharedPreferences mPref;
	static boolean isHoloFloat = false;
	static boolean floatingWindow;
	
	public static void handleLoadPackage(LoadPackageParam l, XSharedPreferences pref) {
		mPref = pref;
		mPref.reload();
		initHooks(l);
	}
	
	// We catch all the error to prevent the system from crashing or bootlooping
	// if one of the hooks fail.
	private static void initHooks(LoadPackageParam l) {
		/*********************************************/
		try {
			inject_ActivityRecord_ActivityRecord(l);
		} catch (Throwable e) {
			XposedBridge.log(Common.LOG_TAG + "(ActivityRecord)");
			XposedBridge.log(e);
		}
		/*********************************************/
		try {
			injectActivityStack(l);
		} catch (Throwable e) {
			XposedBridge.log(Common.LOG_TAG + "(ActivityStack)");
			XposedBridge.log(e);
		}
		/*********************************************/
		try {
			removeAppStartingWindow(l);
		} catch (Throwable e) {
			XposedBridge.log(Common.LOG_TAG + "(removeAppStartingWindow)");
			XposedBridge.log(e);
		}
		/*********************************************/
		try {
			inject_Activity();
		} catch (Throwable e) {
			XposedBridge.log(Common.LOG_TAG + "(inject_Activity)");
			XposedBridge.log(e);
		}
		/*********************************************/
		try {
			injectPerformStop();
		} catch (Throwable e) {
			XposedBridge.log(Common.LOG_TAG + "(injectPerformStop)");
			XposedBridge.log(e);
		}
		/*********************************************/
		try {
			injectGenerateLayout(l);
		} catch (Throwable e) {
			XposedBridge.log(Common.LOG_TAG + "(injectGenerateLayout)");
			XposedBridge.log(e);
		}
		/*********************************************/
		try {
			fixExceptionWhenResuming();
		} catch (Throwable e) {
			XposedBridge.log(Common.LOG_TAG + "(fixExceptionWhenResuming)");
			XposedBridge.log(e);
		}
		/*********************************************/
		
	}
	
	
	/* For passing on flag to next activity*/
	private static void inject_ActivityRecord_ActivityRecord(final LoadPackageParam lpparam)
			throws Throwable {
		if (!lpparam.packageName.equals("android")) return;
		
		XposedBridge.hookAllConstructors(findClass("com.android.server.am.ActivityRecord",
				lpparam.classLoader), new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				mPref.reload();
				
				isHoloFloat = false;
				floatingWindow = false;
				Intent i = null;
				Object stack = null;
				ActivityInfo aInfo = null;
				
				if (Build.VERSION.SDK_INT <= 17) { // JB 4.2 and below
					i = (Intent) param.args[4];
					aInfo = (ActivityInfo) param.args[6];
					stack = param.args[1];
				} else if (Build.VERSION.SDK_INT == 18) { 
					// JB 4.3 has additional _launchedFromPackage. so indexs are affected
					i = (Intent) param.args[5];
					aInfo = (ActivityInfo) param.args[7];
					stack = param.args[1];
				} else if (Build.VERSION.SDK_INT == 19) { 
					// Fuck Google. Changed params order again for KitKat.
					i = (Intent) param.args[4];
					aInfo = (ActivityInfo) param.args[6];
					try {
						Object stackSupervisor = param.args[12]; // mStackSupervisor
						stack = XposedHelpers.callMethod(stackSupervisor, "getFocusedStack");
					} catch (Exception e) {
						Field field = param.args[12].getClass().getDeclaredField("mFocusedStack");
						field.setAccessible(true);
						stack = field.get(param.args[12]);
					}
				}
				if (i == null) return;
				// This is where the package gets its first context from the attribute-cache. In
				// order to hook its attributes we set up our check for floating mutil windows here.
				boolean isBlacklisted = MainXposed.isBlacklisted(aInfo.applicationInfo.packageName);
				boolean isWhitelisted = MainXposed.isWhitelisted(aInfo.applicationInfo.packageName);
				int blackWhitelistOptions = MainXposed.getBlackWhiteListOption();

				if (blackWhitelistOptions == 1) {
					/* Always open apps in halo except blacklisted apps */
					if (!isBlacklisted) {
						isWhitelisted = true;
						// if app is NOT blacklisted, whitelist the app
					}
				} else if (blackWhitelistOptions == 2) {
					/* Never open apps in halo but force whitelisted apps in halo */
					isBlacklisted = !isWhitelisted;
					// if not whitelisted, then blacklist app
				} else if (blackWhitelistOptions == 3) {
					/* Blacklist all apps & only allow whitelisted apps to be opened in halo */
					isBlacklisted = !isWhitelisted;
					// if not in whitelist, then blacklist app
					isWhitelisted = false;
					// turn whilelist off since we are not forcing app in halo
				}
				
				if (!isBlacklisted && isWhitelisted) {
					i.addFlags(Common.FLAG_FLOATING_WINDOW);
				}
				floatingWindow = (i.getFlags() & Common.FLAG_FLOATING_WINDOW) == Common.FLAG_FLOATING_WINDOW;
				
				Class<?> activitystack = stack.getClass();
				Field mHistoryField = null;
				if (Build.VERSION.SDK_INT == 19) { // Kitkat
					mHistoryField = activitystack.getDeclaredField("mTaskHistory"); // ArrayList<TaskRecord>
				} else { // JB4.3 and lower
					mHistoryField = activitystack.getDeclaredField("mHistory"); // ArrayList<ActivityRecord>
				}
				mHistoryField.setAccessible(true);
				ArrayList<?> alist = (ArrayList<?>) mHistoryField.get(stack);
						
				boolean isFloating;
				boolean taskAffinity;
				if (alist.size() > 0 && !floatingWindow && !isBlacklisted) {
					if (Build.VERSION.SDK_INT == 19) {
						Object taskRecord = alist.get(alist.size() - 1);
						Field taskRecord_intent_field = taskRecord.getClass().getDeclaredField("intent");
						taskRecord_intent_field.setAccessible(true);
						Intent taskRecord_intent = (Intent) taskRecord_intent_field.get(taskRecord);
						isFloating = (taskRecord_intent.getFlags() & Common.FLAG_FLOATING_WINDOW) == Common.FLAG_FLOATING_WINDOW;
						String pkgName = taskRecord_intent.getPackage();
						taskAffinity = aInfo.applicationInfo.packageName.equals(pkgName /* info.packageName */);
					} else {
						Object baseRecord = alist.get(alist.size() - 1); // ActivityRecord
						Field baseRecordField = baseRecord.getClass().getDeclaredField("intent");
						baseRecordField.setAccessible(true);
						Intent baseRecord_intent = (Intent) baseRecordField.get(baseRecord);
						isFloating = (baseRecord_intent.getFlags() & Common.FLAG_FLOATING_WINDOW) == Common.FLAG_FLOATING_WINDOW;
						Field baseRecordField_2 = baseRecord.getClass().getDeclaredField("packageName");
						baseRecordField_2.setAccessible(true);
						String baseRecord_pkg = (String) baseRecordField_2.get(baseRecord);
						taskAffinity = aInfo.applicationInfo.packageName.equals(baseRecord_pkg );
						/*baseRecord.packageName*/
					}
					// If the current intent is not a new task we will check its top parent.
					// Perhaps it started out as a multiwindow in which case we pass the flag on
					boolean forceTaskHalo = mPref.getBoolean(Common.KEY_FORCE_OPEN_APP_ABOVE_HALO, Common.DEFAULT_FORCE_OPEN_APP_ABOVE_HALO);
					if (isFloating && (forceTaskHalo || taskAffinity)) {
						Field intentField = param.thisObject.getClass().getDeclaredField("intent");
						intentField.setAccessible(true);
						Intent newer = (Intent) intentField.get(param.thisObject);
						newer.addFlags(Common.FLAG_FLOATING_WINDOW);
						intentField.set(param.thisObject, newer);
						floatingWindow = true;
					}
				}
				if (isBlacklisted) {
					floatingWindow = false;
					int intent_flag = i.getFlags();
					intent_flag &= ~Common.FLAG_FLOATING_WINDOW;
					i.setFlags(intent_flag);
				}
				if (floatingWindow) {
					int intent_flag = i.getFlags();
					intent_flag &= ~Intent.FLAG_ACTIVITY_TASK_ON_HOME;
					i.setFlags(intent_flag);
					i.addFlags(Common.FLAG_FLOATING_WINDOW);
					if (!mPref.getBoolean(Common.KEY_SHOW_APP_IN_RECENTS, Common.DEFAULT_SHOW_APP_IN_RECENTS)) {
						i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
					} else if (mPref.getBoolean(Common.KEY_FORCE_APP_IN_RECENTS, Common.DEFAULT_FORCE_APP_IN_RECENTS)) {
						int intent_flag2 = i.getFlags();
						intent_flag2 &= ~Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS;
						i.setFlags(intent_flag2);
					}
					i.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
					Field tt = param.thisObject.getClass().getDeclaredField("fullscreen");
					tt.setAccessible(true);
					tt.set(param.thisObject, Boolean.FALSE);
				}
			}
		});
	}
	
	
	/* 
	 * It changes the "mResumedActivity" object to null.
	 * There is a check in "resumeTopActivityLocked" that if "mResumedActivity"
	 * is not null, then pause the app. We are working around it like this.
	 */
	static Field activityField;
	static Object previous = null;
	static boolean appPauseEnabled;
	private static void injectActivityStack(final LoadPackageParam lpp) throws Throwable {
		final Class<?> hookClass = findClass("com.android.server.am.ActivityStack", lpp.classLoader);
		XposedBridge.hookAllMethods(hookClass, "resumeTopActivityLocked", new XC_MethodHook() {
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if (!floatingWindow) return;
				
				mPref.reload();
				appPauseEnabled = mPref.getBoolean(Common.KEY_APP_PAUSE, Common.DEFAULT_APP_PAUSE);
				if (appPauseEnabled) return;
				
				Class<?> clazz = param.thisObject.getClass();
				activityField = clazz.getDeclaredField(("mResumedActivity"));
				activityField.setAccessible(true);
				previous = null;
				final Object prevActivity = activityField.get(param.thisObject);
				if (prevActivity != null) {
					previous = prevActivity;
				}
				activityField.set(param.thisObject, null);
			}
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (!floatingWindow) return;
				if (appPauseEnabled) return;
				if (previous != null) {
					Class<?> clazz = param.thisObject.getClass();
					if (activityField == null) activityField = clazz.getDeclaredField(("mResumedActivity"));
					activityField.setAccessible(true);
					activityField.set(param.thisObject, previous);
				}
			}
		});
		
		/* This is a Kitkat work-around to make sure the background is transparent */
		XposedBridge.hookAllMethods(hookClass, "startActivityLocked", new XC_MethodHook() {
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if (!floatingWindow) return;
				if (param.args[1] instanceof Intent) return;
				Object activityRecord = param.args[0];
				Class<?> activityRecordClass = activityRecord.getClass();
				Field activityField = activityRecordClass.getDeclaredField(("fullscreen"));
				activityField.setAccessible(true);
				activityField.set(activityRecord, Boolean.FALSE);
			}
		});

		/*
		 * Prevents the App from bringing the home to the front. // FIXME Kitkat breaks this
		 */
		XposedBridge.hookAllMethods(hookClass, "moveHomeToFrontFromLaunchLocked", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				int launchFlags = (Integer) param.args[0];
				if ((launchFlags & (Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME))
						== (Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME)) {
					boolean floating = (launchFlags & Common.FLAG_FLOATING_WINDOW) == Common.FLAG_FLOATING_WINDOW;
					if (floating) param.setResult(null);
					// if the app is a floating app, and is a new task on home.
					// then skip this method.
				} else {
					param.setResult(null);
					// This is not a new task on home. Dont allow the method to continue.
					// Since there is no point to run method which checks for the same thing
				}
			}
		});
	}
	
	/*
	 * Removes the app starting placeholder screen before the app contents is shown.
	 * Does this by making 'createIfNeeded' to false
	 */
	private static void removeAppStartingWindow(final LoadPackageParam lpp) throws Throwable {
		Class<?> hookClass = findClass("com.android.server.wm.WindowManagerService", lpp.classLoader);
		XposedBridge.hookAllMethods(hookClass, "setAppStartingWindow", new XC_MethodHook() {
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if (!floatingWindow) return;
				if ("android".equals((String) param.args[1])) return;
				// Change boolean "createIfNeeded" to FALSE
				if (param.args[param.args.length - 1] instanceof Boolean) {
					param.args[param.args.length - 1] = Boolean.FALSE;
					// Last param of the arguments
					// It's length has changed in almost all versions of Android.
					// Since it is always the last value, we use this to our advantage.
				}
			}
		});
	}
	
	/*
	 * If the window is not movable (normal halo window), we scale the window every onResume.
	 * onResume is called after every rotation so we do not need to bother with it. 
	 * 
	 * If the window is movable, then scale only on every onStart.
	 * 
	 * This is done onStart because Samsung's multiwindow codes run between onCreate & onStart
	 * and the codes undo my layout scaling.
	 */
	private static void inject_Activity() throws Throwable {
		final boolean isMovable = mPref.getBoolean(Common.KEY_MOVABLE_WINDOW, Common.DEFAULT_MOVABLE_WINDOW);
		final String class_name = isMovable ? "onStart" : "onResume";
		XposedBridge.hookAllMethods(Activity.class, class_name, new XC_MethodHook() {
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				Activity thiz = (Activity) param.thisObject;
				Intent intent = thiz.getIntent();
				isHoloFloat = (intent.getFlags() & Common.FLAG_FLOATING_WINDOW) == Common.FLAG_FLOATING_WINDOW;
				if (isHoloFloat) {
					LayoutScaling.appleFloating(mPref, thiz.getWindow());
				}
			}
		});
		
	}
	
	/*
	 * This is the default Halo window behavior by Paranoid Android to close windows
	 * after the screen is turned off. These are their comments from the sources:
	 * 
	 *  	Floating Window activities should be kept volatile to prevent
	 *  	new activities taking up front in a minimized space. Every
	 *  	stop call, for instance when pressing home, will terminate
	 *  	the activity. If the activity is already finishing we might
	 *  	just as well let it go.
	 *  
	 *  I added the option to allow the user to disable it.
	 */
	private static void injectPerformStop() throws Throwable {
		XposedBridge.hookAllMethods(Activity.class, "performStop", new XC_MethodHook() {
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {		
				Activity thiz = (Activity) param.thisObject;
				mPref.reload();
				if (mPref.getBoolean(Common.KEY_DISABLE_AUTO_CLOSE, Common.DEFAULT_DISABLE_AUTO_CLOSE)) return;
				if (!thiz.isChangingConfigurations() && (thiz.getWindow() != null) && isHoloFloat
						&& !thiz.isFinishing()) {
					thiz.finishAffinity();
				}
				
			}
		});
	}
	
	private static void injectGenerateLayout(final LoadPackageParam lpp)
			throws Throwable {
		Class<?> cls = findClass("com.android.internal.policy.impl.PhoneWindow", lpp.classLoader);
		XposedBridge.hookAllMethods(cls, "generateLayout", new XC_MethodHook() {
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (!(isHoloFloat && floatingWindow)) return;
				Window window = (Window) param.thisObject;
				String name = window.getContext().getPackageName();
				if (name.startsWith("com.android.systemui")) return;
				
				LayoutScaling.appleFloating(mPref, window);
			}
		});
	}
	
	/*
	 * This is to fix "resuming" apps that have not been paused.
	 * Some apps (eg. BoatBrowser) will throw exceptions and we
	 * fix it using this hook.
	 * 
	 * According to the AOSP sources for Instrumentation.java:
	 * 
	 * 		To allow normal system exception process to occur, return false.
     *		If true is returned, the system will proceed as if the exception
     *		didn't happen.
     *
     * Therefore, to remove the exception, we return true if the resume activity
     * is in process and false when we are not resuming to let normal system behavior
     * continue as normal.
	 */
	static boolean mExceptionHook = false;
	private static void fixExceptionWhenResuming() throws Throwable {
		XposedBridge.hookAllMethods(ActivityThread.class, "performResumeActivity", 
				new XC_MethodHook() {
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				mExceptionHook = true;
			}
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				mExceptionHook = false;
			}
		});
		XposedBridge.hookAllMethods(android.app.Instrumentation.class, "onException",
				new XC_MethodReplacement() {
			protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
				return mExceptionHook;
			}
		});
	}
	
}
