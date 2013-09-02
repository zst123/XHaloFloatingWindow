package com.zst.xposed.halo.floatingwindow;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import java.lang.reflect.Field;
import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
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
	}
	public static void inject_ActivityRecord_ActivityRecord(final LoadPackageParam lpparam) {
		try {
			if (!lpparam.packageName.equals("android")) return;
			

			XposedBridge.hookAllConstructors(findClass("com.android.server.am.ActivityRecord", lpparam.classLoader),
					new XC_MethodHook(XCallback.PRIORITY_LOWEST) {
				
			   @Override
			   protected void afterHookedMethod(MethodHookParam param) throws Throwable {
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
		                   // XposedBridge.log(baseRecord_pkg + " ----- floating" + aInfo.applicationInfo.className);
		                    
		                }
		            }
	            	previousPkg = aInfo.applicationInfo.packageName;

		            // If this is a multiwindow activity we prevent it from messing up the history stack,
		            // like jumping back home, killing the current activity or polluting recents
		          }
			 });
			
			
		
		} catch (Exception e) {
			XposedBridge.log("XHaloFloatingWindow-ERROR(ActivityRecord): " + e.toString());
		}
	}
	
	
	
}


 
