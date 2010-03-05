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

package org.alexis.libstardict;

import java.io.File;
import java.util.Date;
import java.util.Vector;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class Index {
	private IndexDB indexdb;
	
	@SuppressWarnings("unused")
	private String indexpath; // Here for JNI purposes.
	
	public Index(Context ctx) {
		/* The db only need to rely on Application context
		 * and window's context is subject to change over rotations. */
		indexdb = new IndexDB(ctx.getApplicationContext());
		indexdb.getWritableDatabase().close(); // Open a writable database to enable upgrading.
		
		System.loadLibrary("littre");
		this.indexpath = new File(indexdb.indexDir(), "XMLittre.idx").getAbsolutePath();
	}
	
	public native String[] getRawWords(String query);
	public native String[] getRawLetter(String query);
	
	public Vector<String> getWords(String query) {
		String[] words = getRawWords(query);
		Vector<String> data = new Vector<String>();
		
		for (int i = 0; i < words.length; i++) {
			data.add(words[i]);
		}
		
		return data;
	}
	
	public Vector<String> getLetter(String query) {
		String[] words = getRawLetter(query);
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
	public native Word getWordFromId(int id);
	
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
	
	public IndexDB getIndexDB() {
		return this.indexdb;
	}
	
	public void storeHistory(Word word) {
		SQLiteDatabase db = indexdb.getWritableDatabase();
		Date date = new Date();
		
		/* Nothing else is needed !
		 * Old history wiping is completely managed by SQLite's 
		 */
		db.execSQL("INSERT INTO history (wordid, word, timestamp) VALUES (?,?,?)", 
				new String[] {String.valueOf(word.id), word.name, String.valueOf(date.getTime())});
		
		db.close();
	}
}
