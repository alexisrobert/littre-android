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

import java.io.File;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class IndexDB extends SQLiteOpenHelper {
	private static int HISTORY_DEPTH = 50; // TODO: Make this configurable
	private File indexDir = null;
	
	public IndexDB(Context context) {
		super(context, "index.db", null, 2);
		indexDir = context.getFilesDir();
	}
	
	public File indexDir() {
		return indexDir;
	}
	
	public boolean needsFilling() {
		return !(new File(this.indexDir(),"XMLittre.idx").isFile());
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE history (word STRING, timestamp INTEGER);");
		
		// I love views and triggers -- SQL<3 :)
		db.execSQL("CREATE VIEW recent_history AS SELECT * FROM history ORDER BY timestamp DESC LIMIT "+HISTORY_DEPTH+";");
		
		db.execSQL("CREATE TRIGGER history_add_trigger AFTER INSERT ON history BEGIN DELETE FROM history " +
				"WHERE timestamp = (SELECT timestamp FROM history EXCEPT SELECT timestamp FROM recent_history); " +
				"END;"); // This query will remove history which is not recent.
		
		// This query will remove all the old history items which correspond to the word we're inserting.
		db.execSQL("CREATE TRIGGER history_remove_duplicates BEFORE INSERT ON history BEGIN " +
				"DELETE FROM history WHERE word = new.word; END;");
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS words");
	}
	
	public void dropDatabase(SQLiteDatabase db) {
		db.execSQL("DROP TABLE IF EXISTS words");
	}
}