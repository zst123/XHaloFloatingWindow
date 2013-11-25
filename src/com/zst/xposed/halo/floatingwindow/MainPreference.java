package com.zst.xposed.halo.floatingwindow;

import com.zst.xposed.halo.floatingwindow.preferences.MainFragment;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class MainPreference extends PreferenceActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getFragmentManager().beginTransaction().replace(android.R.id.content, new MainFragment())
				.commit();
	}
}
