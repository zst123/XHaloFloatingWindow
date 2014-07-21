package com.zst.xposed.halo.floatingwindow.hooks.ipc;

import java.util.HashMap;
import com.zst.xposed.halo.floatingwindow.Common;
import com.zst.xposed.halo.floatingwindow.helpers.AeroSnap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class XHFWService extends XHFWInterface.Stub {
	/*
	 * ---- XHFW IPC Service ----
	 * Thanks to help from these websites:
	 * http://stackoverflow.com/questions/19325010/aosp-stubs-vs-getsystemservice
	 * http://processors.wiki.ti.com/index.php/Android-Adding_SystemService
	 * https://github.com/SpazeDog/xposed-additions/commit/
	 * d22bb72a6b08ea409b7b5f6472f2fff22381ac62
	 */
	
	/********************************************************************/
	/** Hooks **/
	/********************************************************************/
	
	static final String SERVICE_NAME = "XHaloFloatingWindow-Service";
	static Class<?> classSvcMgr;
	
	public static void initZygote() throws Throwable {
		classSvcMgr = XposedHelpers.findClass("android.os.ServiceManager", null);
		final Class<?> classAMS = XposedHelpers.findClass(
				"com.android.server.am.ActivityManagerService", null);
		
		XposedBridge.hookAllMethods(classAMS, "main", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				try {
					final Class<?>[] paramType = { String.class, IBinder.class };
					final XHFWService server = new XHFWService((Context) param.getResult());
					
					XposedHelpers.callStaticMethod(classSvcMgr, "addService", paramType,
							SERVICE_NAME, server);
				} catch (Throwable e) {
					XposedBridge.log("Error hooking ActivityManagerService ==> See Logcat");
					e.printStackTrace();
					// We are hooking a system method and might cause a
					// bootloop if it throws any exception, log this throwable.
				}
			}
		});
	}
	
	public static XHFWInterface retrieveService(Context context) {
		try {
			Object service = XposedHelpers.callStaticMethod(classSvcMgr, "getService",
					new Class<?>[] { String.class }, SERVICE_NAME);
			// ServiceManager.getService(SERVICE_NAME);
			XHFWInterface server = XHFWInterface.Stub.asInterface((IBinder) service);
			if (!server.asBinder().pingBinder()) {
				XposedBridge.log(Common.LOG_TAG + "XHFWService is not running");
			}
			return server;
		} catch (Exception e) {
			XposedBridge.log(Common.LOG_TAG + "Error Retrieving XHFWService - see logcat -->"
					+ e.toString());
			e.printStackTrace();
			return null;
		}
	}
	
	/********************************************************************/
	/** Main **/
	/********************************************************************/
	Context mContext;
	
	public XHFWService(Context c) {
		mContext = c;
	}
	
	/********************************************************************/
	/** App Management **/
	/********************************************************************/
	/*
	HashMap<String, AppInfo> mAppInfoList = new HashMap<String, AppInfo>();
	
	@Override
	public void setApp(String intent_id, String pkgName, boolean floating, int snapSide)
			throws RemoteException {
		if (!floating) {
			mAppInfoList.remove(pkgName);
			Log.d("zst123", "floating = false // " + pkgName);
		} else {
			AppInfo ai = mAppInfoList.get(pkgName);
			if (ai == null) {
				Log.d("zst123", "floating = true /null/ " + pkgName);
				ai = new AppInfo();
				mAppInfoList.put(pkgName, ai);
			}
			ai.intentId = intent_id;
			ai.pkgName = pkgName;
			ai.isFloating = floating;
			ai.snapSide = snapSide;
			ai.timeUpdated = System.currentTimeMillis();
			Log.d("zst123", "floating = true // id="+intent_id+"/pkg=" + pkgName + "/snapSide="+snapSide);
		}
	}
	
	@Override
	public boolean getAppFloating(String intent_id, String pkgName) throws RemoteException {
		AppInfo ai = mAppInfoList.get(pkgName);
		if (ai != null && ai.intentId == intent_id) {
			return ai.isFloating;
		}
		return false;
	}
	
	@Override
	public int getAppSnapSide(String intent_id, String pkgName) throws RemoteException {
		AppInfo ai = mAppInfoList.get(pkgName);
		if (ai != null && ai.intentId == intent_id) {
			return ai.snapSide;
		}
		return AeroSnap.UNKNOWN;
	}
	
	class AppInfo {
		String intentId;
		String pkgName;
		boolean isFloating;
		int snapSide;
		long timeUpdated;
	}
	*/
	
	/********************************************************************/
	/** Window Management **/
	/********************************************************************/
	
	Object mWindowManager;
	ActivityManager mActivityManager;
	int mLastTaskId;
	
	@Override
	public int getLastTaskId() throws RemoteException {
		return mLastTaskId;
	}
	
	@Override
	public void bringAppToFront(IBinder token, int taskId) throws RemoteException {
		final long origId = Binder.clearCallingIdentity();
		if (mWindowManager == null) {
			mWindowManager = getIWindowManagerProxy();
		}
		
		if (mActivityManager == null) {
			mActivityManager = (ActivityManager) mContext
					.getSystemService(Context.ACTIVITY_SERVICE);
		}
		
		try {
			Class<?>[] classes = { IBinder.class, Boolean.class };
			XposedHelpers.callMethod(mWindowManager, "setFocusedApp", classes, token, false);
			// mWindowManager.setFocusedApp(token, false);
		} catch (Exception e) {
			XposedBridge.log(Common.LOG_TAG + "Cannot change App Focus");
			XposedBridge.log(e);
			Log.d("test1", Common.LOG_TAG + "Cannot change App Focus", e);
		}
		
		mLastTaskId = taskId;
		try {
			mActivityManager.moveTaskToFront(taskId, ActivityManager.MOVE_TASK_NO_USER_ACTION);
		} catch (Exception e) {
			XposedBridge.log(Common.LOG_TAG + "Cannot move task to front");
			XposedBridge.log(e);
			Log.e("test1", Common.LOG_TAG + "Cannot move task to front", e);
		}
		Binder.restoreCallingIdentity(origId);
	}
	
	// http://stackoverflow.com/questions/9604644/how-do-i-get-a-reference-to-connectivityservice-object
	public final static Object getIWindowManagerProxy() {
		Class<?> serviceManagerClass = XposedHelpers.findClass("android.os.ServiceManager", null);
		IBinder binderProxy = (IBinder) XposedHelpers.callStaticMethod(serviceManagerClass,
				"getService", "window");
		// ServiceManager.getService("window");
		/*
		 * Now use pass the ServiceManager BinderProxy to the 'asInterface'
		 * method of the interface Stub inner class
		 */
		Class<?> stubClass = XposedHelpers.findClass("android.view.IWindowManager$Stub", null);
		return XposedHelpers.callStaticMethod(stubClass, "asInterface",
				new Class[] { IBinder.class }, binderProxy);
		// IWindowManager.Stub.asInterface(binderPRoxy);
	}

	@Override
	public void removeAppTask(int taskId, int flags) throws RemoteException {
		if (mActivityManager == null) {
			mActivityManager = (ActivityManager) mContext
					.getSystemService(Context.ACTIVITY_SERVICE);
		}
		XposedHelpers.callMethod(mActivityManager, "removeTask",
				taskId, flags);
		// mAm.removeTask(taskId, flags);
	}
}
