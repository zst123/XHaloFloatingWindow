package com.zst.xposed.halo.floatingwindow.preferences;

import java.io.DataOutputStream;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.zst.xposed.halo.floatingwindow.Common;
import com.zst.xposed.halo.floatingwindow.R;

public class MainFragment extends PreferenceFragment implements OnPreferenceClickListener {
	
	static MainFragment mInstance;
	SharedPreferences mPref;
	
	public static MainFragment getInstance() {
		if (mInstance == null) {
			mInstance = new MainFragment();
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
		addPreferencesFromResource(R.xml.pref_main);
		findPreference(Common.KEY_KEYBOARD_MODE).setOnPreferenceClickListener(this);
		findPreference(Common.KEY_RESTART_SYSTEMUI).setOnPreferenceClickListener(this);
		findPreference(Common.KEY_STATUSBAR_TASKBAR_RESTART_SYSTEMUI).setOnPreferenceClickListener(this);
		findPreference(Common.KEY_BLACKLIST_APPS).setOnPreferenceClickListener(this);
		findPreference(Common.KEY_WHITELIST_APPS).setOnPreferenceClickListener(this);
		findPreference(Common.KEY_STATUSBAR_TASKBAR_PINNED_APPS).setOnPreferenceClickListener(this);
		mPref = getActivity().getSharedPreferences(Common.PREFERENCE_MAIN_FILE,
				PreferenceActivity.MODE_WORLD_READABLE);
	}
	
	@Override
	public boolean onPreferenceClick(Preference p) {
		String k = p.getKey();
		if (k.equals(Common.KEY_KEYBOARD_MODE)) {
			showKeyboardDialog();
			return true;
		} else if (k.equals(Common.KEY_RESTART_SYSTEMUI)
				|| k.equals(Common.KEY_STATUSBAR_TASKBAR_RESTART_SYSTEMUI)) {
			showKillPackageDialog("com.android.systemui");
			return true;
		}else if (k.equals(Common.KEY_BLACKLIST_APPS)) {
			showBlacklistActivity();
			return true;
		}else if (k.equals(Common.KEY_WHITELIST_APPS)) {
			showWhitelistActivity();
			return true;
		}else if (k.equals(Common.KEY_STATUSBAR_TASKBAR_PINNED_APPS)) {
			showStatusbarTaskbarPinAppActivity();
			return true;
		}
		return false;
	}
	
	private void showKeyboardDialog() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		final ListView modeList = new ListView(getActivity());
		
		builder.setView(modeList);
		builder.setTitle(R.string.pref_keyboard_title);
		
		final AlertDialog dialog = builder.create();
		final ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
				android.R.layout.simple_list_item_1, android.R.id.text1);
		
		adapter.add(getResources().getString(R.string.keyboard_default));
		adapter.add(getResources().getString(R.string.keyboard_pan));
		adapter.add(getResources().getString(R.string.keyboard_scale));
		
		modeList.setAdapter(adapter);
		modeList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
				String title = ((TextView) view.findViewById(android.R.id.text1))
						.getText().toString();
				if (title.equals(getResources().getString(R.string.keyboard_default))) {
					mPref.edit().putInt(Common.KEY_KEYBOARD_MODE, 1).commit();
				} else if (title.equals(getResources().getString(R.string.keyboard_pan))) {
					mPref.edit().putInt(Common.KEY_KEYBOARD_MODE, 2).commit();
				} else if (title.equals(getResources().getString(R.string.keyboard_scale))) {
					mPref.edit().putInt(Common.KEY_KEYBOARD_MODE, 3).commit();
				}
				Toast.makeText(getActivity(), title, Toast.LENGTH_SHORT).show();
				dialog.dismiss();
			}
		});
		dialog.show();
	}
	
	private void showKillPackageDialog(final String pkgToKill) {
		AlertDialog.Builder build = new AlertDialog.Builder(getActivity());
		build.setMessage(R.string.pref_systemui_restart_title);
		build.setNegativeButton(android.R.string.cancel, null);
		build.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				killPackage(pkgToKill);
			}
		});
		build.show();
	}
	
	private void killPackage(final String pkgToKill) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Process su = Runtime.getRuntime().exec("su");
					if (su == null) return;
					DataOutputStream os = new DataOutputStream(su.getOutputStream());
					os.writeBytes("pkill " + pkgToKill + "\n");
					os.writeBytes("exit\n");
					su.waitFor();
					os.close();
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
	
	private void showStatusbarTaskbarPinAppActivity() {
		startActivity(new Intent(getActivity(), StatusbarTaskbarPinAppActivity.class));
	}
	
	private void showWhitelistActivity() {
		startActivity(new Intent(getActivity(), WhitelistActivity.class));
	}
	
	private void showBlacklistActivity() {
		startActivity(new Intent(getActivity(), BlacklistActivity.class));
	}
}