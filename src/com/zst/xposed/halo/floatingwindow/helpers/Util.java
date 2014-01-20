package com.zst.xposed.halo.floatingwindow.helpers;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;

public class Util {
	
	/* Get System DPI from build.prop 
	 * Some ROMs have Per-App DPI and it might make our views inconsistent 
	 * Fallback to app dpi if it fails*/
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
			//failed, set to zero.
		}
		float scale = Integer.parseInt(dpi);
		if (scale == 0) {
			// zero means it failed in getting dpi, fallback to app dpi 
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
	
	/* Create a Border */
	public static ShapeDrawable makeOutline(int color, int thickness) {
		ShapeDrawable rectShapeDrawable = new ShapeDrawable(new RectShape());
		Paint paint = rectShapeDrawable.getPaint();
		paint.setColor(color);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(thickness);
		return rectShapeDrawable;
	}
}
