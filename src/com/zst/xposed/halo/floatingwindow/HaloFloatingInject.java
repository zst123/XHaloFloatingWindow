package com.zst.xposed.halo.floatingwindow;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findConstructorBestMatch;
import static de.robv.android.xposed.XposedHelpers.findConstructorExact;
import static de.robv.android.xposed.XposedHelpers.findField;
import static de.robv.android.xposed.XposedHelpers.findMethodBestMatch;
import static de.robv.android.xposed.XposedHelpers.findMethodExact;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.IApplicationToken;
import android.view.IWindowManager;
import android.view.Window;
import android.view.WindowManager;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.callbacks.XCallback;


public class HaloFloatingInject implements  IXposedHookZygoteInit , IXposedHookLoadPackage{
	private static final String ID_TAG = "&ID=";
	public static final int FLAG_FLOATING_WINDOW = 0x00002000;
	
	private static String class_boolean = Res.NULL;
	public static XSharedPreferences pref;

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		inject_Activity();
		inject_DecorView_generateLayout();	
	}
	@Override
	public void handleLoadPackage(LoadPackageParam l) throws Throwable {
		inject_WindowManagerService_setAppStartingWindow(l);
		inject_ActivityRecord_ActivityRecord(l);
		inject_ActivityStack(l);
	}
	static String PKG_NAME = "XHaloFloatingWindow-PackageName-Null";

	public static void inject_ActivityRecord_ActivityRecord(final LoadPackageParam lpparam) {
		try {
			if (!lpparam.packageName.equals("android")) return;
			

			XposedBridge.hookAllConstructors(findClass("com.android.server.am.ActivityRecord", lpparam.classLoader),
					new XC_MethodHook(XCallback.PRIORITY_LOWEST) {
				 @Override  protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
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
						 
						 String pkg = aInfo.applicationInfo.packageName;
						 if (pkg.equals("android") || pkg.equals("com.android.systemui")) return;
						 if(aInfo.applicationInfo.uid != Res.previousUid){
							 
							 
						 if((i.getFlags()& FLAG_FLOATING_WINDOW)==0){
							 Res.notFloating = true;
							 return;
						 }
						 }
						 
						 Res.notFloating = false;
						 Res.previousUid = aInfo.applicationInfo.uid;
						 PKG_NAME = pkg;
			}
			   @Override
			   protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				   if (Res.notFloating)return;
				   Field fullS = param.thisObject.getClass().getDeclaredField("fullscreen");
				   fullS.setAccessible(true);
				   fullS.set(param.thisObject, Boolean.FALSE);
			   }
			 });
			
			
		
		} catch (Exception e) {
			XposedBridge.log("XHaloFloatingWindow-ERROR(ActivityRecord): " + e.toString());
		}
	}
	public static void inject_ActivityStack(final LoadPackageParam lpparam) {
		Class<?> hookClass = findClass("com.android.server.am.ActivityStack", lpparam.classLoader);
			XposedBridge.hookAllMethods(hookClass, "startActivityLocked", new XC_MethodHook() { 
				
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if (Res.notFloating) return;
					if (param.args[1] instanceof Intent)return;
					Object activityRecord = param.args[0];
					Class<?> activityRecordClass = activityRecord.getClass();
					if (!PKG_NAME.equals(activityRecordClass.getDeclaredField("packageName").get(activityRecord))) return;
					Field activityField = activityRecordClass.getDeclaredField(("fullscreen"));
					activityField.setAccessible(true);
					activityField.set(activityRecord, Boolean.FALSE);

					
				}
				});
		}
	
		
	public static void inject_WindowManagerService_setAppStartingWindow(final LoadPackageParam lpparam) {
		try {
				Class<?> hookClass = findClass("com.android.server.wm.WindowManagerService", lpparam.classLoader);
				XposedBridge.hookAllMethods(hookClass, "setAppStartingWindow", new XC_MethodHook(){
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if (Res.notFloating) return;
					// Gets String pkg to get package name
					String pkg = (String)param.args[1];
					if(pkg.equals("android"))return; 
					
					// Change boolean "createIfNeeded" to FALSE
					param.args[9] = Boolean.FALSE;
					// Removes Blank window placeholder before activity's layout xml fully loads
					
					//XposedBridge.log("XHaloFloatingWindow-DEBUG(setAppStartingWindow):" + pkg );
					

					}
				
				});
		} catch (Throwable e) {
			XposedBridge.log("XHaloFloatingWindow-ERROR(setAppStartingWindow):" + e.toString());
		}
}
	
	
	public static void inject_Activity( ) { 
		try{	
			findAndHookMethod( Activity.class,  "onCreate", Bundle.class, new XC_MethodHook() { 
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					
					 Activity thiz = (Activity)param.thisObject;
					 String name = thiz.getWindow().getContext().getPackageName();
					 if (name.startsWith("com.android.systemui"))  return; 
					 boolean isHoloFloat = (thiz.getIntent().getFlags() & FLAG_FLOATING_WINDOW) == FLAG_FLOATING_WINDOW;
					 if (Res.notFloating == false){
						 isHoloFloat = true;
					 }
					 if (class_boolean.equals( name + ID_TAG + thiz.getTaskId()))
					 {   		isHoloFloat = true;
					 }else {	class_boolean = Res.NULL;
					 }
					
					 if(isHoloFloat){
						 class_boolean = name + ID_TAG + thiz.getTaskId();
						// XposedBridge.log("XHaloFloatingWindow-DEBUG(onCreate):" + class_boolean);
						 thiz.getWindow().setCloseOnTouchOutsideIfNotSet(true);
						 thiz.getWindow().setGravity(Gravity.CENTER);
						 thiz.getWindow().setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND,WindowManager.LayoutParams.FLAG_DIM_BEHIND);
				            WindowManager.LayoutParams params = thiz.getWindow().getAttributes(); 
				    	    pref = new XSharedPreferences(Res.MY_PACKAGE_NAME,Res.MY_PACKAGE_NAME);
							Float alp = pref.getFloat(Res.KEY_ALPHA, Res.DEFAULT_ALPHA);
							Float dimm = pref.getFloat(Res.KEY_DIM, Res.DEFAULT_DIM);

				            params.alpha = alp;	
				            params.dimAmount = dimm;
				            thiz.getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
				            thiz.getTheme().applyStyle(R.style.Theme_Halo_FloatingWindow, true);
					     scaleFloatingWindow(thiz.getWindow().getContext(),thiz.getWindow());
						 return;
					 }
					 class_boolean = Res.NULL;
					 
				}
				
			});
		
			
		} catch (Throwable e) {
			XposedBridge.log("XHaloFloatingWindow-ERROR(onCreate):" + e.toString());
		}
		
}
	
	
	public static void inject_DecorView_generateLayout() {
		try {
			findAndHookMethod("com.android.internal.policy.impl.PhoneWindow", null, "generateLayout",
					"com.android.internal.policy.impl.PhoneWindow.DecorView", new XC_MethodHook() { 
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					Window window = (Window) param.thisObject;
					Context context = window.getContext();
					String AppPackage = context.getPackageName();
 
					 if (!class_boolean.startsWith(AppPackage + ID_TAG)) return; 
					 
					// XposedBridge.log("XHaloFloatingWindow-DEBUG(DecorView): " + class_boolean);
					 
						String localClassPackageName = context.getClass().getPackage().getName();

					appleFloating(context, window,localClassPackageName);
					
				}
			});
		} catch (Exception e) { XposedBridge.log("XHaloFloatingWindow-ERROR(DecorView): " + e.toString());
		}
	}
	
	public static void appleFloating(Context context , Window mWindow, String class_name ){
		try{
		Intent intent__ = new Intent(context.getPackageManager().getLaunchIntentForPackage(class_name));
	        	ResolveInfo rInfo = context.getPackageManager().resolveActivity(intent__, 0);
	        	ActivityInfo info = rInfo.activityInfo;	            
	        	TypedArray ta = context.obtainStyledAttributes(info.theme, com.android.internal.R.styleable.Window);
	        	
	            TypedValue backgroundValue = ta.peekValue(com.android.internal.R.styleable.Window_windowBackground);
	            
	            if (backgroundValue != null && backgroundValue.toString().contains("light")) {
	                context.getTheme().applyStyle(R.style.Theme_Halo_FloatingWindowLight, true);
	            } else {  //Checks if light or dark theme
	                context.getTheme().applyStyle(R.style.Theme_Halo_FloatingWindow, true);
	            }
	            
	            ta.recycle();
		}catch(Throwable t){
            context.getTheme().applyStyle(R.style.Theme_Halo_FloatingWindow, true);
		}
	            // Create our new window
	           //mWindow.mIsFloatingWindow = true; < We dont need this. onCreate Hook will compare getTaskId and resize accordingly
	            mWindow.setCloseOnTouchOutsideIfNotSet(true);
	            mWindow.setGravity(Gravity.CENTER);
	            mWindow.setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND,WindowManager.LayoutParams.FLAG_DIM_BEHIND);
	            WindowManager.LayoutParams params = mWindow.getAttributes(); 
	    	    pref = new XSharedPreferences(Res.MY_PACKAGE_NAME,Res.MY_PACKAGE_NAME);
				Float alp = pref.getFloat(Res.KEY_ALPHA, Res.DEFAULT_ALPHA);
				Float dimm = pref.getFloat(Res.KEY_DIM, Res.DEFAULT_DIM);

	            params.alpha = alp;	
	            params.dimAmount = dimm;
	            mWindow.setAttributes((android.view.WindowManager.LayoutParams) params);
			     scaleFloatingWindow(context,mWindow);
	}

	public static void scaleFloatingWindow(Context context ,  Window mWindow ) {
		DisplayMetrics metrics = new DisplayMetrics();
		try{
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        display.getMetrics(metrics); 
		}catch (Exception e){
			DisplayMetrics dm = context.getResources().getDisplayMetrics();
			metrics = new DisplayMetrics();
			metrics = dm;
		}
	    pref = new XSharedPreferences(Res.MY_PACKAGE_NAME,Res.MY_PACKAGE_NAME);
        if (metrics.heightPixels > metrics.widthPixels) { // portrait 
        	Float width_portrait = pref.getFloat(Res.KEY_PORTRAIT_WIDTH, Res.DEFAULT_PORTRAIT_WIDTH);
    		Float height__portrait = pref.getFloat(Res.KEY_PORTRAIT_HEIGHT, Res.DEFAULT_PORTRAIT_HEIGHT);
            mWindow.setLayout((int)(metrics.widthPixels * width_portrait), (int)(metrics.heightPixels * height__portrait));
        } else {  // landscape
        	Float width_ls = pref.getFloat(Res.KEY_LANDSCAPE_WIDTH, Res.DEFAULT_LANDSCAPE_WIDTH);
    		Float height__ls = pref.getFloat(Res.KEY_LANDSCAPE_HEIGHT, Res.DEFAULT_LANDSCAPE_HEIGHT);
        	mWindow.setLayout((int)(metrics.widthPixels * width_ls), (int)(metrics.heightPixels * height__ls));
        }
        mWindow.setWindowAnimations(android.R.style.Animation_Dialog);
        mWindow.setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        mWindow.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
		int i = pref.getInt(Res.KEY_KEYBOARD_MODE, Res.DEFAULT_KEYBOARD_MODE);
		if (i ==2){
	        mWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
		}else if (i == 3){
	        mWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		}
        mWindow.getCallback().onWindowAttributesChanged(mWindow.getAttributes());

    }

}


 
