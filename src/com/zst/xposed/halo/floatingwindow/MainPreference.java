package com.zst.xposed.halo.floatingwindow;

import java.util.ArrayList;

import com.zst.xposed.halo.floatingwindow.preferences.MainFragment;
import com.zst.xposed.halo.floatingwindow.preferences.MovableFragment;

import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.widget.ArrayAdapter;

public class MainPreference extends PreferenceActivity implements OnNavigationListener {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		ArrayList<String> itemList = new ArrayList<String>();
		itemList.add (getResources().getString(R.string.pref_main_top_title));
		itemList.add(getResources().getString(R.string.pref_movable_top_title));
		itemList.add(getResources().getString(R.string.pref_testing_top_title));
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, android.R.id.text1, itemList);
		
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
	    actionBar.setListNavigationCallbacks(adapter, this);
	    
	    getFragmentManager().beginTransaction().replace(android.R.id.content,
				MainFragment.getInstance()).addToBackStack(null).commit();
	}

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		getActionBar().setSelectedNavigationItem(itemPosition);
		switch (itemPosition) {
		case 0:
			getFragmentManager().beginTransaction().replace(android.R.id.content,
					MainFragment.getInstance()).commit();
			break;
		case 1:
			getFragmentManager().beginTransaction().replace(android.R.id.content,
					MovableFragment.getInstance()).commit();
			break;
		case 2:
			getFragmentManager().beginTransaction().replace(android.R.id.content,
					TestingActivity.getInstance()).commit();
			break;
		}
		return false;
	}
	
	@Override
	public void onBackPressed() {
	    int fragments = getFragmentManager().getBackStackEntryCount();
	    if (fragments == 1) { 
	        finish();
	    }
	    // http://stackoverflow.com/questions/20340303/
	    super.onBackPressed();
	}
}
