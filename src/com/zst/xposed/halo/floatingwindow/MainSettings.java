package com.zst.xposed.halo.floatingwindow;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainSettings extends Activity {
boolean sv = true;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_settings);
		init();
	}
	public void init(){
		 SharedPreferences pref = getApplicationContext().getSharedPreferences(
				 Res.MY_PACKAGE_NAME, MODE_WORLD_READABLE);
		Button landscapeWidth = (Button)findViewById(R.id.LWIDTHb);
		landscapeWidth.setText(""+pref.getFloat(Res.KEY_LANDSCAPE_WIDTH, Res.DEFAULT_LANDSCAPE_WIDTH));

		Button landscapeHeight = (Button)findViewById(R.id.LHEIGHTb);
		landscapeHeight.setText(""+pref.getFloat(Res.KEY_LANDSCAPE_HEIGHT, Res.DEFAULT_LANDSCAPE_HEIGHT));

		Button portHgt = (Button)findViewById(R.id.PHEIGHTb);
		portHgt.setText(""+pref.getFloat(Res.KEY_PORTRAIT_HEIGHT, Res.DEFAULT_PORTRAIT_HEIGHT));

		Button pWdh = (Button)findViewById(R.id.PWIDTHb);
		pWdh.setText(""+pref.getFloat(Res.KEY_PORTRAIT_WIDTH, Res.DEFAULT_PORTRAIT_WIDTH));

		Button dimValue = (Button)findViewById(R.id.DIMb);
		dimValue.setText(""+pref.getFloat(Res.KEY_DIM, Res.DEFAULT_DIM));

		Button transparencyV = (Button)findViewById(R.id.TRANSb);
		transparencyV.setText(""+pref.getFloat(Res.KEY_ALPHA, Res.DEFAULT_ALPHA));

		Button light = (Button)findViewById(R.id.button1);
		Button dark = (Button)findViewById(R.id.button2);

	}
	public void click(View v){
		String msg = "Enter decimal no. between 0.1 & 1 \n (Enter 0.53 for 53%) \n \n ";
		String currentValue = ((Button)v).getText().toString();

		switch(v.getId()){
		case R.id.LWIDTHb:
			showDialog(Res.KEY_LANDSCAPE_WIDTH, msg + "Landscape Width:\n Width of window in landscape" , currentValue);
			break;
		case R.id.LHEIGHTb:
			showDialog(Res.KEY_LANDSCAPE_HEIGHT, msg + "Landscape Height:\n Height of window in landscape", currentValue);
			break;
		case R.id.PHEIGHTb:
			showDialog(Res.KEY_PORTRAIT_HEIGHT, msg + "Portrait Height:\n Height of window in portrait",currentValue);
			break;
		case R.id.PWIDTHb:
			showDialog(Res.KEY_PORTRAIT_WIDTH, msg + "Portrait Width:\n Width of window in portrait" ,currentValue);
			break;
		case R.id.DIMb:
			showDialog(Res.KEY_DIM, msg + "Dim Amount:\n Amount to dim background", currentValue);
			break;
		case R.id.TRANSb:
			showDialog(Res.KEY_ALPHA, msg + "Transparency:\n Transparency value of window", currentValue);
			break;
			
		}

	}
	
	
		public void openFloating(Intent intent){
			 intent.addFlags(HaloFloatingInject.FLAG_FLOATING_WINDOW);
	         intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_TASK_ON_HOME);
	         intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_SINGLE_TOP);
	         intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_CLEAR_TOP);
	         intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS); 
	         intent.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
	         intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	         intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
	         intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
	         intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
	         intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
			 startActivity(intent);

			// t.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		}
	@Override
		public boolean onMenuItemSelected(int featureId, MenuItem item) {
		if (item.getTitle().equals("Open Xposed Installer")){
			Intent intent = new Intent(getPackageManager().getLaunchIntentForPackage("de.robv.android.xposed.installer"));
			openFloating(intent);
		}
		if (item.getTitle().equals("About")){
			Intent intent = new Intent(this,AboutActivity.class);
			openFloating(intent);
		}
		if (item.getTitle().equals("Reset Preferences")){
			new AlertDialog.Builder(this)
	        .setIcon(android.R.drawable.ic_dialog_alert)
	        .setMessage("Confirm revert?")
	        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
	            @Override
	            public void onClick(DialogInterface dialog, int which) {
	            	SharedPreferences pref = getApplicationContext().getSharedPreferences(
	   					 Res.MY_PACKAGE_NAME, MODE_WORLD_READABLE);
	   			Editor editor = pref.edit();
	   	        editor.clear();
	   			 editor.commit();  
		            init();
	            }

	        })
	        .setNegativeButton("NO", null)
	        .show();

			
		}
		return super.onMenuItemSelected(featureId, item);
		}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add("Open Xposed Installer");
		menu.add("Reset Preferences");
		menu.add("About");

		return true;
	}
	
	public void testclick(View v){
		if(v.getId() ==R.id.button1){
			Intent intent = new Intent(this,AboutActivityWhite.class);
			openFloating(intent);
		}else if(v.getId() ==R.id.button2){
			Intent intent = new Intent(this,AboutActivity.class);
			openFloating(intent);
		}
		 
	}
	
	public void showDialog(final String prefKey, String message , String value){
		AlertDialog.Builder builder = new AlertDialog.Builder(this); 
	    final AlertDialog dialog = builder.create();
	    dialog.setTitle("Changing Preference");
	    dialog.setView(this.getLayoutInflater().inflate(R.layout.dialog_pref_changer, null));
	    dialog.show();
	    
	    TextView text = (TextView)dialog.findViewById(R.id.discriptionText);
        text.setText(message+"");
        
        TextView debug = (TextView)dialog.findViewById(R.id.debugINFO);
        debug.setText("Debug: " + prefKey);
        
        final EditText numb = (EditText)dialog.findViewById(R.id.valueBox);
        numb.setText(value);

        
        OnClickListener listener  = new  OnClickListener() {
            @Override
            public void onClick(View v) {
            	if(v.getId() ==R.id.dialog_save){
            		if(numb.getText().toString().equals("") | numb.getText().toString().equals(".")){
            			deletePrefs(prefKey);
         	            dialog.dismiss();
            			return;
            		}
                     Float floatz = Float.parseFloat(numb.getText().toString());
                     if (floatz > 1f) floatz = 1f;
                     if (floatz < 0.1f) floatz = 0.1f;
                     setPref(prefKey, floatz);
     	            dialog.dismiss();
        		}else if(v.getId() ==R.id.dialog_discard){
    	            dialog.dismiss();
        		}
            }
        };
		Button save = (Button)dialog.findViewById(R.id.dialog_save);
		Button discard = (Button)dialog.findViewById(R.id.dialog_discard);
		save.setOnClickListener(listener);
		discard.setOnClickListener(listener);
	}
	public void deletePrefs(String id){
		SharedPreferences pref = getApplicationContext().getSharedPreferences(
					 Res.MY_PACKAGE_NAME, MODE_WORLD_READABLE);
			Editor editor = pref.edit();
			editor.remove(id);
			 editor.commit();  
           init();
	}
	public void setPref(String id, Float value){
		SharedPreferences pref = getApplicationContext().getSharedPreferences(
				 Res.MY_PACKAGE_NAME, MODE_WORLD_READABLE);
		Editor editor = pref.edit();
        editor.putFloat(id, value) ; 
		 editor.commit();
         init();

	}
}

