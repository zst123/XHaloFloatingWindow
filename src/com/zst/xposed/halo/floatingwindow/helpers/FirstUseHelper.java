package com.zst.xposed.halo.floatingwindow.helpers;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import com.zst.xposed.halo.floatingwindow.Common;

public class FirstUseHelper {
	
	public static boolean hasTouchWizDevice() {
		return Build.MANUFACTURER.equalsIgnoreCase("samsung");
		//TODO
	}
	
	/* If ROMs bundle the APK onto the system folder, warn the user */
	public static boolean isAppOnSystem(Context c) {
		return c.getApplicationInfo().sourceDir.toLowerCase(Locale.ENGLISH)
				.startsWith("/system/app/");
	}
	
	public static boolean isKitkatVisBugFixAppInstalled(PackageManager pm) {
		try {
			pm.getPackageInfo("com.zst.xposed.fix.nonfullscreenactivities",
					PackageManager.GET_ACTIVITIES);
			return true;
		} catch (PackageManager.NameNotFoundException e) {
			return false;
		}
	}
	
	/** @return 0 for no; 1 for confirmed; 2 for suspected */
	public static int hasKitkatVisibilityBug(Context c) {
		if (Build.VERSION.SDK_INT != 19)
			return 0;
		
		if (isKitkatVisBugFixAppInstalled(c.getPackageManager())) {
			return 0;
		}
		
		if (Build.ID.toUpperCase(Locale.ENGLISH).startsWith("KOT49") ||
			// Nexus Devices 4.4.1 and 4.4.2
			Build.ID.toUpperCase(Locale.ENGLISH).startsWith("KRT16")) {
			// Nexus Devices 4.4.0
			
			return 1;
		}
		
		if (Build.VERSION.RELEASE.equals("4.4") ||
			Build.VERSION.RELEASE.equals("4.4.1")) {
			return 1;
		}
		
		if (Build.VERSION.RELEASE.equals("4.4.2")) {
			// https://github.com/android/platform_frameworks_base/commit/446ef1de8d373c1b017df8d19ebf9a47811fb402
			// Only fixed in AOSP 4.4.2 Jan 9, 2014.
			// So we need to confirm with the system
			return 2;
		}
		
		return 0;
	}
	
	/**
	 * @return true if ROM uses 0x00002000 flag
	 */
	public static boolean hasMainHaloFlagInRom() {
		if (Build.VERSION.SDK_INT <= 15) {
			// All ICS ROMs doesn't have Halo or Omni Multiwindow
			// They only started in Jelly Bean
			return false;
		}
		
		try {
			Field haloField = Intent.class.getDeclaredField("FLAG_FLOATING_WINDOW");
			haloField.setAccessible(true);
			if (Modifier.isStatic(haloField.getModifiers())) {
				if (haloField.getInt(null) == Common.FLAG_FLOATING_WINDOW) {
					return true;
				}
			}
		} catch (Exception e1) { // just continue
		}
		try {
			Field omniMwField = Intent.class.getDeclaredField("FLAG_ACTIVITY_SPLIT_VIEW");
			omniMwField.setAccessible(true);
			if (Modifier.isStatic(omniMwField.getModifiers())) {
				if (omniMwField.getInt(null) == Common.FLAG_FLOATING_WINDOW) {
					return true;
				}
			}
		} catch (Exception e1) { // just continue
		}
		
		for (Field field : Intent.class.getDeclaredFields()) {
			field.setAccessible(true);
			if (Modifier.isFinal(field.getModifiers()) &&
					Modifier.isStatic(field.getModifiers())) {
				try {
					Object integer = field.get(null);
					if (integer instanceof Integer) {
						if ((Integer) integer == Common.FLAG_FLOATING_WINDOW) {
							return true;
						}
					}
				} catch (Exception e) {
					// just continue
				}
			}
		}
		return false;
	}
}
