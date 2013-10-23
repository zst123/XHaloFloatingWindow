package com.zst.xposed.halo.floatingwindow;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

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
		
		Button kbmd = (Button)findViewById(R.id.KMODEb);
		kbmd.setText(getKbMode(pref.getInt(Res.KEY_KEYBOARD_MODE, Res.DEFAULT_KEYBOARD_MODE)));

		Button paus = (Button)findViewById(R.id.PAUSEb);
		paus.setText(pref.getBoolean(Res.KEY_APP_PAUSE, Res.DEFAULT_APP_PAUSE)? "Off":"On");
		
		Button paus1 = (Button)findViewById(R.id.lpnmButton);
		paus1.setText(pref.getBoolean(Res.KEY_LONGPRESS_INJECT, Res.DEFAULT_LONGPRESS_INJECT)? "On":"Off");

		Button move1 = (Button)findViewById(R.id.movable_setting_1);
		move1.setText(pref.getBoolean(Res.KEY_MOVABLE_WINDOW, Res.DEFAULT_MOVABLE_WINDOW)? "Enabled":"Disabled");
	}
	public void click(View v){
		String msg = "Enter decimal no. between 0.1 & 1 \n (Enter 0.76 for 76%) \n \n ";
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
		case R.id.ButtonGravity:
		showGravityDialog();
		break;
		}

	}
	
	
		public void openFloating(Intent intent){
			 intent.addFlags(Res.FLAG_FLOATING_WINDOW);
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
			try{
			Intent intent = new Intent(getPackageManager().getLaunchIntentForPackage("de.robv.android.xposed.installer"));
			openFloating(intent);
			}catch(Exception e){
				try{
					Intent intent = new Intent(Intent.ACTION_MAIN);
					intent.addCategory(Intent.CATEGORY_LAUNCHER);
					intent.setComponent(new ComponentName("de.robv.android.xposed.installer","de.robv.android.xposed.installer.XposedInstallerActivity"));
					openFloating(intent);
				}catch(Exception ee){
				Toast t = Toast.makeText(this, "Xposed Installer isn't found", Toast.LENGTH_SHORT);
				t.setGravity(Gravity.CENTER, 0, 0);
				t.show();
				}
			}
		}
		if (item.getTitle().equals("About")){
			Intent intent = new Intent(this,AboutActivity.class);
			startActivity(intent);
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
			Intent intent = new Intent(this,AboutActivity.class);
			intent.putExtra("d","d");
			startActivity(intent);
		}else if(v.getId() ==R.id.button2){
			Intent intent = new Intent(this,AboutActivity.class);
			startActivity(intent);
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
        debug.setTextSize(7f);
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
	
	public void showGravityDialog(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this); 
	    final AlertDialog dialog = builder.create();
	    dialog.setTitle("Changing Gravity");
	    dialog.setView(this.getLayoutInflater().inflate(R.layout.dialog_gravity, null));
	    dialog.show();
	    SharedPreferences pref = getApplicationContext().getSharedPreferences(
				 Res.MY_PACKAGE_NAME, MODE_WORLD_READABLE);
	    int flags = pref.getInt(Res.KEY_GRAVITY, Res.DEFAULT_GRAVITY);
        int id = R.id.RadioButton5;
	    if (flags == (Gravity.TOP | Gravity.LEFT)){
	    	id = R.id.RadioButton1;
	    }else if(flags == (Gravity.TOP | Gravity.RIGHT)){
	    	id = R.id.radioButton3;
	    }else if(flags == (Gravity.CENTER)){
	    	id = R.id.RadioButton5;
	    }else if(flags == (Gravity.BOTTOM | Gravity.LEFT)){
	    	id = R.id.RadioButton7;
	    }else if(flags == (Gravity.BOTTOM | Gravity.RIGHT)){
	    	id = R.id.RadioButton9;
	    }else if(flags == (Gravity.TOP | Gravity.CENTER_HORIZONTAL)){
	    	id = R.id.radioButton2;
	    }else if(flags == (Gravity.LEFT | Gravity.CENTER_VERTICAL)){
	    	id = R.id.radioButton4;
	    }else if(flags == (Gravity.RIGHT | Gravity.CENTER_VERTICAL)){
	    	id = R.id.radioButton6;
	    }else if(flags == (Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL)){
	    	id = R.id.radioButton8;
	    }
	    
       ((RadioButton)dialog.findViewById(id)).setChecked(true);;
        

        
        OnClickListener listener  = new  OnClickListener() {
            @Override
            public void onClick(View v) {
            	if(v.getId() ==R.id.dialog_save){
            		int id =((RadioGroup)dialog.findViewById(R.id.radioSex)).getCheckedRadioButtonId();
            		int ff = Gravity.CENTER;
            		if (id == R.id.RadioButton1){
            			ff = (Gravity.TOP | Gravity.LEFT);
            	    }else if(id == R.id.radioButton3){
            	    	ff = (Gravity.TOP | Gravity.RIGHT);
            	    }else if(id == R.id.RadioButton5){
            	    	ff = (Gravity.CENTER);
            	    }else if(id == R.id.RadioButton7){
            	    	ff = (Gravity.BOTTOM | Gravity.LEFT);
            	    }else if( id == R.id.RadioButton9){
            	    	ff = (Gravity.BOTTOM | Gravity.RIGHT);
            	    }else if(id == R.id.radioButton2){
            	    	ff = (Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            	    }else if(id == R.id.radioButton4){
            	    	ff  = (Gravity.LEFT | Gravity.CENTER_VERTICAL);
            	    }else if(id == R.id.radioButton6){
                	    ff = (Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            	    }else if(id == R.id.radioButton8){
            	    	ff = (Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL); 
            	    }
                     setPref(Res.KEY_GRAVITY, ff);
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
					 Res.MY_PACKAGE_NAME, MODE_WORLD_WRITEABLE);
			Editor editor = pref.edit();
			editor.remove(id);
			 editor.commit();  
           init();
	}
	public void setPref(String id, Float value){
		SharedPreferences pref = getApplicationContext().getSharedPreferences(
				 Res.MY_PACKAGE_NAME, MODE_WORLD_WRITEABLE);
		Editor editor = pref.edit();
        editor.putFloat(id, value) ; 
		 editor.commit();
         init();

	}
	public void setPref(String id, int value){
		SharedPreferences pref = getApplicationContext().getSharedPreferences(
				 Res.MY_PACKAGE_NAME, MODE_WORLD_WRITEABLE);
		Editor editor = pref.edit();
        editor.putInt(id, value) ; 
		 editor.commit();
         init();

	}
	public void kbclick(View v){
		
		SharedPreferences pref = getApplicationContext().getSharedPreferences(
				 Res.MY_PACKAGE_NAME, MODE_WORLD_WRITEABLE);
		int u =pref.getInt(Res.KEY_KEYBOARD_MODE, Res.DEFAULT_KEYBOARD_MODE);
		if (u==3){
			u = 0;
		}
		u++;
		Editor editor = pref.edit();
       editor.putInt(Res.KEY_KEYBOARD_MODE, u) ; 
		 editor.commit();
        init();
	}
	public String getKbMode(int i){
		if (i ==2){
			return "Pan up";
		}else if (i == 3){
			return "Scale";
		}else{
			return "Default";
		}
	}
	public void bap(View v){
		SharedPreferences pref = getApplicationContext().getSharedPreferences(
				 Res.MY_PACKAGE_NAME, MODE_WORLD_WRITEABLE);
		boolean b =pref.getBoolean(Res.KEY_APP_PAUSE, Res.DEFAULT_APP_PAUSE);
		
		Editor editor = pref.edit();
      editor.putBoolean(Res.KEY_APP_PAUSE, (!b)) ; 
		 editor.commit();
       init();
	}
	public void halo_toggle(View v){
	    LinearLayout s = (LinearLayout)findViewById(R.id.halo_setting_buttons);
	   if( s.getVisibility() == View.VISIBLE){
		   s.setVisibility(View.GONE);
	   }else{
	    s.setVisibility(View.VISIBLE);
	   }
	}
	public void systemui_toggle(View v){
	    LinearLayout s = (LinearLayout)findViewById(R.id.systemui_setting_buttons);
	   if( s.getVisibility() == View.VISIBLE){
		   s.setVisibility(View.GONE);
	   }else{
	    s.setVisibility(View.VISIBLE);
	   }
	}
	public void movable_toggle(View v){
	    LinearLayout s = (LinearLayout)findViewById(R.id.movable_buttons);
	   if( s.getVisibility() == View.VISIBLE){
		   s.setVisibility(View.GONE);
	   }else{
	    s.setVisibility(View.VISIBLE);
	   }
	}
	public void lmpn(View v){
		SharedPreferences pref = getApplicationContext().getSharedPreferences(
				 Res.MY_PACKAGE_NAME, MODE_WORLD_WRITEABLE);
		boolean b =pref.getBoolean(Res.KEY_LONGPRESS_INJECT, Res.DEFAULT_LONGPRESS_INJECT);
		
		Editor editor = pref.edit();
      editor.putBoolean(Res.KEY_LONGPRESS_INJECT, (!b)) ; 
		 editor.commit();
       init();
       Toast t = Toast.makeText(this, "Please restart SystemUI or reboot", Toast.LENGTH_SHORT);
		t.setGravity(Gravity.CENTER, 0, 0);
		t.show();
	}
	public void movable_one_setting(View v){
		SharedPreferences pref = getApplicationContext().getSharedPreferences(
				Res.MY_PACKAGE_NAME, MODE_WORLD_WRITEABLE);
		boolean b =pref.getBoolean(Res.KEY_MOVABLE_WINDOW, Res.DEFAULT_MOVABLE_WINDOW);	
		Editor editor = pref.edit();
		editor.putBoolean(Res.KEY_MOVABLE_WINDOW, (!b)); 
		editor.commit();
		init();
	}
}

