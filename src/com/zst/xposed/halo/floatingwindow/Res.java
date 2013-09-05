package com.zst.xposed.halo.floatingwindow;

public class Res {
// PREFERENCES KEYS
	public static final String MY_PACKAGE_NAME = Res.class.getPackage().getName();
	public static final String KEY_ALPHA = "alpha";
	public static final String KEY_DIM = "dimAmount";
	public static final String KEY_PORTRAIT_WIDTH = "pwidth";
	public static final String KEY_PORTRAIT_HEIGHT = "pheight";
	public static final String KEY_LANDSCAPE_WIDTH = "lwidth";
	public static final String KEY_LANDSCAPE_HEIGHT = "lheight";
	public static final String KEY_KEYBOARD_MODE = "kbmode_softkb";
	public static final String KEY_APP_PAUSE = "app_pausing_rocks";



// PREFERENCE DEFAULT VALUE
	public static final Float DEFAULT_ALPHA = 1f;
	public static final Float DEFAULT_DIM = 0.25f;
	public static final Float DEFAULT_PORTRAIT_WIDTH = 0.95f;
	public static final Float DEFAULT_PORTRAIT_HEIGHT = 0.7f;
	public static final Float DEFAULT_LANDSCAPE_WIDTH = 0.7f;
	public static final Float DEFAULT_LANDSCAPE_HEIGHT = 0.85f;
	public static final int DEFAULT_KEYBOARD_MODE = 1;
	public static final boolean DEFAULT_APP_PAUSE = false;


	
	public static boolean notFloating = true;
	public static int previousUid = -756456451;





	


	public static final String NULL = "nulled";

}
