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

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class IndexDB extends SQLiteOpenHelper {
	private boolean tofill = false;
	private static int HISTORY_DEPTH = 50; // TODO: Make this configurable
	
	public IndexDB(Context context) {
		super(context, "index.db", null, 1);
	}
	
	public boolean needsFilling() {
		if (this.tofill == false) {
			// If there is no words, we need to fill the index.
			SQLiteDatabase db = this.getReadableDatabase();
			Cursor c = db.rawQuery("SELECT 1 FROM words LIMIT 1", new String[] {});
			
			if (c.getCount() == 0)
				this.tofill = true;
			c.close(); // <= You NEED to close cursor. If you don't, there you'll have warnings/memleaks.
			db.close();
		}
		
		return this.tofill;
	}
	
	public void setFilled() {
		this.tofill = true;
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE words (word STRING, offset INTEGER, size INTEGER);");
		db.execSQL("CREATE TABLE history (word STRING, timestamp INTEGER);");
		
		// I love views and triggers -- SQL<3 :)
		db.execSQL("CREATE VIEW recent_history AS SELECT * FROM history ORDER BY timestamp DESC LIMIT "+HISTORY_DEPTH+";");
		
		db.execSQL("CREATE TRIGGER history_add_trigger AFTER INSERT ON history BEGIN DELETE FROM history " +
				"WHERE timestamp = (SELECT timestamp FROM history EXCEPT SELECT timestamp FROM recent_history); " +
				"END;"); // This query will remove history which is not recent.
		
		// This query will remove all the old history items which correspond to the word we're inserting.
		db.execSQL("CREATE TRIGGER history_remove_duplicates BEFORE INSERT ON history BEGIN " +
				"DELETE FROM history WHERE word = new.word; END;");
		
		this.tofill = true;
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		dropDatabase(db);
		onCreate(db);
	}
	
	public void dropDatabase(SQLiteDatabase db) {
		db.execSQL("DROP TABLE IF EXISTS words");
	}
}