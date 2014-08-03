package com.zst.xposed.halo.floatingwindow.preferences;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

import com.zst.xposed.halo.floatingwindow.Common;
import com.zst.xposed.halo.floatingwindow.R;

public class MovableFragment extends PreferenceFragment {
	
	static MovableFragment mInstance;
	SharedPreferences mPref;
	
	public static MovableFragment getInstance() {
		if (mInstance == null) {
			mInstance = new MovableFragment();
		}
		return mInstance;
	}
	@Override
	@SuppressWarnings("deprecation")
	@SuppressLint("WorldReadableFiles")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getPreferenceManager().setSharedPreferencesName(Common.PREFERENCE_MAIN_FILE);
		getPreferenceManager().setSharedPreferencesMode(PreferenceActivity.MODE_WORLD_READABLE);
		addPreferencesFromResource(R.xml.pref_movable);
		findPreference(Common.KEY_MOVABLE_WINDOW + "_titlebar_screen").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				getActivity().startActivity(new Intent(getActivity(), TitleBarSettingsActivity.class));
				return false;
			}
		});
		mPref = getActivity().getSharedPreferences(Common.PREFERENCE_MAIN_FILE,
				PreferenceActivity.MODE_WORLD_READABLE);
	}
}