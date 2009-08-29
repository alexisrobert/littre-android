/*
 * Stardict """library""" for Android (not so UI-independent :) )
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

package org.alexis.libstardict;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.Vector;

import org.alexis.littre.R;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.util.Log;

public class Index {
	private IndexDB indexdb;
	private Context ctx;
	private ProgressDialog d;
	private DownloadTask task = null;
	
	@SuppressWarnings("unused")
	private String indexpath; // Here for JNI.
	
	public Index(Context ctx) throws FileNotFoundException {
		this.ctx = ctx;
		
		/* The db only need to rely on Application context
		 * and window's context is subject to change over rotations. */
		indexdb = new IndexDB(ctx.getApplicationContext());
		
		System.loadLibrary("littre");
		this.indexpath = new File(indexdb.indexDir(), "XMLittre.idx").getAbsolutePath();
	}
	
	public void open() {
		/* If the thread is already launched, the db is locked so we would
		 * enter into a mutex waiting. And if the .tofill attribute is set to
		 * true, we don't need to update it because that means the Thread
		 * is currently filling the DB (i don't think i'm crystal clear here :) ). */
		
		if (indexdb.needsFilling() == true) {
	    	ConnectivityManager c = (ConnectivityManager)ctx.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
	    	
			if (c.getActiveNetworkInfo() == null || c.getActiveNetworkInfo().isAvailable() == false) {
				AlertDialog.Builder alert = new AlertDialog.Builder(ctx);
				alert.setTitle(ctx.getString(R.string.no_network_title));
				alert.setMessage(ctx.getString(R.string.no_network_idx_message));
				alert.show();
				return;
			}
			
			c = null; /* Trash the object, telling the GC to free memory at the next run.
					   * This function is running for a long time and we want to consume the less memory possible. */
			
			d = new ProgressDialog(ctx);
			d.setTitle("Téléchargement de l'index");
			d.setMessage("Veuillez patienter, téléchargement de l'index ...");
			d.setIndeterminate(false);
			
			d.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			
			d.setCancelable(false);
			d.show();
			
			// If the thread is already launching, we're only showing the ProgressDialog
			if (this.task == null) {
				Log.i("libstardict", "First run, will download index ...");
				task = new DownloadTask();
				task.execute();
			}
		}
	}
	
	public void setContext(Context ctx) {
		this.ctx = ctx;
	}
	
	/* If there is a configuration change, dropping everything
	 * relating to the current window context. */
	public void prepareConfigurationChange() {
		if (this.d != null) this.d.dismiss();
	}
	
	public native String[] getRawWords(String query);
	
	public Vector<String> getWords(String query) {
		String[] words = getRawWords(query);
		Vector<String> data = new Vector<String>();
		
		for (int i = 0; i < words.length; i++) {
			data.add(words[i]);
		}
		
		return data;
	}
	
	public Vector<String> getHistory() {
		return wordsQuery("SELECT word FROM recent_history", new String[]{});
	}
	
	public native Word getWord(String name);
	
	private Vector<String> wordsQuery(String sql, String[] params) {
		Vector<String> data = new Vector<String>();
		SQLiteDatabase db = indexdb.getReadableDatabase();
		Cursor c = db.rawQuery(sql, params);
		c.moveToFirst();
		
		for (int i = 0; i < c.getCount(); i++) {
			data.add(c.getString(0));
			c.moveToNext();
		}
		
		c.close();
		db.close();
		
		return data;
	}
	
	// Download the .idx
	private class DownloadTask extends AsyncTask<Object, Object, Boolean> {
		@Override
		protected Boolean doInBackground(Object... arg0) {
			Log.d("libstardict","Beginning downloading index ...");
			
			try {
				URL url = new URL(ctx.getString(R.string.indexurl));
				HttpURLConnection http = (HttpURLConnection) url.openConnection();
				http.connect();
				BufferedInputStream fis = new BufferedInputStream(http.getInputStream());
				FileOutputStream fos = new FileOutputStream(new File(indexdb.indexDir(),"XMLittre.idx.tmp"));
				
				byte[] bytes = new byte[20480];
				int progress = 0;
				int readBytes = 0;
				
				while ((readBytes = fis.read(bytes)) > 0) {
					progress += readBytes;
					d.setProgress((int)(((float)progress/http.getContentLength())*100));
					fos.write(bytes, 0, readBytes);
				}
				
				fis.close();
				fos.close();
			} catch (IOException e){
				e.printStackTrace();
			}
			
			new File(indexdb.indexDir(),"XMLittre.idx.tmp").renameTo(new File(indexdb.indexDir(),"XMLittre.idx"));
			
			Log.d("libstardict", "Download finished!");
			
			return true;
		}
		
		protected void onPostExecute(Boolean result) {
			d.dismiss();
		}
	}
	
	public void storeHistory(String word) {
		SQLiteDatabase db = indexdb.getWritableDatabase();
		Date date = new Date();
		
		/* Nothing else is needed !
		 * Old history wiping is completely managed by SQLite's 
		 */
		db.execSQL("INSERT INTO history (word, timestamp) VALUES (?,?)", 
				new String[] {word, String.valueOf(date.getTime())});
		
		db.close();
	}
}
