/*
 * /*package com.zst.xposed.halo.floatingwindow.hooks;
 *

import static de.robv.android.xposed.XposedHelpers.findClass;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import com.zst.xposed.halo.floatingwindow.Common;

import android.app.Activity;
import android.app.ActivityThread;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.view.Window;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.IXposedHookZygoteInit.StartupParam;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.callbacks.XCallback;


public class HaloFlagInject {
	public static final int FLAG_FLOATING_WINDOW = 0x00002000;
	public static XSharedPreferences pref;
	    static boolean newTask;
	    static boolean floatingWindow;
		 static String previousPkg = Common.NULL;
		 
	public void initZygote(StartupParam startupParam) throws Throwable {
		 pref = new XSharedPreferences(Common.MY_PACKAGE_NAME,Common.MY_PACKAGE_NAME);
	}
		 
	public void handleLoadPackage(LoadPackageParam l, XSharedPreferences pref) throws Throwable {
		pref.reload();
		inject_ActivityRecord_ActivityRecord(l);
		try {
			inject_ActivityStack(l, pref);
		} catch (Throwable e) {
			XposedBridge.log("XHaloFloatingWindow-ERROR(ActivityStack): " + e.toString());
		}
		inject_WindowManagerService_setAppStartingWindow(l);
		inject_Activity(pref);
		inject_DecorView_generateLayout(l);
		inject_ActivityThread();
		if (l.packageName.equals(NotificationShadeHook.SYSTEM_UI)){
		 if (pref.getBoolean(Common.KEY_LONGPRESS_INJECT, Common.DEFAULT_LONGPRESS_INJECT))NotificationShadeHook.inject_BaseStatusBar_LongPress(l); 
		}
	}
	public static void inject_ActivityRecord_ActivityRecord(final LoadPackageParam lpparam) {
		try {
			if (!lpparam.packageName.equals("android")) return;
			

			XposedBridge.hookAllConstructors(findClass("com.android.server.am.ActivityRecord", lpparam.classLoader),
					new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {
				
			   @Override
			   protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				   isHoloFloat= false;
				   floatingWindow = false;
				   Intent i = null;
				   Object stack = null;
					 ActivityInfo aInfo = null;
					 if (Build.VERSION.SDK_INT <= 17){ //JB 4.2 and below
					 i  = (Intent) param.args[4];
					 aInfo  = (ActivityInfo) param.args[6];
					 stack = param.args[1];
					 }else if (Build.VERSION.SDK_INT == 18) { // JB 4.3 has additional _launchedFromPackage. so indexs are affected
					 i  = (Intent) param.args[5];
					 aInfo  = (ActivityInfo) param.args[7];
					 stack = param.args[1];
					 }else if (Build.VERSION.SDK_INT == 19) { // Fuck Google. Changed params order again for KitKat.
					 i  = (Intent) param.args[4];
					 aInfo  = (ActivityInfo) param.args[6];
					 	try{
					 		Object stackSupervisor = param.args[12]; //mStackSupervisor
					 		stack = XposedHelpers.callMethod(stackSupervisor, "getFocusedStack");
					 	}catch(Exception e){
					 		Field field = param.args[12].getClass().getDeclaredField("mFocusedStack");
							 field.setAccessible(true);
							 stack = field.get(param.args[12]);
					 	}
					 //TODO check if working
					 }
					 if (i == null) return;
					 
				// This is where the package gets its first context from the attribute-cache
		            // In order to hook its attributes we set up our check for floating mutil windows here.

		            floatingWindow = (i.getFlags() & FLAG_FLOATING_WINDOW) == FLAG_FLOATING_WINDOW;
			        
		            Class activitystack = stack.getClass();
		            Field mHistoryField = null;
		            if (Build.VERSION.SDK_INT == 19) { //Kitkat
		            	mHistoryField = activitystack.getDeclaredField("mTaskHistory"); // ArrayList<TaskRecord>
		            }else { // JB4.3 and lower
		            	mHistoryField = activitystack.getDeclaredField("mHistory"); // ArrayList<ActivityRecord>
		            } 
		             
		            mHistoryField.setAccessible(true);
		            ArrayList<?> alist = (ArrayList<?>)mHistoryField.get(stack);
		            
		            boolean isFloating;
		            boolean taskAffinity;
		            if (alist.size() > 0){
			            if (Build.VERSION.SDK_INT == 19) {
			            	Object taskRecord =  alist.get(alist.size() - 1);
			            	Field taskRecord_intent_field = taskRecord.getClass().getDeclaredField("intent");
			            	taskRecord_intent_field.setAccessible(true);
			            	Intent  taskRecord_intent = (Intent) taskRecord_intent_field.get(taskRecord);
			                isFloating = (taskRecord_intent.getFlags() & FLAG_FLOATING_WINDOW) == FLAG_FLOATING_WINDOW;
			            	String pkgName = taskRecord_intent.getPackage();
			            	taskAffinity = aInfo.applicationInfo.packageName.equals(pkgName /*info.packageName*d/);
			            }else{
		            	//XposedBridge.log("more than 0 -----" + aInfo.applicationInfo.className);
		            	Object baseRecord =  alist.get(alist.size() -1); //ActivityRecord
		            	Field baseRecordField = baseRecord.getClass().getDeclaredField("intent");
		            	baseRecordField.setAccessible(true);
		            	Intent  baseRecord_intent = (Intent) baseRecordField.get(baseRecord);
		            	isFloating = (baseRecord_intent.getFlags() & FLAG_FLOATING_WINDOW) == FLAG_FLOATING_WINDOW;
		                Field baseRecordField_2 = baseRecord.getClass().getDeclaredField("packageName");
		            	baseRecordField_2.setAccessible(true);
		            	String  baseRecord_pkg = (String) baseRecordField_2.get(baseRecord);
		                taskAffinity = aInfo.applicationInfo.packageName.equals(baseRecord_pkg /*baseRecord.packageName*d/);
			            }
		                newTask = false;
		                // If the current intent is not a new task we will check its top parent.
		                // Perhaps it started out as a multiwindow in which case we pass the flag on
		                if (isFloating && taskAffinity) {
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
					   if (floatingWindow){ 
					        i.addFlags(Common.FLAG_FLOATING_WINDOW);
					         i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
					        tt.set(param.thisObject, Boolean.FALSE);
					   }
		            
	            	previousPkg = aInfo.applicationInfo.packageName;

		          }
			 });
			
			
		
		} catch (Throwable e) {
			XposedBridge.log("XHaloFloatingWindow-ERROR(ActivityRecord): " + e.toString());
		}
	}
	static Object previous = null;
	public static void inject_ActivityStack(final LoadPackageParam lpparam, final XSharedPreferences pref) throws Throwable{
		Class<?> hookClass = findClass("com.android.server.am.ActivityStack", lpparam.classLoader);
			XposedBridge.hookAllMethods(hookClass, "resumeTopActivityLocked", new XC_MethodHook() { 
				
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if (!floatingWindow) return;
		    		boolean b = pref.getBoolean(Common.KEY_APP_PAUSE, Common.DEFAULT_APP_PAUSE);
		    		if (!b) return;
					if (param.args.length == 2){
						Class<?> clazz = param.thisObject.getClass();
						Field field = clazz.getDeclaredField(("mResumedActivity"));
						field.setAccessible(true);
						previous = null;
						previous = field.get(param.thisObject);
						field.set(param.thisObject, null);
					}	
				}
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					if (!floatingWindow) return;
		    		boolean b = pref.getBoolean(Common.KEY_APP_PAUSE, Common.DEFAULT_APP_PAUSE);
		    		if (!b) return;
					Class<?> clazz = param.thisObject.getClass();
					Field field = clazz.getDeclaredField(("mResumedActivity"));
					field.setAccessible(true);
					if (previous != null)field.set(param.thisObject, previous);
				}
				
		});
			//FIXME Kitkat breaks this
			XposedBridge.hookAllMethods(hookClass, "moveHomeToFrontFromLaunchLocked", new XC_MethodReplacement() { 
				@Override
				protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
					int launchFlags = (Integer)param.args[0]; 
					if ((launchFlags &
			                (Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_TASK_ON_HOME))
			                == (Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_TASK_ON_HOME)) {
						boolean floating = (launchFlags&Common.FLAG_FLOATING_WINDOW) == Common.FLAG_FLOATING_WINDOW;
			            if (!floating) {
			            	try{ 
			    	    		Method showsb = param.thisObject.getClass().getMethod("moveHomeToFrontLocked");
			    	    		showsb.invoke( param.thisObject );
			    	    	}catch(Throwable e2){ }
			            }
					}
					return null;
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
					param.args[param.args.length-1] = Boolean.FALSE;
					}
				
				});
		} catch (Throwable e) {
			XposedBridge.log("XHaloFloatingWindow-ERROR(setAppStartingWindow):" + e.toString());
		}
	}	
	static boolean isHoloFloat = false;
	public static void inject_Activity(XSharedPreferences pref) { 
		boolean isMovable = pref.getBoolean(Common.KEY_MOVABLE_WINDOW, Common.DEFAULT_MOVABLE_WINDOW);
		try{	
			XposedBridge.hookAllMethods( Activity.class, isMovable?"onStart":"onResume", new XC_MethodHook() { 
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable { 
					 Activity thiz = (Activity)param.thisObject;
					 String name = thiz.getWindow().getContext().getPackageName();
					 Intent intent = thiz.getIntent();
					 if (name.startsWith("com.android.systemui")){
						 //How did halo flag get into SystemUI? Remove it.
						 intent.setFlags(intent.getFlags() & ~Common.FLAG_FLOATING_WINDOW);
						 isHoloFloat = false;
						 return; 
					 }
					  isHoloFloat = (intent.getFlags() & FLAG_FLOATING_WINDOW) == FLAG_FLOATING_WINDOW;
					 if(isHoloFloat) {
					     LayoutScaling.appleFloating(thiz.getWindow().getContext(),thiz.getWindow());
						 return;
					 }
				}
			});
		
			
		} catch (Throwable e) {
			XposedBridge.log("XHaloFloatingWindow-ERROR(onCreate):" + e.toString());
		}
		try{	
			XposedBridge.hookAllMethods( Activity.class, "performStop", new XC_MethodHook() { 
				protected void afterHookedMethod(MethodHookParam param) throws Throwable { 
					// Floatingwindows activities should be kept volatile to prevent new activities taking
			        // up front in a minimized space. Every stop call, for instance when pressing home,
			        // will terminate the activity. If the activity is already finishing we might just
			        // as well let it go.
					 Activity thiz = (Activity)param.thisObject;
			        if (!thiz.isChangingConfigurations() && thiz.getWindow() != null && isHoloFloat && !thiz.isFinishing())
			            thiz.finishAffinity();
				}
			});
		} catch (Throwable e) {
			XposedBridge.log("XHaloFloatingWindow-ERROR(performStop):" + e.toString());
		}
		
	}
	public static void inject_DecorView_generateLayout(final LoadPackageParam lpparam) {
		try {
			Class<?> hookClass = findClass("com.android.internal.policy.impl.PhoneWindow", lpparam.classLoader);
			XposedBridge.hookAllMethods(hookClass, "generateLayout",  new XC_MethodHook() { 
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					Window window = (Window) param.thisObject;
					Context context = window.getContext();
					String name = window.getContext().getPackageName();
					if (name.startsWith("com.android.systemui"))  return; 
					 
					 if (!(isHoloFloat && floatingWindow)) return; 
					 if (window.getDecorView().getTag(10000) != null) return;
					 //Return so it doesnt override our custom movable window scaling
					 
						LayoutScaling.appleFloating(context, window);
						window.getDecorView().setTag(10000, (Object)1);
					
				}
			});
		} catch (Throwable e) { XposedBridge.log("XHaloFloatingWindow-ERROR(DecorView): " + e.toString());
		}
	}
	
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
				}); /* Fix BoatBrowser etc. app FC onResume *k/
		XposedBridge.hookAllMethods(android.app.Instrumentation.class, "onException",
				new XC_MethodReplacement() {
					protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
						return mExceptionHook;
					}
				});
		
	}
	
}



*/
 
