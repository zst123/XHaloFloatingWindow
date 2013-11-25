package com.zst.xposed.halo.floatingwindow.preferences;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import com.zst.xposed.halo.floatingwindow.Common;
import com.zst.xposed.halo.floatingwindow.R;

public class MainFragment extends PreferenceFragment implements OnPreferenceClickListener {
	
	SharedPreferences mPref;
	
	@Override
	@SuppressLint("WorldReadableFiles")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.pref_main);
		findPreference(Common.KEY_GRAVITY).setOnPreferenceClickListener(this);
		findPreference(Common.KEY_KEYBOARD_MODE).setOnPreferenceClickListener(this);
		getPreferenceManager().setSharedPreferencesMode(PreferenceActivity.MODE_WORLD_READABLE);
		getPreferenceManager().setSharedPreferencesName(Common.PREFERENCE_MAIN_FILE);
		mPref = getActivity().getSharedPreferences(Common.PREFERENCE_MAIN_FILE,
				PreferenceActivity.MODE_WORLD_READABLE);
	}
	
	@Override
	public boolean onPreferenceClick(Preference p) {
		String k = p.getKey();
		if (k.equals(Common.KEY_GRAVITY)) {
			showGravityDialog();
			return true;
		}
		return false;
	}
	
	public void showGravityDialog() {
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
}