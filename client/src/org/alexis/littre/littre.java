/*
 * Littre dictionnary for Android
 * Copyright (C) 2009 Alexis ROBERT <alexis.robert@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, at version 3 of the License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.alexis.littre;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alexis.libstardict.IndexDB;
import org.alexis.libstardict.Preferences;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.AdapterView.OnItemClickListener;

public class littre extends Activity {
	static private final String[] MENUMAPPING_FROM = {"name","icon"};
	static private final int[] MENUMAPPING_TO = {R.id.menuname,R.id.menuicon};
	
	static final String INTENT_GET_HISTORY = "org.alexis.littre.GetHistory";
	
	private DownloadTask downtask = null;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
        
        setContentView(R.layout.welcome); // We need to have our ListView instancied to fill it!
        
        List<Map<String,String>> menulist = new ArrayList<Map<String,String>>();
        
        // Arggg... put this in a resource XML file! Or make them at-least final !
        addMenuItem(getString(R.string.home_alphabet), android.R.drawable.ic_menu_directions, menulist);
        addMenuItem(getString(R.string.home_search), android.R.drawable.ic_menu_search, menulist);
        addMenuItem(getString(R.string.home_history), android.R.drawable.ic_menu_recent_history, menulist);
        addMenuItem(getString(R.string.home_settings), android.R.drawable.ic_menu_preferences, menulist); // TODO: Put this in a REAL menu.
        
        SimpleAdapter adapter = new SimpleAdapter(this, menulist, R.layout.welcomeitem, MENUMAPPING_FROM, MENUMAPPING_TO);
        ((ListView)this.findViewById(R.id.welcomelist)).setAdapter(adapter);
        ((ListView)this.findViewById(R.id.welcomelist)).setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View v, int position,
					long id) {
				switch ((int)id) {
				case 0: // Alphabet
					Intent i1 = new Intent(null,null,getApplicationContext(), AlphabetActivity.class);
					startActivity(i1);
					break;
				case 1: // Search
					onSearchRequested();
					break;
				case 2: // History
					Intent i2 = new Intent(INTENT_GET_HISTORY, null, getApplicationContext(), HistoryActivity.class);
		        	startActivity(i2);
		        	break;
				case 3: // Settings
					Intent i3 = new Intent(null, null, getApplicationContext(), PreferencesActivity.class);
					startActivity(i3);
					break;
				}
			}
		});
        
        if (IndexDB.needsFilling(this) == true)
        	this.downloadIndex();
    }
    
    // I know, side-effects are dirty. But I don't see any macros in Java.
    // OK, I aknowledge, i talk a little bit too much in the comments.
    private void addMenuItem(String name, int icon, List<Map<String,String>> menulist) {
    	Map<String, String> map = new HashMap<String, String>();
    	
    	map.put("name", name);
    	map.put("icon", String.valueOf(icon));
    	
    	menulist.add(map);
    }
    
    /** INDEX DOWNLOADING PART **/
    
    private void downloadIndex() {
	    ConnectivityManager c = (ConnectivityManager)getApplicationContext().
	    							getSystemService(Context.CONNECTIVITY_SERVICE);
	    
		if (c.getActiveNetworkInfo() == null || c.getActiveNetworkInfo().isAvailable() == false) {
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setTitle(getString(R.string.no_network_title));
			alert.setMessage(getString(R.string.no_network_idx_message));
			alert.show();
			return;
		}
		
		ProgressDialog d = new ProgressDialog(this);
		d.setTitle(getString(R.string.downloading_title));
		d.setMessage(getString(R.string.downloading_message));
		d.setIndeterminate(false);
		
		d.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			
		d.setCancelable(false);
		d.show();
		
		downtask = (DownloadTask)getLastNonConfigurationInstance();
		if (downtask == null) {
			Log.i("libstardict", "First run, will download index ...");
			downtask = new DownloadTask();
		}
		
		downtask.activity = this;
		downtask.d = d;
		
		if(downtask.getStatus() == AsyncTask.Status.PENDING) {
			downtask.execute();
		}
	}
    
	// Download thread
	private static class DownloadTask extends AsyncTask<Object, Integer, Boolean> {
		public Activity activity = null;
		public ProgressDialog d = null;
		private PowerManager.WakeLock wl;
		
		@Override
		protected Boolean doInBackground(Object... arg0) {
			boolean downloadok = false;
			
			Log.d("libstardict","Beginning downloading index ...");
			
			/* We request a wake lock. We don't want unfinished downloads
			 * due to the phone going in suspend mode. */
			PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
			wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Littre index download");
			wl.acquire();
			
			try {
				URL url = new URL(activity.getString(R.string.indexurl));
				HttpURLConnection http = (HttpURLConnection) url.openConnection();
				http.connect();
				
				BufferedInputStream fis = new BufferedInputStream(http.getInputStream());
				FileOutputStream fos = new FileOutputStream(
						new File(Preferences.getIndexDir(activity),"XMLittre.idx.tmp"));
				
				byte[] bytes = new byte[20480];
				int progress = 0;
				int readBytes = 0;
				
				while ((readBytes = fis.read(bytes)) > 0) {
					progress += readBytes;
					publishProgress((int)(((float)progress/http.getContentLength())*100));
					fos.write(bytes, 0, readBytes);
				}
				
				fis.close();
				fos.close();
				
				/** Checking MD5 **/
				Log.i("littre", "Checking MD5 ...");
				publishProgress(-1); // -1 means MD5 computing. Yes. That's a hack.
				
				String origmd5 = downloadMD5();
				String ourmd5 = computeMD5();
				
				if (origmd5.equals(ourmd5) == true) {
					Log.i("littre", String.format("MD5 check ok : %s (remote) == %s (local)",
							origmd5,ourmd5));
					downloadok = true;
				} else {
					Log.i("littre", String.format("MD5 check error : %s (remote) != %s (local)",
							origmd5, ourmd5));
				}
			} catch (IOException e){
				e.printStackTrace();
			}
			
			if (downloadok == true) {
				new File(Preferences.getIndexDir(activity),"XMLittre.idx.tmp")
					.renameTo(Preferences.getIndexPath(activity));
				
				Log.d("libstardict", "Download finished!");
			} else {
				
				new File(Preferences.getIndexDir(activity),"XMLittre.idx.tmp").delete();
				Log.d("libstardict", "Error while downloading !");
			}
			
			return downloadok;
		}
		
		public String downloadMD5() throws IOException {
			URL url = new URL(activity.getString(R.string.md5url));
			HttpURLConnection http = (HttpURLConnection) url.openConnection();
			http.connect();
			
			BufferedInputStream fis = new BufferedInputStream(http.getInputStream());
			
			byte[] md5 = new byte[32];
			fis.read(md5);
			
			return new String(md5);
		}
		
		public String computeMD5() throws IOException {
			String output = "";
			
			try {
				MessageDigest digest = MessageDigest.getInstance("MD5");
				FileInputStream is = new FileInputStream(
						new File(Preferences.getIndexDir(activity),"XMLittre.idx.tmp"));
				
				byte[] bytes = new byte[20480];
				int readBytes = 0;
				
				while ((readBytes = is.read(bytes)) > 0) {
					digest.update(bytes, 0, readBytes);
				}
				
				BigInteger bigInt = new BigInteger(1, digest.digest());
				output = bigInt.toString(16);
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return output;
		}
		
		protected void onProgressUpdate(Integer... progress) {
			if (progress[0] != -1) {
				d.setProgress(progress[0]);
			} else {
				d.setMessage(activity.getString(R.string.md5check_message));
				d.setIndeterminate(true);
			}
		}
		
		protected void onPostExecute(Boolean result) {
			d.dismiss();
			wl.release(); // Release the wake lock.
			
			if (result == false) {
				AlertDialog.Builder builder = new AlertDialog.Builder(activity);
				builder.setTitle(activity.getString(R.string.downloading_title))
					.setMessage(activity.getString(R.string.downloading_error))
					.setNeutralButton("Fermer", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						activity.finish();
					}
				});
				
				AlertDialog alert = builder.create();
				alert.show();
			}
		}
	}
	
    @Override
    public Object onRetainNonConfigurationInstance() {
        return downtask;
    }
}