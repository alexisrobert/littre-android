package org.alexis.littre;

import org.alexis.libstardict.FileUtils;
import org.alexis.libstardict.IndexDB;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;

public class PreferencesActivity extends PreferenceActivity {
	private static PreferencesActivity context = null;
	
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
					dialog.setTitle(getString(R.string.moving_nosd_title));
					dialog.setMessage(getString(R.string.moving_nosd_message));
					dialog.setNeutralButton(getString(R.string.moving_nosd_button), null);
					dialog.show();
					return false;
				}
				
				
				MoveTask task = new MoveTask();
				task.execute((String)newValue);
				
				return true;
			}
		});
	}
	
	private static class MoveTask extends AsyncTask<String, Object, Boolean> {
		private ProgressDialog dialog = null;
		private String direction = null;
		
		@Override
		protected Boolean doInBackground(String... params) {
			direction = params[0];
			
			if (direction.equals("sdcard")) {
				return IndexDB.moveToSD(context);
			} else {
				return IndexDB.moveToInternal(context);
			}
		}
		
		@Override
		protected void onPreExecute() {
			dialog = new ProgressDialog(context);
			dialog.setTitle(context.getString(R.string.moving_progress_title));
			dialog.setMessage(context.getString(R.string.moving_progress_message));
			dialog.setCancelable(false);
			dialog.show();
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			dialog.dismiss();
			
			// If the thread didn't succeed, we don't want the preferences file to be
			// in an incoherent state.
			if (result == false) {
				Log.i("littre", "Moving failed ! Restoring old settings ...");
				if (direction.equals("sdcard"))
					context.getPreferenceScreen().getEditor().putString("index_sdcard", "internal").commit();
				else
					context.getPreferenceScreen().getEditor().putString("index_sdcard", "sdcard").commit();
			}
		}
	}
}
