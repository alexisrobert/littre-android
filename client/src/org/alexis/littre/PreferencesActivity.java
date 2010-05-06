package org.alexis.littre;

import org.alexis.libstardict.IndexDB;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;

public class PreferencesActivity extends PreferenceActivity {
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		findPreference("index_sdcard").setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				
				if (((String)newValue).equals("sdcard")) {
					return IndexDB.moveToSD(getApplicationContext());
				} else {
					return IndexDB.moveToInternal(getApplicationContext());
				}
			}
		});
	}
}
