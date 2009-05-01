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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.Date;
import java.util.Vector;

import org.alexis.littre.R;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.util.Log;

public class Index {
	private IndexDB indexdb;
	private Context ctx;
	private Handler mHandler = new Handler();
	private ProgressDialog d;
	private Thread t = null;
	
	public Index(Context ctx) throws FileNotFoundException {
		this.ctx = ctx;
		
		/* The db only need to rely on Application context
		 * and window's context is subject to change over rotations. */
		indexdb = new IndexDB(ctx.getApplicationContext());
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
			d.setTitle("Construction de l'index");
			d.setMessage("Veuillez patienter, téléchargement et construction de l'index (peut prendre plusieurs minutes) ...");
			d.setIndeterminate(true);
			d.setCancelable(false);
			d.show();
			
			// If the thread is already launching, we're only showing the ProgressDialog
			if (this.t == null || this.t.isAlive() == false) {
				Log.i("libstardict", "First run, will fill SQLite index from .idx ...");
				this.t = new Thread(new Runnable() {			
					SQLiteDatabase db;
					
					public void run() {
						db = indexdb.getWritableDatabase();
						parse(db);
						db.close();
						
						mHandler.post(new Runnable() {
							public void run() {
								d.dismiss();
								indexdb.setFilled();
							}
						});
					}
				});
				t.start();
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
	
	public Vector<String> getWords(String query) {
		return wordsQuery("SELECT word FROM words WHERE word LIKE ?", new String[] {query+"%"});
	}
	
	public Vector<String> getHistory() {
		return wordsQuery("SELECT word FROM recent_history", new String[]{});
	}
	
	public Word getWord(String name) {
		Word w = new Word();
		
		SQLiteDatabase db = indexdb.getReadableDatabase();
		Cursor c = db.rawQuery("SELECT word, offset, size FROM words WHERE word = ?", new String[] {name});
		c.moveToFirst();
		
		w.setName(c.getString(0));
		w.setOffset(c.getInt(1));
		w.setSize(c.getInt(2));
		
		Log.d("libstardict", w.toString());
		
		c.close();
		db.close();
		
		return w;
	}
	
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
	
	/* Put char in a buffer. If the buffer is too big, double its capacity
	 * The buffer will so have an 2^n growth rate, which is exponential.
	 * This is a very simple dynamic buffer implementation. */
	private ByteBuffer putBuffer(ByteBuffer charbuf, byte data) {
		if (charbuf.hasRemaining() == false) {
			Log.d("libstardict", String.format("Buffer capacity size was increased from %d to %d",
					charbuf.capacity(), charbuf.capacity()*2));
			ByteBuffer newbytebuf = ByteBuffer.allocate(charbuf.capacity()*2);
			newbytebuf.put(charbuf.array());
			newbytebuf.put(data);
			return newbytebuf;
		} else {
			charbuf.put(data);
			return charbuf;
		}
	}
	
	// Parse the .idx file and fill the data in the SQLite db
	private void parse(SQLiteDatabase database) {
		Log.d("libstardict","Beginning parsing index ...");
		
		int buf;
		ByteBuffer bbuf = ByteBuffer.allocate(8);
		ByteBuffer charbuf = ByteBuffer.allocate(8);
		CharsetDecoder ch = Charset.forName("UTF-8").newDecoder()
							.onMalformedInput(CodingErrorAction.REPLACE)
							.onUnmappableCharacter(CodingErrorAction.REPLACE);
		
		int counter = 0;
		
		database.beginTransaction();
		
		SQLiteStatement stm = database.compileStatement("INSERT INTO words (word, offset, size) VALUES (?, ?, ?)");
		
		try {
			URL url = new URL(ctx.getString(R.string.indexurl));
			HttpURLConnection http = (HttpURLConnection) url.openConnection();
			http.connect();
			BufferedInputStream fis = new BufferedInputStream(http.getInputStream());
			
			while (true) {
				counter++;
				
				buf = fis.read();
				while (buf != '\u0000' && buf != -1) {
					charbuf = putBuffer(charbuf, (byte)buf);
					buf = fis.read();
				}
				
				if (buf == -1)
					break;
				
				charbuf.limit(charbuf.position());
				charbuf.position(0);
				stm.bindString(1, ch.decode(charbuf).toString());
				
				// Now get offset/size
				if (fis.read(bbuf.array(),0,8) == -1) {
					break;
				}
				
				bbuf.position(0);
				
				stm.bindLong(2, bbuf.getInt());
				stm.bindLong(3, bbuf.getInt());
				
				bbuf.clear();
				
				stm.executeInsert();
				if (counter % 10000 == 0)
					Log.i("libstardict", String.format("%d words processed",counter));
				
				charbuf.clear();
			}
			database.setTransactionSuccessful();
			database.endTransaction();
		} catch (IOException e){
			e.printStackTrace();
			
			database.endTransaction();
			indexdb.dropDatabase(database); // Will recreate index
		}
		
		stm.close();
		
		Log.d("libstardict", String.format("Parsing finished : %d words processed", counter));
	}

	public void storeHistory(String word) {
		SQLiteDatabase db = indexdb.getWritableDatabase();
		Date date = new Date();
		
		/* Nothing else is needed !
		 * Old history wiping is entierly managed by SQLite's 
		 */
		db.execSQL("INSERT INTO history (word, timestamp) VALUES (?,?)", 
				new String[] {word, String.valueOf(date.getTime())});
		
		db.close();
	}
}
