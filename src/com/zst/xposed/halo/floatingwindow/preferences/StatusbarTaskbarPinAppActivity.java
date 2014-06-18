package com.zst.xposed.halo.floatingwindow.preferences;

import java.util.Set;

import com.zst.xposed.halo.floatingwindow.Common;
import com.zst.xposed.halo.floatingwindow.R;
import com.zst.xposed.halo.floatingwindow.preferences.adapters.AppAdapter;
import com.zst.xposed.halo.floatingwindow.preferences.adapters.AppAdapter.AppItem;
import com.zst.xposed.halo.floatingwindow.preferences.adapters.PackageNameAdapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.AdapterView.OnItemClickListener;

public class StatusbarTaskbarPinAppActivity extends Activity {
	
	/* others */
	static final int ID_ADD = 1;
	SharedPreferences mPref;
	
	/* main stuff */
	PackageNameAdapter mPkgAdapter;
	ListView mListView;
	
	/* app dialog stuff */
	AppAdapter dAdapter;
	Dialog dDialog;
	ProgressBar dProgressBar;
	ListView dListView;
	EditText dSearch;
	ImageButton dButton;
	
	@Override
	@SuppressWarnings("deprecation")
	@SuppressLint("WorldReadableFiles")
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPref = getSharedPreferences(Common.PREFERENCE_STATUSBAR_LAUNCHER_FILE, MODE_WORLD_READABLE);
		loadPinlist();
		initAppList();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem setting_item = menu.add(Menu.NONE, ID_ADD, 0, R.string.add);
		setting_item.setIcon(R.drawable.blacklist_add);
		setting_item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		return true;
	}
	
	@Override
	public void onPause() {
		super.onPause();
		// Avoid WindowLeaked Exception
		// http://publicstaticdroidmain.com/2012/01/avoiding-android-memory-leaks-part-1/
		if (dDialog != null && dDialog.isShowing()) {
			dDialog.dismiss();
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case ID_ADD:
			dDialog.show();
			break;
		}
		return false;
	}
	
	private void initAppList() {
		dDialog = new Dialog(this);
		dDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dDialog.setContentView(R.layout.dialog_blacklist);
		
		dProgressBar = (ProgressBar) dDialog.findViewById(R.id.progressBar1);
		dListView = (ListView) dDialog.findViewById(R.id.listView1);
		dSearch = (EditText) dDialog.findViewById(R.id.searchText);
		dButton = (ImageButton) dDialog.findViewById(R.id.searchButton);
		
		dAdapter = new AppAdapter(this, dProgressBar);
		dListView.setAdapter(dAdapter);
		dListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> v, View arg1, int pos, long arg3) {
				AppItem info = (AppItem) v.getItemAtPosition(pos);
				addApp(info.packageName);
				dDialog.dismiss();
			}
		});
		dButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dAdapter.getFilter().filter(dSearch.getText().toString());
			}
		});
		
		dAdapter.update();
	}
	
	private void loadPinlist() {
		mPkgAdapter = new PackageNameAdapter(this, getSetStrings()) {
			@Override
			public void onRemoveApp(String pkg) {
				removeApp(pkg);
			}
		};
		mListView = new ListView(this);
		mListView.setAdapter(mPkgAdapter);
		setContentView(mListView);
	}
	
	private Set<String> getSetStrings() {
		return mPref.getAll().keySet();
	}
	
	public void removeApp(String pkg) {
		mPref.edit().remove(pkg).commit();
		mPkgAdapter.update(getSetStrings());
		updateList();
	}
	
	public void addApp(String pkg) {
		mPref.edit().putBoolean(pkg, true).commit();
		updateList();
	}
	
	private void updateList() {
		mPkgAdapter.update(getSetStrings());
		sendBroadcast(new Intent(Common.STATUSBAR_TASKBAR_REFRESH));
	}
	
}
