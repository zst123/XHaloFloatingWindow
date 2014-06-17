package com.zst.xposed.halo.floatingwindow.helpers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;

import de.robv.android.xposed.XposedHelpers;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RectShape;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;

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
	
	public static ShapeDrawable makeCircle(int color, int diameter) {
		ShapeDrawable shape = new ShapeDrawable(new OvalShape());
		Paint paint = shape.getPaint();
		paint.setColor(color);
		paint.setStyle(Style.FILL);
		paint.setAntiAlias(true);
		shape.setIntrinsicHeight(diameter);
		shape.setIntrinsicWidth(diameter);
		return shape;
	}
	
	/* Rotate a drawable given an angle */
	public static Drawable getRotateDrawable(final Drawable d, final float angle) {
		final Drawable[] array = { d };
		return new LayerDrawable(array) {
			@Override
			public void draw(final Canvas canvas) {
				canvas.save();
				canvas.rotate(angle, d.getBounds().width() / 2, d.getBounds().height() / 2);
				super.draw(canvas);
				canvas.restore();
			}
		};
	}
	
	/* Set background drawable based on the API */
	@SuppressWarnings("deprecation")
	public static void setBackgroundDrawable(View view, Drawable drawable) {
		if (Build.VERSION.SDK_INT >= 16) {
			view.setBackground(drawable);
		} else {
			view.setBackgroundDrawable(drawable);
		}
	}
	
	public static void addPrivateFlagNoMoveAnimationToLayoutParam(WindowManager.LayoutParams params) {
		if (Build.VERSION.SDK_INT <= 15) return;
		
		try {
			Field fieldPrivateFlag = XposedHelpers.findField(WindowManager.LayoutParams.class, "privateFlags");
			fieldPrivateFlag.setInt(params, (fieldPrivateFlag.getInt(params) | 0x00000040));
		} catch (Exception e) {
			
		}
		/* this private flag is only in JB and above to turn off move animation.
		 * we need this to speed up our resizing */
		// params.privateFlags |= 0x00000040; //PRIVATE_FLAG_NO_MOVE_ANIMATION
	}
}
