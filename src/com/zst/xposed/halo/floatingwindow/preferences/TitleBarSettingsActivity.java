package com.zst.xposed.halo.floatingwindow.preferences;

import com.zst.xposed.halo.floatingwindow.Common;
import com.zst.xposed.halo.floatingwindow.R;
import com.zst.xposed.halo.floatingwindow.helpers.Util;
import android.preference.PreferenceFragment;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

@SuppressWarnings("deprecation")
@SuppressLint("WorldReadableFiles")
public class TitleBarSettingsActivity extends Activity implements OnSharedPreferenceChangeListener {
	public static final int TITLEBAR_ICON_NONE = 0;
	public static final int TITLEBAR_ICON_ORIGINAL = 1;
	public static final int TITLEBAR_ICON_BachMinuetInG = 2;
	public static final int TITLEBAR_ICON_DEFAULT = TITLEBAR_ICON_BachMinuetInG;
	
	SharedPreferences mPref;
	Resources mResource;
	int mIconType;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mResource = getResources();
		setContentView(R.layout.dialog_titlebar_icon_chooser);
		
		FragmentPagerAdapter adapter = new FragmentPagerAdapter(getFragmentManager()) {
			//http://stackoverflow.com/questions/12581896/fragmentpageradapter-getitem-is-not-called
			@Override
			public int getCount() {
				return 3;
			}
			@Override
			public String getPageTitle(int pos) {
				switch (pos) {
				case 0:
					return mResource.getString(R.string.tbic_theme);
				case 1:
					return mResource.getString(R.string.tbic_functionality);
				case 2:
					return mResource.getString(R.string.tbic_other_settings);
				}
				return "";
			}
			@Override
			public Fragment getItem(int position) {
				switch (position) {
				case 0:
					return new Fragment() {
						@Override
						public View onCreateView(LayoutInflater inflater, ViewGroup c,
								Bundle savedInstanceState) {
							ListView lv = new ListView(getContext());
							final ThemeItemAdapter adapter = new ThemeItemAdapter(getContext());
							
							adapter.add(new ThemeItem(mResource,
									R.string.tbic_theme_none_t,
									R.string.tbic_theme_none_s, TITLEBAR_ICON_NONE));
							adapter.add(new ThemeItem(mResource,
									R.string.tbic_theme_original_t,
									R.string.tbic_theme_original_s, TITLEBAR_ICON_ORIGINAL));
							adapter.add(new ThemeItem(mResource,
									R.string.tbic_theme_clearer_t,
									R.string.tbic_theme_clearer_s, TITLEBAR_ICON_BachMinuetInG));
							adapter.mSelectedId = mIconType;
							
							lv.setAdapter(adapter);
							lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
								@Override
								public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
									adapter.mSelectedId = pos;
									adapter.notifyDataSetChanged();
									mPref.edit().putInt(Common.KEY_WINDOW_TITLEBAR_ICON_TYPE, pos).commit();
								}
							});
							return lv;
						}
					};
				case 1:
					return new PreferenceFragment() {
						@Override
						public void onCreate(Bundle savedInstanceState) {
							super.onCreate(savedInstanceState);
							getPreferenceManager().setSharedPreferencesName(Common.PREFERENCE_MAIN_FILE);
							getPreferenceManager().setSharedPreferencesMode(PreferenceActivity.MODE_WORLD_READABLE);
							addPreferencesFromResource(R.xml.pref_movable_titlebar_function);
						}
					};
				case 2:
					return new PreferenceFragment() {
						@Override
						public void onCreate(Bundle savedInstanceState) {
							super.onCreate(savedInstanceState);
							getPreferenceManager().setSharedPreferencesName(Common.PREFERENCE_MAIN_FILE);
							getPreferenceManager().setSharedPreferencesMode(PreferenceActivity.MODE_WORLD_READABLE);
							addPreferencesFromResource(R.xml.pref_movable_titlebar_others);
						}
					};
				}
				
				return new Fragment();
			}
		};
		
		ViewPager vp = (ViewPager) findViewById(R.id.view_pager);
		vp.setAdapter(adapter);
		
		PagerTabStrip pts = (PagerTabStrip) findViewById(R.id.pager_title_strip);
		pts.setTabIndicatorColor(0xFF333333);
		pts.setTextColor(0xFF333333);
		pts.setBackgroundColor(Color.TRANSPARENT);
		
		init();
	}
	
	private Context getContext() {
		return this;
	}
	
	// Foreground Titlebar Views
	TextView tbAppTitle;
	ImageButton tbCloseButton;
	ImageButton tbMaxButton;
	ImageButton tbMinButton;
	ImageButton tbMoreButton;
	View tbDivider;
	// Foreground Actionbar Views
	ImageView abAppIcon;
	TextView abAppTitle;
	ImageView abOverflowButton;
	// Background
	View abBackground;
	View tbBackground;
	
	private void init() {
		mPref = getSharedPreferences(Common.PREFERENCE_MAIN_FILE, MODE_WORLD_READABLE);
		
		tbAppTitle = (TextView) findViewById(R.id.movable_titlebar_appname);
		tbCloseButton = (ImageButton) findViewById(R.id.movable_titlebar_close);
		tbMaxButton = (ImageButton) findViewById(R.id.movable_titlebar_max);
		tbMinButton = (ImageButton) findViewById(R.id.movable_titlebar_min);
		tbMoreButton = (ImageButton) findViewById(R.id.movable_titlebar_more);
		tbDivider = findViewById(R.id.movable_titlebar_line);
		
		abAppIcon = (ImageView) findViewById(android.R.id.button1);
		abAppTitle = (TextView) findViewById(android.R.id.candidatesArea);
		abOverflowButton = (ImageView) findViewById(android.R.id.button2);

		abBackground = findViewById(android.R.id.background);
		tbBackground = findViewById(R.id.movable_titlebar);
		
		updatePref();
	}
	
	private void updatePref() {
		int tbHeight = Util.realDp(mPref.getInt(Common.KEY_WINDOW_TITLEBAR_SIZE,
				Common.DEFAULT_WINDOW_TITLEBAR_SIZE), getContext());
		((LinearLayout.LayoutParams) tbBackground.getLayoutParams()).height = tbHeight;
		
		int tbDividerHei = Util.realDp(mPref.getInt(Common.KEY_WINDOW_TITLEBAR_SEPARATOR_SIZE,
				Common.DEFAULT_WINDOW_TITLEBAR_SEPARATOR_SIZE), getContext());
		if (mPref.getBoolean(Common.KEY_WINDOW_TITLEBAR_SEPARATOR_ENABLED,
				Common.DEFAULT_WINDOW_TITLEBAR_SEPARATOR_ENABLED)) {
			((RelativeLayout.LayoutParams) tbDivider.getLayoutParams()).height = tbDividerHei;
			int tbDividerCol = Color.parseColor("#" + mPref.getString(Common.KEY_WINDOW_TITLEBAR_SEPARATOR_COLOR,
					Common.DEFAULT_WINDOW_TITLEBAR_SEPARATOR_COLOR));
			tbDivider.setBackgroundColor(tbDividerCol);
		} else {
			((RelativeLayout.LayoutParams) tbDivider.getLayoutParams()).height = 0;
		}
		
		mIconType = mPref.getInt(Common.KEY_WINDOW_TITLEBAR_ICON_TYPE,
				Common.DEFAULT_WINDOW_TITLEBAR_ICONS_TYPE);
				
		switch (mIconType) {
		case TITLEBAR_ICON_NONE:
			((LinearLayout.LayoutParams) tbBackground.getLayoutParams()).height = 0;
			break;
		case TITLEBAR_ICON_ORIGINAL:
			tbCloseButton.setImageDrawable(mResource.getDrawable(R.drawable.movable_title_close_old));
			tbMaxButton.setImageDrawable(mResource.getDrawable(R.drawable.movable_title_max_old));
			tbMinButton.setImageDrawable(mResource.getDrawable(R.drawable.movable_title_min_old));
			tbMoreButton.setImageDrawable(mResource.getDrawable(R.drawable.movable_title_more_old));
			break;
		case TITLEBAR_ICON_BachMinuetInG:
			tbCloseButton.setImageDrawable(mResource.getDrawable(R.drawable.movable_title_close));
			tbMaxButton.setImageDrawable(mResource.getDrawable(R.drawable.movable_title_max));
			tbMinButton.setImageDrawable(mResource.getDrawable(R.drawable.movable_title_min));
			tbMoreButton.setImageDrawable(mResource.getDrawable(R.drawable.movable_title_more));
			break;
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mPref.registerOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		mPref.unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		updatePref();
	}

	class ThemeItem {
		public final String title;
		public final String msg;
		public final int id;
		public ThemeItem(Resources r, int _title, int _msg, int _id) {
			title = r.getString(_title);
			msg = r.getString(_msg);
			id = _id;
		}
	}
	class ThemeItemAdapter extends ArrayAdapter<ThemeItem> {
		class ItemView extends LinearLayout {
			public TextView title;
			public TextView msg;
			public ItemView(Context context, LayoutInflater inflator) {
				super(context);
				inflator.inflate(R.layout.view_app_list, this);
				title = (TextView) findViewById(android.R.id.title);
				msg = (TextView) findViewById(android.R.id.message);
				findViewById(android.R.id.icon).setVisibility(View.GONE);
				
				int padding = Util.dp(8, context);
				setPadding(padding, padding, padding, padding);
			}
		}
		LayoutInflater mInflator;
		int mSelectedId;
		public ThemeItemAdapter(Context context) {
			super(context, 0);
			mInflator = (LayoutInflater) getContext().getSystemService(
					Context.LAYOUT_INFLATER_SERVICE);
		}
		public View getView(int position, View v, ViewGroup parent) {
			if (v == null) {
				v = new ItemView(getContext(), mInflator);
			}
			ItemView convertView = (ItemView) v;
			
			final ThemeItem item = getItem(position);
			if (item != null) {
				final String title = item.title;
				final boolean isSelected = item.id == mSelectedId;
				convertView.title.setText(!isSelected ? title :
					Html.fromHtml("<b><u>" + title + "</u></b>"));
				convertView.msg.setText(item.msg);
			}
			return convertView;
		}
	}
}