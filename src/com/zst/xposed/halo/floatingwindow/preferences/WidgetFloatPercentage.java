package com.zst.xposed.halo.floatingwindow.preferences;

import com.zst.xposed.halo.floatingwindow.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class WidgetFloatPercentage extends DialogPreference implements
		SeekBar.OnSeekBarChangeListener {
	
	private TextView mFinalValue;
	private SeekBar mSeekBar;
	private TextView mValue;
	
	public Float mMin;
	public Float mMax;
	public Float mDefault;
	
	public WidgetFloatPercentage(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		setDialogLayoutResource(R.layout.pref_seekbar);
		mDefault = Float.parseFloat(attrs.getAttributeValue(null, "defaultValue"));
		mMin = Float.parseFloat(attrs.getAttributeValue(null, "minimum"));
		mMax = Float.parseFloat(attrs.getAttributeValue(null, "maximum"));
	}
	
	@Override
	protected void onBindView(View view) {
		super.onBindView(view);
		mFinalValue = new TextView(getContext());
		mFinalValue.setTextSize(24);
		LinearLayout layout = (LinearLayout)view;
		layout.addView(mFinalValue, layout.getChildCount());
		updatePercentage();
	}
	
	@Override
	protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
		builder.setNeutralButton(R.string.default_value, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});
	}
	
	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);
		
		mValue = (TextView) view.findViewById(R.id.value);
		mValue.setText("0%");
		mSeekBar = (SeekBar) view.findViewById(R.id.seekbar);
		mSeekBar.setOnSeekBarChangeListener(this);
	}
	
	@Override
	protected void showDialog(Bundle state) {
		super.showDialog(state);
		
		// can't use onPrepareDialogBuilder for this as we want the dialog
		// to be kept open on click
		AlertDialog d = (AlertDialog) getDialog();
		Button defaultsButton = d.getButton(DialogInterface.BUTTON_NEUTRAL);
		defaultsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				int progress = Math.round((mDefault * 100) - (mMin * 100));
				mSeekBar.setProgress(progress);
			}
		});
		
		final SharedPreferences prefs = getSharedPreferences();
		
		float value = prefs.getFloat(getKey(), mDefault);
		value -= mMin;
		int max = Math.round((mMax * 100) - (mMin * 100));
		mSeekBar.setMax(max);
		mSeekBar.setProgress(Math.round(value * 100));
		
	}
	
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);
		
		if (positiveResult) {
			int realValue = mSeekBar.getProgress() + Math.round((mMin * 100));
			Editor editor = getEditor();
			editor.putFloat(getKey(), (realValue * 0.01f));
			editor.commit();
		}
		updatePercentage();
	}
	
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		int realValue = progress + Math.round((mMin * 100));
		mValue.setText(realValue + "%");
	}
	
	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}
	
	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
	}
	
	private void updatePercentage() {
		float value = getSharedPreferences().getFloat(getKey(), mDefault);
		mFinalValue.setText(Math.round((value * 100)) + "%");
	}
}
