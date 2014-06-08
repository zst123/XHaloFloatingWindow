package com.zst.xposed.halo.floatingwindow.preferences.adapters;

import java.util.LinkedList;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public abstract class PageAdapter extends FragmentPagerAdapter {
	final LinkedList<Integer> mFragmentPages;
	
	public PageAdapter(FragmentManager fm, LinkedList<Integer> fragmentPages) {
		super(fm);
		mFragmentPages = fragmentPages;
	}
	
	@Override
	public Fragment getItem(int position) {
		return new PageFragment() {
			@Override
			public View onCreateView(LayoutInflater inflater, ViewGroup c,
					Bundle savedInstanceState) {
				return makeView(inflater, c, getPosition());
			}
		}.setPosition(mFragmentPages.get(position));
	}
	
	public abstract View makeView(LayoutInflater inflater, ViewGroup container, int position);

	@Override
	public int getCount() {
		return mFragmentPages.size();
	}
	
	
	public static abstract class PageFragment extends Fragment {
		public static final String FRAGMENT_KEY_POSITION = "position";
		
		public PageFragment setPosition(int position) {
			// http://stackoverflow.com/questions/9245408
			Bundle args = new Bundle();
			args.putInt(FRAGMENT_KEY_POSITION, position);
			setArguments(args);
			return this;
		}
		
		public int getPosition() {
			return getArguments().getInt(FRAGMENT_KEY_POSITION, -1);
		}
	}
}