package com.zst.xposed.halo.floatingwindow;

import com.zst.xposed.halo.floatingwindow.preferences.MainFragment;
import com.zst.xposed.halo.floatingwindow.preferences.MovableFragment;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;

public class MainPreference extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_viewpager);
		
		FragmentPagerAdapter adapter = new FragmentPagerAdapter(getFragmentManager()) {
			@Override
			public Fragment getItem(int position) {
				switch (position) {
				case 0:
					return MainFragment.getInstance();
				case 1:
					return MovableFragment.getInstance();
				case 2:
					return TestingActivity.getInstance();
				}
				return new Fragment();
			}
			
			@Override
			public String getPageTitle(int pos) {
				switch (pos) {
				case 0:
					return getResources().getString(R.string.pref_main_top_title);
				case 1:
					return getResources().getString(R.string.pref_movable_top_title);
				case 2:
					return getResources().getString(R.string.pref_testing_top_title);
				}
				return "";
			}
			
			@Override
			public int getCount() {
				return 3;
			}
		};
		ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);
		viewPager.setAdapter(adapter);
		
		PagerTabStrip pts = (PagerTabStrip) findViewById(R.id.pager_title_strip);
		pts.setTabIndicatorColor(0xFF333333);
		pts.setTextColor(0xFF333333);
		pts.setBackgroundColor(Color.TRANSPARENT);
	}
}
