package com.zst.xposed.halo.floatingwindow.hooks;

import static de.robv.android.xposed.XposedHelpers.findClass;

import android.content.Context;
import android.content.Intent;
import android.content.res.XModuleResources;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import com.zst.xposed.halo.floatingwindow.Common;
import com.zst.xposed.halo.floatingwindow.R;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class RecentAppsHook {
	
	static String TEXT_APP_INFO;
	static String TEXT_OPEN_IN_HALO;
	static String TEXT_REMOVE_FROM_LIST;
	
	static final int ID_REMOVE_FROM_LIST = 1000;
	static final int ID_APP_INFO = 2000;
	static final int ID_OPEN_IN_HALO = 3000;
	
	public static void initZygote(XModuleResources module_res) {
		TEXT_APP_INFO = module_res.getString(R.string.recents_app_info);
		TEXT_OPEN_IN_HALO = module_res.getString(R.string.recents_open_halo);
		TEXT_REMOVE_FROM_LIST = module_res.getString(R.string.recents_remove_from_list);
	}
	
	public static void handleLoadPackage(final LoadPackageParam lpp, final XSharedPreferences pref) {
		if (!lpp.packageName.equals("com.android.systemui")) return;
		pref.reload();
		if (pref.getBoolean(Common.KEY_SYSTEM_RECENTS_LONGPRESS_OPTION,
				Common.DEFAULT_SYSTEM_RECENTS_LONGPRESS_OPTION)) {
			injectMenu(lpp);
		}
	}
	
	private static void injectMenu(final LoadPackageParam lpp) {
		final Class<?> hookClass = findClass("com.android.systemui.recent.RecentsPanelView",
				lpp.classLoader);
		XposedBridge.hookAllMethods(hookClass, "handleLongPress", new XC_MethodHook() {
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				final View thiz = (View) param.thisObject;
				final View selectedView = (View) param.args[0];
				final View anchorView = (View) param.args[1];
				final View thumbnailView = (View) param.args[2];
				
				thumbnailView.setSelected(true);
				
				PopupMenu popup = new PopupMenu(thiz.getContext(), anchorView == null ? selectedView : anchorView);
				popup.getMenu().add(Menu.NONE, ID_REMOVE_FROM_LIST, 1, TEXT_REMOVE_FROM_LIST);
				popup.getMenu().add(Menu.NONE, ID_APP_INFO, 2, TEXT_APP_INFO);
				popup.getMenu().add(Menu.NONE, ID_OPEN_IN_HALO, 3, TEXT_OPEN_IN_HALO);
				
				try {
					XposedHelpers.setObjectField(thiz, "mPopup", popup);
				} catch (Exception e) {
					// User on ICS
				}
				
				final Object viewHolder = selectedView.getTag();
				
				final PopupMenu.OnMenuItemClickListener menu = new PopupMenu.OnMenuItemClickListener() {
					public boolean onMenuItemClick(MenuItem item) {
						try {
							switch (item.getItemId()) {
							case ID_REMOVE_FROM_LIST:
								ViewGroup recentsContainer = (ViewGroup) XposedHelpers
										.getObjectField(thiz, "mRecentsContainer");
								recentsContainer.removeViewInLayout(selectedView);
								return true;
							case ID_APP_INFO:
								if (viewHolder != null) {
									closeRecentApps(thiz);
									Object ad = XposedHelpers.getObjectField(viewHolder, "taskDescription");
									String pkg_name = (String) XposedHelpers.getObjectField(ad, "packageName");
									startApplicationDetailsActivity(thiz.getContext(), pkg_name);
								}
								return true;
							case ID_OPEN_IN_HALO:
								if (viewHolder != null) {
									closeRecentApps(thiz);
									Object ad = XposedHelpers.getObjectField(viewHolder, "taskDescription");
									final Intent intent = (Intent) XposedHelpers.getObjectField(ad, "intent");
									intent.addFlags(Common.FLAG_FLOATING_WINDOW
											| Intent.FLAG_ACTIVITY_MULTIPLE_TASK
											| Intent.FLAG_ACTIVITY_NO_USER_ACTION
											| Intent.FLAG_ACTIVITY_NEW_TASK);
									if (Build.VERSION.SDK_INT >= 19) {
										thiz.post(new Runnable() {
											@Override
											public void run() {
												thiz.getContext().startActivity(intent);
											}
										});
									} else {
										thiz.getContext().startActivity(intent);
									}
								}
								return true;
							}
						} catch (Throwable t) {
							XposedBridge.log(Common.LOG_TAG + "RecentAppsHook / onMenuItemClick (" + item.getItemId() + ")");
							XposedBridge.log(t);
						}
						return false;
					}
				};
				popup.setOnMenuItemClickListener(menu);
				popup.setOnDismissListener(new PopupMenu.OnDismissListener() {
					public void onDismiss(PopupMenu menu) {
						thumbnailView.setSelected(false);
						try {
							XposedHelpers.setObjectField(thiz, "mPopup", null);
						} catch (Exception e) {
							// User on ICS
						}
					}
				});
				popup.show();
				param.setResult(null);
			}
		});
	}
	
	private static void closeRecentApps(View thiz) {
		if (Build.VERSION.SDK_INT >= 16) {
			try {
				XposedHelpers.callMethod(thiz, "dismissAndGoBack");
				return;
			} catch (Exception e) {
			}
		}
		
		Object bar = /* StatusBarManager */ thiz.getContext().getSystemService("statusbar");
		if (bar != null) {
			try {
				XposedHelpers.callMethod(bar, "collapse");
				return;
			} catch (Throwable e) {
			}
		}
		
		new Thread() {
			@Override
			public void run() {
				try {
					Runtime.getRuntime().exec("input keyevent " + KeyEvent.KEYCODE_BACK);
				} catch (Exception e) {
				}
			}
		}.start();
	}
	
	private static void startApplicationDetailsActivity(Context ctx, String packageName) {
		Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts(
				"package", packageName, null));
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		ctx.startActivity(intent);
	}
}
