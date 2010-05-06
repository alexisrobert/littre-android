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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class IndexDB extends SQLiteOpenHelper {
	private static int HISTORY_DEPTH = 50; // TODO: Make this configurable
	
	public IndexDB(Context context) {
		super(context, "index.db", null, 4);
	}
	
	public static boolean needsFilling(Context ctx) {
		return !(Preferences.getIndexPath(ctx).isFile());
	}
	
	public static boolean moveToSD(Context ctx) {
		if (Preferences.isIndexSD(ctx))
			return true;
		
		Log.i("littre", "Moving index to SDCard ...");
		
		// Making sure the SDCard is mounted in rw
		if (!FileUtils.isSDMounted())
			return false;
		
		// Making sure SDCard directories structure is ready
		if (!Preferences.makeSDDirs())
			return false;
		
		try {
			return FileUtils.moveFile(Preferences.getIndexPath(ctx),
										new File(Preferences.SDCARD_DATA, Preferences.FILENAME));
		} catch (Exception e) {
			Log.i("littre", e.getMessage());
			return false;
		}
	}
	
	public static boolean moveToInternal(Context ctx) {
		if (!Preferences.isIndexSD(ctx))
			return true;
		
		Log.i("littre", "Moving index to internal memory ...");
		
		return FileUtils.moveFile(Preferences.getIndexPath(ctx),
									new File(ctx.getFilesDir(), Preferences.FILENAME));
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE history (wordid INTEGER, word STRING, timestamp INTEGER);");
		
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
		Log.i("littre", "Upgrading database ...");
		db.execSQL("DROP TABLE IF EXISTS words");
		
		if (oldVersion < 4) {
			Log.i("littre", " - History use wordids");
			db.execSQL("DROP TABLE history");
			db.execSQL("DROP VIEW recent_history");
			onCreate(db);
		}
	}
	
	public void dropDatabase(SQLiteDatabase db) {
		db.execSQL("DROP TABLE IF EXISTS words");
	}
}