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
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.zst.xposed.halo.floatingwindow.Common;
import com.zst.xposed.halo.floatingwindow.R;
import com.zst.xposed.halo.floatingwindow.TestingActivity;

public class MainFragment extends PreferenceFragment implements OnPreferenceClickListener {
	
	SharedPreferences mPref;
	
	@Override
	@SuppressLint("WorldReadableFiles")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getPreferenceManager().setSharedPreferencesName(Common.PREFERENCE_MAIN_FILE);
		getPreferenceManager().setSharedPreferencesMode(PreferenceActivity.MODE_WORLD_READABLE);
		addPreferencesFromResource(R.xml.pref_main);
		findPreference(Common.KEY_GRAVITY).setOnPreferenceClickListener(this);
		findPreference(Common.KEY_KEYBOARD_MODE).setOnPreferenceClickListener(this);
		findPreference(Common.KEY_RESTART_SYSTEMUI).setOnPreferenceClickListener(this);
		findPreference(Common.KEY_BLACKLIST_APPS).setOnPreferenceClickListener(this);
		findPreference(Common.KEY_WHITELIST_APPS).setOnPreferenceClickListener(this);
		findPreference(Common.KEY_TESTING_SCREEN).setOnPreferenceClickListener(this);
		findPreference(Common.KEY_STATUSBAR_TASKBAR_PINNED_APPS).setOnPreferenceClickListener(this);
		mPref = getActivity().getSharedPreferences(Common.PREFERENCE_MAIN_FILE,
				PreferenceActivity.MODE_WORLD_READABLE);
	}
	
	@Override
	public boolean onPreferenceClick(Preference p) {
		String k = p.getKey();
		if (k.equals(Common.KEY_GRAVITY)) {
			showGravityDialog();
			return true;
		}else if (k.equals(Common.KEY_KEYBOARD_MODE)) {
			showKeyboardDialog();
			return true;
		}else if (k.equals(Common.KEY_RESTART_SYSTEMUI)) {
			showKillPackageDialog("com.android.systemui");
			return true;
		}else if (k.equals(Common.KEY_BLACKLIST_APPS)) {
			showBlacklistActivity();
			return true;
		}else if (k.equals(Common.KEY_WHITELIST_APPS)) {
			showWhitelistActivity();
			return true;
		}else if (k.equals(Common.KEY_TESTING_SCREEN)) {
			showTestScreen();
			return true;
		}else if (k.equals(Common.KEY_STATUSBAR_TASKBAR_PINNED_APPS)) {
			showStatusbarTaskbarPinAppActivity();
			return true;
		}
		return false;
	}
	
	private void showGravityDialog() {
		View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_gravity, null, false);
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		final AlertDialog dialog = builder.create();
		dialog.setTitle(R.string.pref_gravity_title);
		dialog.setView(view);
		dialog.show();
		int flags = mPref.getInt(Common.KEY_GRAVITY, Common.DEFAULT_GRAVITY);
		
		int id = R.id.RadioButton5;
		if (flags == (Gravity.TOP | Gravity.LEFT)) {
			id = R.id.RadioButton1;
		} else if (flags == (Gravity.TOP | Gravity.RIGHT)) {
			id = R.id.radioButton3;
		} else if (flags == (Gravity.CENTER)) {
			id = R.id.RadioButton5;
		} else if (flags == (Gravity.BOTTOM | Gravity.LEFT)) {
			id = R.id.RadioButton7;
		} else if (flags == (Gravity.BOTTOM | Gravity.RIGHT)) {
			id = R.id.RadioButton9;
		} else if (flags == (Gravity.TOP | Gravity.CENTER_HORIZONTAL)) {
			id = R.id.radioButton2;
		} else if (flags == (Gravity.LEFT | Gravity.CENTER_VERTICAL)) {
			id = R.id.radioButton4;
		} else if (flags == (Gravity.RIGHT | Gravity.CENTER_VERTICAL)) {
			id = R.id.radioButton6;
		} else if (flags == (Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL)) {
			id = R.id.radioButton8;
		}
		((RadioButton) dialog.findViewById(id)).setChecked(true);
		
		final OnClickListener listener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				switch (v.getId()) {
				case R.id.dialog_save:
					int id = ((RadioGroup) dialog.findViewById(R.id.radioSex))
							.getCheckedRadioButtonId();
					int newGravity = Gravity.CENTER;
					if (id == R.id.RadioButton1) {
						newGravity = (Gravity.TOP | Gravity.LEFT);
					} else if (id == R.id.radioButton3) {
						newGravity = (Gravity.TOP | Gravity.RIGHT);
					} else if (id == R.id.RadioButton5) {
						newGravity = (Gravity.CENTER);
					} else if (id == R.id.RadioButton7) {
						newGravity = (Gravity.BOTTOM | Gravity.LEFT);
					} else if (id == R.id.RadioButton9) {
						newGravity = (Gravity.BOTTOM | Gravity.RIGHT);
					} else if (id == R.id.radioButton2) {
						newGravity = (Gravity.TOP | Gravity.CENTER_HORIZONTAL);
					} else if (id == R.id.radioButton4) {
						newGravity = (Gravity.LEFT | Gravity.CENTER_VERTICAL);
					} else if (id == R.id.radioButton6) {
						newGravity = (Gravity.RIGHT | Gravity.CENTER_VERTICAL);
					} else if (id == R.id.radioButton8) {
						newGravity = (Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
					}
					mPref.edit().putInt(Common.KEY_GRAVITY, newGravity).commit();
					dialog.dismiss();
					break;
				case R.id.dialog_discard:
					dialog.dismiss();
					break;
				}
			}
		};
		Button save = (Button) dialog.findViewById(R.id.dialog_save);
		Button discard = (Button) dialog.findViewById(R.id.dialog_discard);
		save.setOnClickListener(listener);
		discard.setOnClickListener(listener);
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
	
	private void showTestScreen() {
		startActivity(new Intent(getActivity(), TestingActivity.class));
	}
}