package com.zst.xposed.halo.floatingwindow.preferences.adapters;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.zst.xposed.halo.floatingwindow.R;
import com.zst.xposed.halo.floatingwindow.preferences.BlacklistActivity;
import com.zst.xposed.halo.floatingwindow.preferences.WhitelistActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class PackageNameAdapter extends BaseAdapter {
	
	final WhitelistActivity mWhite;
	final BlacklistActivity mBlack;
	final Handler mHandler;
	final PackageManager mPackageManager;
	final LayoutInflater mLayoutInflater;
	
	protected List<PackageInfo> mInstalledAppInfo;
	protected List<PackageItem> mApps = new LinkedList<PackageItem>();
	
	// temp. list holding the filtered items
	
	public PackageNameAdapter(BlacklistActivity act, Set<String> app_array) {
		mBlack = act;
		mWhite = null;
		mHandler = new Handler();
		mPackageManager = act.getBaseContext().getPackageManager();
		mLayoutInflater = (LayoutInflater) act.getBaseContext()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		update(app_array);
	}
	
	public PackageNameAdapter(WhitelistActivity act, Set<String> app_array) {
		mWhite = act;
		mBlack = null;
		mHandler = new Handler();
		mPackageManager = act.getBaseContext().getPackageManager();
		mLayoutInflater = (LayoutInflater) act.getBaseContext()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		update(app_array);
	}
	
	public void update(final Set<String> app_array) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				synchronized (mApps) {
					final List<PackageItem> temp = new LinkedList<PackageItem>();
					for (String pkg_name : app_array) {
						try {
						ApplicationInfo ai = mPackageManager.getApplicationInfo(pkg_name, 0);
						final PackageItem item = new PackageItem();
						item.title = ai.loadLabel(mPackageManager);
						item.icon =  ai.loadIcon(mPackageManager);
						item.packageName = ai.packageName;
						final int index = Collections.binarySearch(temp, item);
						if (index < 0)
							temp.add((-index - 1), item);
						} catch (Exception e) {				
						}
					}
					mApps.clear();
					mApps = temp;
					notifyDataSetChangedOnHandler();
				}
			}
		}).start();
	}
	
	private void notifyDataSetChangedOnHandler() {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				notifyDataSetChanged();
			}
		});
	}
	
	@Override
	public int getCount() {
		return mApps.size();
	}
	
	@Override
	public PackageItem getItem(int position) {
		return mApps.get(position);
	}
	
	@Override
	public long getItemId(int position) {
		return mApps.get(position).hashCode();
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView != null) {
			holder = (ViewHolder) convertView.getTag();
		} else {
			convertView = mLayoutInflater.inflate(R.layout.view_package_list, parent, false);
			holder = new ViewHolder();
			holder.name = (TextView) convertView.findViewById(android.R.id.title);
			holder.icon = (ImageView) convertView.findViewById(android.R.id.icon);
			holder.pkg = (TextView) convertView.findViewById(android.R.id.message);
			holder.remove = (ImageButton) convertView.findViewById(R.id.removeButton);
			convertView.setTag(holder);
		}
		final PackageItem appInfo = getItem(position);
		
		holder.name.setText(appInfo.title);
		holder.pkg.setText(appInfo.packageName);
		holder.icon.setImageDrawable(appInfo.icon);
		holder.icon.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
							Uri.fromParts("package", appInfo.packageName, null));
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					if (mWhite != null) {
						mWhite.startActivity(intent);
					}else{
						mBlack.startActivity(intent);
					}
				} catch (Exception e) {
					Activity mStarter;
					if (mWhite != null) {
						mStarter = mWhite;
					}else{
						mStarter = mBlack;
					}
					final String txt = mStarter.getResources().getString(R.string.pref_blacklist_error)
							+ appInfo.packageName + "\n" + e.toString();
					Toast.makeText(mStarter, txt, Toast.LENGTH_LONG).show();
					e.printStackTrace();
				}				
			}
		});
		holder.remove.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if (mWhite != null) {
					mWhite.removeApp(appInfo.packageName);;
				}else{
					mBlack.removeApp(appInfo.packageName);;
				}
			}
		});
		return convertView;
	}
	
	
	public class PackageItem implements Comparable<PackageItem> {
		public CharSequence title;
		public String packageName;
		public Drawable icon;
		
		@Override
		public int compareTo(PackageItem another) {
			return this.title.toString().compareTo(another.title.toString());
		}
	}
	
	static class ViewHolder {
		TextView name;
		ImageView icon;
		TextView pkg;
		ImageButton remove;
	}
}
