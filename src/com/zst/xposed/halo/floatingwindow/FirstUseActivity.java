package com.zst.xposed.halo.floatingwindow;

import java.util.LinkedList;

import com.zst.xposed.halo.floatingwindow.helpers.FirstUseHelper;
import com.zst.xposed.halo.floatingwindow.preferences.adapters.PageAdapter;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class FirstUseActivity extends Activity {
	
	public static final int MENU_TOGGLE = 1;
	
	public static class Pages {
		public static final int WELCOME = 100;
		public static final int HALO_FLAG_CONFLICTING = 300;
		public static final int APK_ON_SYSTEM = 400;
		public static final int KK_VISIBILITY_BUG_CONFIRMED = 501;
		public static final int KK_VISIBILITY_BUG_SUSPECTED = 502;
		public static final int FINISH = 600;
	}
	
	ViewPager mViewPager;
	PageAdapter mPageAdapter;
	// Thanks: http://just-another-blog.net/programming/how-to-implement-horizontal-view-swiping-with-tabs/
	// Thanks: http://stackoverflow.com/questions/15845632/adding-preferencefragment-to-fragmentpageradapter
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTheme(android.R.style.Theme_Holo_Light_NoActionBar);
		setContentView(R.layout.activity_viewpager);
		
		LinkedList<Integer> fragmentPages = new LinkedList<Integer>();
		fragmentPages.add(Pages.WELCOME);
		/*if (FirstUseHelper.hasMainHaloFlagInRom())*/ fragmentPages.add(Pages.HALO_FLAG_CONFLICTING);
		/*if (FirstUseHelper.isAppOnSystem(this))*/ fragmentPages.add(Pages.APK_ON_SYSTEM);
		
		switch (FirstUseHelper.hasKitkatVisibilityBug(this)) {
		case 1:
			fragmentPages.add(Pages.KK_VISIBILITY_BUG_CONFIRMED);
			break;
		case 2:
			fragmentPages.add(Pages.KK_VISIBILITY_BUG_SUSPECTED);
			break;
		}
		fragmentPages.add(Pages.FINISH);
		
		mPageAdapter = new PageAdapter(getFragmentManager(), fragmentPages) {
			@Override
			public View makeView(LayoutInflater inflater, ViewGroup container, int position) {
				switch (position) {
				case Pages.WELCOME:
					return inflater.inflate(R.layout.firstuse_welcome, container, false);
				case Pages.HALO_FLAG_CONFLICTING:
					return inflater.inflate(R.layout.firstuse_haloconflict, container, false);
				case Pages.APK_ON_SYSTEM:
					View vg = inflater.inflate(R.layout.firstuse_haloconflict, container, false);
					((TextView) vg.findViewById(android.R.id.title)).setText(getResources()
							.getString(R.string.firstuse_system_title));
					((TextView) vg.findViewById(android.R.id.text1)).setText(getResources()
							.getString(R.string.firstuse_system_info));
					((TextView) vg.findViewById(android.R.id.text2)).setText(getResources()
							.getString(R.string.firstuse_system_fix));
					return vg;
					//TODO finish up KK bug page
				case Pages.FINISH:
					View view = inflater.inflate(R.layout.firstuse_finish, container, false);
					view.findViewById(android.R.id.button1).setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							finish();
						}
					});
					return view;
				}
				return inflater.inflate(R.layout.activity_testing, container, false);
			
			}
		};
		mViewPager = (ViewPager) findViewById(R.id.view_pager);
		mViewPager.setAdapter(mPageAdapter);
		findViewById(R.id.pager_title_strip).setVisibility(View.GONE);
		
		//switch (getPosition()) {
		
		/*if (Pages.WELCOME == getPosition()) {
			return inflater.inflate(R.layout.fragment_welcome, container, false);
		} else if (PAGES.SKYPE_RESOLVER.value == getPosition()) {
			return SkypeResolverHelper.getInstance().initView(inflater, container);
		} else if (PAGES.PORT_SCANNER.value == getPosition()) {
			return PortScannerHelper.getInstance().initView(inflater, container);
		} else if (PAGES.CONTACT_ME.value == getPosition()) {
			return ContactMeFormHelper.getInstance().initView(inflater, container);
		} else if (PAGES.GET_IP.value == getPosition()) {
			return GetIPHelper.getInstance().initView(inflater, container);
		}
		return inflater.inflate(R.layout.list_port_scanner, container, false);*/
	}
		
			//	Log.d("zst123", "path=" + getApplicationInfo().sourceDir);
				//Log.d("zst123", "hasMainHaloFlagInRom" + hasMainHaloFlagInRom());
	//}
	

	
	
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		/*menu.add(Menu.NONE, MENU_TOGGLE, 0, R.string.pref_toggle_service_title)
			.setIcon(SidebarService.isRunning ?
					R.drawable.ic_menu_toggle_off : R.drawable.ic_menu_toggle_on)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);*/
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_TOGGLE:
			
		}
		return super.onOptionsItemSelected(item);
	}
	
	///}
	
	/***********************************************************************************/
	/***********************************************************************************/
	/***********************************************************************************/
	/***********************************************************************************/
	/***********************************************************************************/
	/***********************************************************************************/
	/***********************************************************************************/
	private void ttest() {
		Log.d("zst123", "RELEASE" + android.os.Build.VERSION.RELEASE);       //The current development codename, or the string "REL" if this is a release build.
		Log.d("zst123", "BOARD" + android.os.Build.BOARD);                 //The name of the underlying board, like "goldfish".    
		Log.d("zst123", "BOOTLOADER" +android.os.Build.BOOTLOADER);            //  The system bootloader version number.
		Log.d("zst123", "BRAND" + android.os.Build.BRAND);                 //The brand (e.g., carrier) the software is customized for, if any.
		Log.d("zst123", "CPU_ABI"+ android.os.Build.CPU_ABI);               //The name of the instruction set (CPU type + ABI convention) of native code.
		Log.d("zst123", "CPU_ABI2"+android.os.Build.CPU_ABI2);              //  The name of the second instruction set (CPU type + ABI convention) of native code.
		Log.d("zst123", "DEVICE"+android.os.Build.DEVICE);                //  The name of the industrial design.
		Log.d("zst123", "DISPLAY"+android.os.Build.DISPLAY);               //A build ID string meant for displaying to the user
		Log.d("zst123", "FINGERPRINT"+android.os.Build.FINGERPRINT);           //A string that uniquely identifies this build.
		Log.d("zst123", "HARDWARE"+android.os.Build.HARDWARE);              //The name of the hardware (from the kernel command line or /proc).
		Log.d("zst123", "HOST"+android.os.Build.HOST);  
		Log.d("zst123", "ID"+android.os.Build.ID);                    //Either a changelist number, or a label like "M4-rc20".
		Log.d("zst123",  "MANUFACTURER"+android.os.Build.MANUFACTURER);          //The manufacturer of the product/hardware.
		Log.d("zst123", "MODEL"+android.os.Build.MODEL);                 //The end-user-visible name for the end product.
		Log.d("zst123", "PRODUCT"+android.os.Build.PRODUCT);               //The name of the overall product.
		Log.d("zst123", "RADIO"+android.os.Build.RADIO);                 //The radio firmware version number.
		Log.d("zst123", "TAGS"+android.os.Build.TAGS);                  //Comma-separated tags describing the build, like "unsigned,debug".
		Log.d("zst123", "TYPE"+android.os.Build.TYPE);                  //The type of build, like "user" or "eng".
		Log.d("zst123", "USER"+android.os.Build.USER);                  //
	}
	
	/***********************************************************************************/
	
	
	
}