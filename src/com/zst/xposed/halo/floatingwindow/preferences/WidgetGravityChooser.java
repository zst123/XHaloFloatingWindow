package com.zst.xposed.halo.floatingwindow.preferences;

import java.util.LinkedList;
import java.util.List;

import com.zst.xposed.halo.floatingwindow.R;
import com.zst.xposed.halo.floatingwindow.helpers.Util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

public class WidgetGravityChooser extends DialogPreference {
	
	final static int COLOR_SELECTED = 0xffaaaaaa;
	final static int COLOR_UNSELECTED = 0xff444444;
	
	SharedPreferences mPref;
	int mOldGravity;
	int mNewGravity = -1;
	
	List<Button> mButtons;
	
	public WidgetGravityChooser(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
		super.onPrepareDialogBuilder(builder);
		builder.setTitle(getNameFromPosition(getPositionFromGravity(mOldGravity)));
	}
	
	@Override
	protected View onCreateDialogView() {
		mPref = getPreferenceManager().getSharedPreferences();
		mOldGravity = mPref.getInt(getKey(), Gravity.CENTER);
		
		mButtons = new LinkedList<Button>();
		
		LinearLayout holder = new LinearLayout(getContext());
		holder.setGravity(Gravity.CENTER_HORIZONTAL);
		holder.setOrientation(LinearLayout.VERTICAL);
		
		for (int x = 0; x < 3; x++) {
			LinearLayout line = new LinearLayout(getContext());
			line.setOrientation(LinearLayout.HORIZONTAL);
			line.setLayoutParams(new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.WRAP_CONTENT,
					LinearLayout.LayoutParams.WRAP_CONTENT));
			for (int y = 1; y <= 3; y++) {
				Button b = createButton((x * 3) + y);
				mButtons.add(b);
				line.addView(b);
			}
			holder.addView(line);
		}
		
		ScrollView sv = new ScrollView(getContext());
		sv.addView(holder);
		return sv;
	}
	
	private Button createButton(final int position) {
		boolean selected = (getGravityFromPosition(position) == mOldGravity);
		Button button = new Button(getContext()) {
			@Override
			public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
				super.onMeasure(heightMeasureSpec, heightMeasureSpec);
			}
		};
		button.setBackgroundColor(selected ? COLOR_SELECTED : COLOR_UNSELECTED);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				for (Button bs : mButtons) {
					bs.setBackgroundColor(COLOR_UNSELECTED);
				}
				v.setBackgroundColor(COLOR_SELECTED);
				mNewGravity = getGravityFromPosition(position);
				getDialog().setTitle(getNameFromPosition(position));
				
			}
		});
		button.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					v.setAlpha(0.8f);
					break;
				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_CANCEL:
					v.setAlpha(1f);
					break;
				}
				return false;
			}
		});
		
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				Util.dp(48, getContext()),
				Util.dp(48, getContext()), 1);
		int margin = Util.dp(4, getContext());
		params.setMargins(margin, margin, margin, margin);
		button.setLayoutParams(params);
		
		return button;
	}
	
	private int getGravityFromPosition(int position) {
		switch (position) {
		case 1:
			return Gravity.TOP | Gravity.LEFT;
		case 2:
			return Gravity.TOP | Gravity.CENTER_HORIZONTAL;
		case 3:
			return Gravity.TOP | Gravity.RIGHT;
		case 4:
			return Gravity.LEFT | Gravity.CENTER_VERTICAL;
		case 5:
			return Gravity.CENTER;
		case 6:
			return Gravity.RIGHT | Gravity.CENTER_VERTICAL;
		case 7:
			return Gravity.BOTTOM | Gravity.LEFT;
		case 8:
			return Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
		case 9:
			return Gravity.BOTTOM | Gravity.RIGHT;
		default:
			return Gravity.CENTER;
		}
	}
	
	private int getPositionFromGravity(int gravity) {
		switch (gravity) {
		case Gravity.TOP | Gravity.LEFT:
			return 1;
		case Gravity.TOP | Gravity.CENTER_HORIZONTAL:
			return 2;
		case Gravity.TOP | Gravity.RIGHT:
			return 3;
		case Gravity.LEFT | Gravity.CENTER_VERTICAL:
			return 4;
		case Gravity.CENTER:
			return 5;
		case Gravity.RIGHT | Gravity.CENTER_VERTICAL:
			return 6;
		case Gravity.BOTTOM | Gravity.LEFT:
			return 7;
		case Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL:
			return 7;
		case Gravity.BOTTOM | Gravity.RIGHT:
			return 9;
		}
		return 0;
	}
	
	private String getNameFromPosition(int gravity) {
		switch (gravity) {
		case 1:
			return getContext().getResources().getString(R.string.option_gravity_1);
		case 2:
			return getContext().getResources().getString(R.string.option_gravity_2);
		case 3:
			return getContext().getResources().getString(R.string.option_gravity_3);
		case 4:
			return getContext().getResources().getString(R.string.option_gravity_4);
		case 5:
			return getContext().getResources().getString(R.string.option_gravity_5);
		case 6:
			return getContext().getResources().getString(R.string.option_gravity_6);
		case 7:
			return getContext().getResources().getString(R.string.option_gravity_7);
		case 8:
			return getContext().getResources().getString(R.string.option_gravity_8);
		case 9:
			return getContext().getResources().getString(R.string.option_gravity_9);
		}
		return "";
	}
	
	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);
	}
	
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);
		if (positiveResult && mNewGravity != -1) {
			mPref.edit().putInt(getKey(), mNewGravity).commit();
		}
	}
}