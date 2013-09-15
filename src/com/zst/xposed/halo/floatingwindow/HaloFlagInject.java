package com.zst.xposed.halo.floatingwindow;

import static de.robv.android.xposed.XposedHelpers.findClass;

import java.lang.reflect.Field;
import java.util.ArrayList;

import android.app.Activity;
import android.app.ActivityThread;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.view.Window;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.callbacks.XCallback;


public class HaloFlagInject implements  IXposedHookLoadPackage{
	public static final int FLAG_FLOATING_WINDOW = 0x00002000;
	public static XSharedPreferences pref;
	    static boolean newTask;
	    static boolean floatingWindow;
		 static String previousPkg = Res.NULL;
	@Override
	public void handleLoadPackage(LoadPackageParam l) throws Throwable {
		inject_ActivityRecord_ActivityRecord(l);
		inject_ActivityStack(l);
		inject_WindowManagerService_setAppStartingWindow(l);
		inject_Activity();
		inject_DecorView_generateLayout(l);
		inject_ActivityThread();
		if (l.packageName.equals(NotificationShadeHook.SYSTEM_UI)){
		NotificationShadeHook.inject_BaseStatusBar_LongPress(l); 
		}
	}
	public static void inject_ActivityRecord_ActivityRecord(final LoadPackageParam lpparam) {
		try {
			if (!lpparam.packageName.equals("android")) return;
			

			XposedBridge.hookAllConstructors(findClass("com.android.server.am.ActivityRecord", lpparam.classLoader),
					new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {
				
			   @Override
			   protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				   floatingWindow = false;
				   Intent i;
					 ActivityInfo aInfo;
					 if (param.args[4] instanceof Intent){
					 i  = (Intent) param.args[4];
					 aInfo  = (ActivityInfo) param.args[6];
					 }else{// Android 4.3 has additional _launchedFromPackage
					 i  = (Intent) param.args[5];
					 aInfo  = (ActivityInfo) param.args[7];
					 
					 }
					 if (i == null) return;
					 
				// This is where the package gets its first context from the attribute-cache
		            // In order to hook its attributes we set up our check for floating mutil windows here.

		            floatingWindow = (i.getFlags() & FLAG_FLOATING_WINDOW) == FLAG_FLOATING_WINDOW;
	                boolean newHome = (i.getFlags() & Intent.FLAG_ACTIVITY_TASK_ON_HOME) == Intent.FLAG_ACTIVITY_TASK_ON_HOME;		            
		            int flagger1 = i.getFlags();
		            flagger1 &= ~Intent.FLAG_ACTIVITY_TASK_ON_HOME;
			        i.setFlags( flagger1 );
			        if (floatingWindow){
			        i.addFlags(Res.FLAG_FLOATING_WINDOW);
			         i.setFlags(i.getFlags() & ~Intent.FLAG_ACTIVITY_SINGLE_TOP);
			         i.setFlags(i.getFlags() & ~Intent.FLAG_ACTIVITY_CLEAR_TOP);
			         i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS); 
			         i.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
			         i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			         i.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
			         i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
			         i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			        }
		            Object stack = param.args[1];
		            Class activitystack = stack.getClass();
		            Field mHistoryField = activitystack.getDeclaredField("mHistory");
		            mHistoryField.setAccessible(true);
		            ArrayList<?> alist = (ArrayList<?>)mHistoryField.get(stack);
		            
		            if (alist.size() > 0){
		            	//XposedBridge.log("more than 0 -----" + aInfo.applicationInfo.className);
		            	Object baseRecord =  alist.get(alist.size() -1); //ActivityRecord
		            	Field baseRecordField = baseRecord.getClass().getDeclaredField("intent");
		            	baseRecordField.setAccessible(true);
		            	Intent  baseRecord_intent = (Intent) baseRecordField.get(baseRecord);
		                final boolean floats = (baseRecord_intent.getFlags() & FLAG_FLOATING_WINDOW) == FLAG_FLOATING_WINDOW;
		                Field baseRecordField_2 = baseRecord.getClass().getDeclaredField("packageName");
		            	baseRecordField_2.setAccessible(true);
		            	String  baseRecord_pkg = (String) baseRecordField_2.get(baseRecord);
		                final boolean taskAffinity = aInfo.applicationInfo.packageName.equals(baseRecord_pkg /*baseRecord.packageName*/);
		                newTask = false;
		                // If the current intent is not a new task we will check its top parent.
		                // Perhaps it started out as a multiwindow in which case we pass the flag on
		                if (floats && (!newTask || taskAffinity)) {
				            Field intentField = param.thisObject.getClass().getDeclaredField("intent");
				            intentField.setAccessible(true);
				            Intent newer = (Intent)intentField.get(param.thisObject);
				            newer.addFlags(FLAG_FLOATING_WINDOW);
				            intentField.set(param.thisObject, newer);
				            Field fullS = param.thisObject.getClass().getDeclaredField("fullscreen");
							   fullS.setAccessible(true);
							   fullS.set(param.thisObject, Boolean.FALSE);
		                    floatingWindow = true;
		                    
		                } 
		                }
		            Field tt = param.thisObject.getClass().getDeclaredField("fullscreen");
					   tt.setAccessible(true);
					   if(floatingWindow) tt.set(param.thisObject, Boolean.FALSE);
	                if (newHome && !tt.getBoolean(param.thisObject)) floatingWindow = true; 
		            
	            	previousPkg = aInfo.applicationInfo.packageName;

		          }
			 });
			
			
		
		} catch (Exception e) {
			XposedBridge.log("XHaloFloatingWindow-ERROR(ActivityRecord): " + e.toString());
		}
	}
	static Object previous = null;
	public static void inject_ActivityStack(final LoadPackageParam lpparam) {
		Class<?> hookClass = findClass("com.android.server.am.ActivityStack", lpparam.classLoader);
			XposedBridge.hookAllMethods(hookClass, "resumeTopActivityLocked", new XC_MethodHook() { 
				
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
		    	    pref = new XSharedPreferences(Res.MY_PACKAGE_NAME,Res.MY_PACKAGE_NAME);
		    		boolean b = pref.getBoolean(Res.KEY_APP_PAUSE, Res.DEFAULT_APP_PAUSE);
		    		if (!b) return;
					if (!floatingWindow) return;
					if (param.args.length == 2){
						Object prevt = param.args[0];
						Class<?> clazz = param.thisObject.getClass();
						Field field = clazz.getDeclaredField(("mResumedActivity"));
						field.setAccessible(true);
						previous = null;
						previous = field.get(param.thisObject);
						field.set(param.thisObject, null);
					}	
				}
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					pref = new XSharedPreferences(Res.MY_PACKAGE_NAME,Res.MY_PACKAGE_NAME);
		    		boolean b = pref.getBoolean(Res.KEY_APP_PAUSE, Res.DEFAULT_APP_PAUSE);
		    		if (!b) return;
					if (!floatingWindow) return;
					Class<?> clazz = param.thisObject.getClass();
					Field field = clazz.getDeclaredField(("mResumedActivity"));
					field.setAccessible(true);
					if (previous != null)field.set(param.thisObject, previous);
				}
				
		});
	}	
	public static void inject_WindowManagerService_setAppStartingWindow(final LoadPackageParam lpparam) {
		try {
				Class<?> hookClass = findClass("com.android.server.wm.WindowManagerService", lpparam.classLoader);
				XposedBridge.hookAllMethods(hookClass, "setAppStartingWindow", new XC_MethodHook(){
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if (!floatingWindow) return;
					String pkg = (String)param.args[1];
					if(pkg.equals("android"))return; 
					
					// Change boolean "createIfNeeded" to FALSE
					param.args[9] = Boolean.FALSE;
					}
				
				});
		} catch (Throwable e) {
			XposedBridge.log("XHaloFloatingWindow-ERROR(setAppStartingWindow):" + e.toString());
		}
	}	
	static boolean isHoloFloat = false;
	public static void inject_Activity( ) { 
		try{	
			XposedBridge.hookAllMethods( Activity.class,  "onCreate", new XC_MethodHook() { 
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable { 
					 Activity thiz = (Activity)param.thisObject;
					 String name = thiz.getWindow().getContext().getPackageName();
					 if (name.startsWith("com.android.systemui"))  return; 
					  isHoloFloat = (thiz.getIntent().getFlags() & FLAG_FLOATING_WINDOW) == FLAG_FLOATING_WINDOW;
					
					 if(isHoloFloat){ 
						 
					     LayoutScaling.applyThemeLess(thiz.getWindow().getContext(),thiz.getWindow());
						 return;
					 } 
					 
				}
				protected void afterHookedMethod(MethodHookParam param) throws Throwable { 
					 Activity thiz = (Activity)param.thisObject;
					 if(isHoloFloat){ 
						 
					     LayoutScaling.applyThemeLess(thiz.getWindow().getContext(),thiz.getWindow());
						 return;
					 } 
					 
				}
				
			});
		
			
		} catch (Throwable e) {
			XposedBridge.log("XHaloFloatingWindow-ERROR(onCreate):" + e.toString());
		}
		
	}
	public static void inject_DecorView_generateLayout(final LoadPackageParam lpparam) {
		try {
			Class<?> hookClass = findClass("com.android.internal.policy.impl.PhoneWindow", lpparam.classLoader);
			XposedBridge.hookAllMethods(hookClass, "generateLayout",  new XC_MethodHook() { 
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					Window window = (Window) param.thisObject;
					Context context = window.getContext();
					String AppPackage = context.getPackageName();
 
					 if (!isHoloFloat) return; 
					 
					 
						String localClassPackageName = context.getClass().getPackage().getName();

						LayoutScaling.applyTheme(context, window,localClassPackageName);
					
				}
			});
		} catch (Exception e) { XposedBridge.log("XHaloFloatingWindow-ERROR(DecorView): " + e.toString());
		}
	}
	
	static boolean ExceptionHook = false;
	public static void inject_ActivityThread() {
		try {
				XposedBridge.hookAllMethods(ActivityThread.class, "performResumeActivity", new XC_MethodHook(){
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable { 
					ExceptionHook = true;
					}
				protected void afterHookedMethod(MethodHookParam param) throws Throwable { 
					ExceptionHook = false;
					}
				}); /* Fix BoatBrowser etc. app FC onResume */
				XposedBridge.hookAllMethods(android.app.Instrumentation.class, "onException", new XC_MethodReplacement(){
					protected Object replaceHookedMethod(MethodHookParam param) throws Throwable { 
						return ExceptionHook;
						}
				}); 
		} catch (Throwable e) {
			XposedBridge.log("XHaloFloatingWindow-ERROR(setAppStartingWindow):" + e.toString());
		}
	}	
}




 
