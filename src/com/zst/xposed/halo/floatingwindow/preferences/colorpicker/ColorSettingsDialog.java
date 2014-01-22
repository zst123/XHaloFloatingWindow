package com.zst.xposed.halo.floatingwindow.preferences.colorpicker;

import java.util.Locale;

import com.zst.xposed.halo.floatingwindow.R;
import com.zst.xposed.halo.floatingwindow.preferences.colorpicker.ColorPickerView.OnColorChangedListener;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class ColorSettingsDialog extends AlertDialog implements
		ColorPickerView.OnColorChangedListener {

	private ColorPickerView mColorPicker;

	private ColorPanelView mOldColor;
	private ColorPanelView mNewColor;
	private EditText mHexColor;
	private Button mHexButton;

	private LayoutInflater mInflater;
	private OnColorChangedListener mListener;

	public ColorSettingsDialog(Context context, int initialColor, final String defColor) {
		super(context);
		getWindow().setFormat(PixelFormat.RGBA_8888);
		// To fight color banding.
		setUp(initialColor, defColor);
	}

	private void setUp(int color, final String defColor) {
        mInflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View layout = mInflater.inflate(R.layout.dialog_colorpicker, null);

        mColorPicker = (ColorPickerView) layout.findViewById(R.id.color_picker_view);
        mOldColor = (ColorPanelView) layout.findViewById(R.id.old_color_panel);
        mNewColor = (ColorPanelView) layout.findViewById(R.id.new_color_panel);
        mHexColor = (EditText) layout.findViewById(R.id.current_hex_text);

        mColorPicker.setOnColorChangedListener(this);
        mOldColor.setColor(color);
        mColorPicker.setColor(color, true);

        mHexButton = (Button) layout.findViewById(R.id.color_apply);
        mHexButton.setOnClickListener(new View.OnClickListener(){
        	@Override
        	public void onClick(View v) {
        		try{
        			int color = Color.parseColor("#" + mHexColor.getText().toString());
        			mColorPicker.setColor(color);
        			colorChange(color);
        		}catch(Exception e){
        			mHexColor.setText("FFFFFF");
        		}
        	}
        });

        setView(layout);
    }

	@Override
	public void onColorChanged(int color) {
		colorChange(color);
	}

	private void colorChange(int color){
		mNewColor.setColor(color);
		mHexColor.setText(Integer.toHexString(color).toUpperCase(Locale.ENGLISH).substring(2));
		if (mListener != null) {
			mListener.onColorChanged(color);
		}
	}

	public void setAlphaSliderVisible(boolean visible) {
		mColorPicker.setAlphaSliderVisible(visible);
	}

	public String getColorString() {
		return Integer.toHexString(mColorPicker.getColor());
	}

	public int getColor() {
		return mColorPicker.getColor();
	}

}