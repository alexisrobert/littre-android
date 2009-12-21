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

import java.util.HashMap;

import org.alexis.libstardict.Index;
import org.alexis.libstardict.WordCursor;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.LiveFolders;
import android.util.Log;

/*
 * This class enables other processes to access to the index.
 * 
 * But ... they cannot access to offset/size now, because
 * they just need to fire an intent (to SearchActivity) to show it.
 * 
 * content://org.alexis.littre.stardictprovider/words - maps to Index.getWords
 * content://org.alexis.littre.stardictprovider/letter - maps to Index.getLetter
 * content://org.alexis.littre.stardictprovider/history - maps to Index.getHistory
 * content://org.alexis.littre.stardictprovider/history/livefolder - you know ... stuff ... :p
 */

public class StardictProvider extends ContentProvider {
	private static final int WORDS = 10;
	private static final int LETTER = 20;
	private static final int HISTORY = 30;
	private static final int HISTORY_LIVEFOLDER = 31;
	private static final int SEARCH_SUGGEST = 40;
	
	private static final String AUTHORITY = "org.alexis.littre.stardictprovider";
	private static final UriMatcher URI_MATCHER = new UriMatcher(0);
	private Index idx = null;
    
	private static final String[] SEARCH_COLUMNS = {
        "_id",  // must include this column
        SearchManager.SUGGEST_COLUMN_TEXT_1,
        SearchManager.SUGGEST_COLUMN_INTENT_DATA,
        };
	
	
	// Unused in this class, for global lisibility
	public static final Uri WORDS_URI = 
        Uri.parse("content://org.alexis.littre.stardictprovider/words");
	public static final Uri LETTER_URI = 
        Uri.parse("content://org.alexis.littre.stardictprovider/letter");
	public static final Uri HISTORY_URI = 
        Uri.parse("content://org.alexis.littre.stardictprovider/history");
	public static final Uri HISTORY_LIVEFOLDER_URI = 
        Uri.parse("content://org.alexis.littre.stardictprovider/history/livefolder");
	
	private static final HashMap<String,String> LIVE_FOLDER_PROJECTION_MAP;
	static {
		URI_MATCHER.addURI(AUTHORITY, "words", WORDS);
		URI_MATCHER.addURI(AUTHORITY, "letter", LETTER);
		URI_MATCHER.addURI(AUTHORITY, "history", HISTORY);
		URI_MATCHER.addURI(AUTHORITY, "history/livefolder", HISTORY_LIVEFOLDER);
		URI_MATCHER.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST);
		URI_MATCHER.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY+"/*", SEARCH_SUGGEST);
		
	    LIVE_FOLDER_PROJECTION_MAP = new HashMap<String,String>();
	    LIVE_FOLDER_PROJECTION_MAP.put(LiveFolders._ID, "word" +
	            " AS " + LiveFolders._ID);
	    LIVE_FOLDER_PROJECTION_MAP.put(LiveFolders.NAME, "word" +
	            " AS " + LiveFolders.NAME);
	    
	    // TODO : Find a way to convert timestamp using projection mappings
	    //LIVE_FOLDER_PROJECTION_MAP.put(LiveFolders.DESCRIPTION, "timestamp" +
	    //        " AS " + LiveFolders.DESCRIPTION);
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		Log.e("libstardict", "You can't delete words !");
		return 0;
	}
	
	@Override
	public String getType(Uri uri) {
		return "vnd.android.cursor.dir";
	}
	
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		Log.e("libstardict", "You can't insert words !");
		return null;
	}
	
	@Override
	public boolean onCreate() {
		if (this.idx == null)
			idx = new Index(this.getContext());
		return true;
	}
	
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		int match = URI_MATCHER.match(uri);
		
		switch (match) {
		case WORDS:
		case LETTER:
			String[] words;
			
			if (selectionArgs.length == 0)
				return null;
			
			if (match == WORDS)
				words = idx.getRawWords(selectionArgs[0]);
			else
				words = idx.getRawLetter(selectionArgs[0]);
			
			return new WordCursor(words);
		case SEARCH_SUGGEST:
			MatrixCursor cursor = new MatrixCursor(SEARCH_COLUMNS);
			
			if (uri.getLastPathSegment().equals(SearchManager.SUGGEST_URI_PATH_QUERY) == true ||
					uri.getLastPathSegment().length() < 4) {
				return cursor;
			} else {
				Log.d("libstardict", String.format("SEARCH SUGGEST : %s", uri.getLastPathSegment()));
				String[] words2 = idx.getRawWords(uri.getLastPathSegment().toLowerCase());
				long id = 0;
				
				for (String word : words2) {
					id++;
					cursor.addRow(new Object[] {
						id,
						word,
						word
					});
				}
				
				return cursor;
			}
		case HISTORY:
			SQLiteDatabase db = idx.getIndexDB().getReadableDatabase();
			Cursor c = db.rawQuery("SELECT word FROM recent_history", new String[]{});
			return c;
		case HISTORY_LIVEFOLDER:
			SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			qb.setTables("recent_history");
			qb.setProjectionMap(LIVE_FOLDER_PROJECTION_MAP);
			
			SQLiteDatabase db2 = idx.getIndexDB().getReadableDatabase();
			Cursor c2 = qb.query(db2, projection, selection, selectionArgs, null, null, null);
			c2.setNotificationUri(getContext().getContentResolver(), uri);
			
			return c2;
		default:
			Log.e("stardictprovider", String.format("UNKNOWN URI : %s", uri.toString()));
			return null;
		}
	}
	
	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}
}
