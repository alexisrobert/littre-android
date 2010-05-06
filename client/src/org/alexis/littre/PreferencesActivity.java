package org.alexis.littre;

import org.alexis.libstardict.FileUtils;
import org.alexis.libstardict.IndexDB;

import android.app.AlertDialog;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;

public class PreferencesActivity extends PreferenceActivity {
	private PreferencesActivity context = null;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		context = this; // Do you have a cleaner way to access the context from listeners ?
		
		findPreference("index_sdcard").setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				
				if (!FileUtils.isSDMounted()) {
					AlertDialog.Builder dialog = new AlertDialog.Builder(context);
					dialog.setTitle("Error");
					dialog.setMessage("You must have an sdcard to change this parameter.");
					dialog.setNeutralButton("Ok", null);
					dialog.show();
					return false;
				}
				
				
				if (((String)newValue).equals("sdcard")) {
					return IndexDB.moveToSD(getApplicationContext());
				} else {
					return IndexDB.moveToInternal(getApplicationContext());
				}
			}
		});
	}
}
