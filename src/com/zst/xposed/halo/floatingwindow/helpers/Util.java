package com.zst.xposed.halo.floatingwindow.helpers;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import android.content.Context;

public class Util {
	
	/* Get System DPI from build.prop */
	public static int realDp(int dp, Context c) {
		String dpi = "";
		try {
			Process p = new ProcessBuilder("/system/bin/getprop", "ro.sf.lcd_density")
					.redirectErrorStream(true).start();
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = "";
			while ((line = br.readLine()) != null) {
				dpi = line;
			}
			p.destroy();
		} catch (Exception e) {
			dpi = "0";
		}
		float scale = Integer.parseInt(dpi);
		if (scale == 0) {
			scale = c.getResources().getDisplayMetrics().density;
		} else {
			scale = (scale / 160);
		}
		int pixel = (int) (dp * scale + 0.5f);
		return pixel;
	}
	
	/* Get App DPI */
	public static int dp(int dp, Context c) {
		float scale = c.getResources().getDisplayMetrics().density;
		int pixel = (int) (dp * scale + 0.5f);
		return pixel;
	}
}
