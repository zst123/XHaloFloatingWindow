package com.zst.xposed.halo.floatingwindow.preferences.adapters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import com.zst.xposed.halo.floatingwindow.R;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

public class AppAdapter extends BaseAdapter implements Filterable {
	
	final View mProgressBar;
	final Context mContext;
	final Handler mHandler;
	final PackageManager mPackageManager;
	final LayoutInflater mLayoutInflater;
	
	protected List<PackageInfo> mInstalledAppInfo;
	protected List<AppItem> mInstalledApps = new LinkedList<AppItem>();
	protected List<PackageInfo> mTemporarylist;
	
	// temp. list holding the filtered items
	
	public AppAdapter(Context context, View progress_bar) {
		mProgressBar = progress_bar;
		mContext = context;
		mHandler = new Handler();
		mPackageManager = mContext.getPackageManager();
		mLayoutInflater = (LayoutInflater) mContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mInstalledAppInfo = mPackageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS);
		mTemporarylist = mInstalledAppInfo;
		update();
	}
	
	public synchronized void update() {
		if (isUpdating == true) {
			return;
		}
		toggleProgressBarVisible(true);
		new Thread(new Runnable() {
			@Override
			public void run() {
					isUpdating = true;
					final List<AppItem> temp = new LinkedList<AppItem>(); 
					for (PackageInfo info : mTemporarylist) {
						final AppItem item = new AppItem();
						item.title = info.applicationInfo.loadLabel(mPackageManager);
						item.icon = info.applicationInfo.loadIcon(mPackageManager);
						item.packageName = info.packageName;
						final int index = Collections.binarySearch(temp, item);
						if (index < 0) {
							temp.add((-index - 1), item);
						} else {
							temp.add((index + 1), item);
						}
					}
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							mInstalledApps = temp;
							notifyDataSetChanged();
							isUpdating = false;
						}
					});					
					toggleProgressBarVisible(false);
			}
		}).start();
	}
	
	private void toggleProgressBarVisible(final boolean b) {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				if (mProgressBar != null) {
					mProgressBar.setVisibility(b ? View.VISIBLE : View.GONE);
				}
			}
		});
	}
	
	@Override
	public int getCount() {
		return mInstalledApps.size();
	}
	
	@Override
	public AppItem getItem(int position) {
		try {
			return mInstalledApps.get(position);
		}catch (Exception e){
			return new AppItem();
		}
	}
	
	@Override
	public long getItemId(int position) {
		return getItem(position).hashCode();
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView != null) {
			holder = (ViewHolder) convertView.getTag();
		} else {
			convertView = mLayoutInflater.inflate(R.layout.view_app_list, parent, false);
			holder = new ViewHolder();
			holder.name = (TextView) convertView.findViewById(android.R.id.title);
			holder.icon = (ImageView) convertView.findViewById(android.R.id.icon);
			holder.pkg = (TextView) convertView.findViewById(android.R.id.message);
			convertView.setTag(holder);
		}
		AppItem appInfo = getItem(position);
		
		holder.name.setText(appInfo.title);
		holder.pkg.setText(appInfo.packageName);
		holder.icon.setImageDrawable(appInfo.icon);
		return convertView;
	}
	
	boolean isFiltering;
	boolean isUpdating;
	@Override
	public Filter getFilter() {
		return new Filter() {
			@Override
			protected void publishResults(CharSequence constraint, FilterResults results) {
			}
			
			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				if (isFiltering || isUpdating) {
					return new FilterResults();
				}
				
				if (TextUtils.isEmpty(constraint)) {
					// No filter implemented we return all the list
					mTemporarylist = mInstalledAppInfo;
					update();
					return new FilterResults();
				}
				
				isFiltering = true;
				ArrayList<PackageInfo> FilteredList = new ArrayList<PackageInfo>();
				for (PackageInfo data : mInstalledAppInfo) {
					final String filterText = constraint.toString().toLowerCase(Locale.ENGLISH);
					try {
						if (data.applicationInfo.loadLabel(mPackageManager).toString()
								.toLowerCase(Locale.ENGLISH).contains(filterText)) {
							FilteredList.add(data);
						} else if (data.packageName.contains(filterText)) {
							FilteredList.add(data);
						}
					} catch (Exception e) {
					}
				}
				mTemporarylist = FilteredList;
				update();
				isFiltering = false;
				return new FilterResults();
			}
		};
	}
	
	public class AppItem implements Comparable<AppItem> {
		public CharSequence title;
		public String packageName;
		public Drawable icon;
		
		@Override
		public int compareTo(AppItem another) {
			return this.title.toString().compareTo(another.title.toString());
		}
	}
	
	static class ViewHolder {
		TextView name;
		ImageView icon;
		TextView pkg;
	}
}
