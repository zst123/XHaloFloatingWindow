package com.zst.xposed.halo.floatingwindow;

import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class TestingActivity extends Fragment implements View.OnClickListener {
	
	static TestingActivity mInstance;
	
	public static TestingActivity getInstance() {
		if (mInstance == null) {
			mInstance = new TestingActivity();
		}
		return mInstance;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
        return inflater.inflate(R.layout.activity_testing, container, false);
    }
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		initXposedLoaded(false);
		initButtons();
	}
	
	/* Hooked by this class to change loaded to true */
	public void initXposedLoaded(boolean loaded) {
		TextView title = (TextView) getView().findViewById(android.R.id.title);
		TextView subtitle = (TextView) getView().findViewById(android.R.id.message);
		View bg = getView().findViewById(android.R.id.background);
		if (loaded) {
			bg.setBackgroundColor(0xFF669900);
			title.setText(R.string.pref_testing_active_title);
			subtitle.setText(R.string.pref_testing_active_subtitle);
		} else {
			bg.setBackgroundColor(0xFFCC1111);
			title.setText(R.string.pref_testing_inactive_title);
			subtitle.setText(R.string.pref_testing_inactive_subtitle);
		}
	}
	
	private void initButtons() {
		getView().findViewById(R.id.button1).setOnClickListener(this);
		getView().findViewById(R.id.button2).setOnClickListener(this);
		getView().findViewById(R.id.Button01).setOnClickListener(this);
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.button1:
			final Intent intent = new Intent(Settings.ACTION_SETTINGS);
			intent.addFlags(Common.FLAG_FLOATING_WINDOW);
			startActivity(intent);
			break;
		case R.id.button2:
			final Intent intent1 = new Intent(getActivity().getPackageManager().getLaunchIntentForPackage(
					"de.robv.android.xposed.installer"));
			intent1.addFlags(Common.FLAG_FLOATING_WINDOW);
			startActivity(intent1);
			break;
		case R.id.Button01:
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.setData(Uri.parse(Common.XDA_THREAD));
			i.addFlags(Common.FLAG_FLOATING_WINDOW);
			startActivity(i);
			break;
		}
	}
}
