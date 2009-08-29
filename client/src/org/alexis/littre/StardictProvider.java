package org.alexis.littre;

import java.util.HashMap;

import org.alexis.libstardict.Index;

import android.content.ContentProvider;
import android.content.ContentValues;
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
 * content://org.alexis.littre.stardictprovider/history_livefolder - you know ... stuff ... :p
 */

public class StardictProvider extends ContentProvider {
	public static final Uri WORDS_URI = 
        Uri.parse("content://org.alexis.littre.stardictprovider/words");
	public static final Uri LETTER_URI = 
        Uri.parse("content://org.alexis.littre.stardictprovider/letter");
	public static final Uri HISTORY_URI = 
        Uri.parse("content://org.alexis.littre.stardictprovider/history");
	public static final Uri HISTORY_LIVEFOLDER_URI = 
        Uri.parse("content://org.alexis.littre.stardictprovider/history_livefolder");
	
	private Index idx;
	
	private static final HashMap<String,String> LIVE_FOLDER_PROJECTION_MAP;
	static {
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
		idx = new Index(this.getContext());
		return true;
	}
	
	@Override
	// TODO: Switch to URI_MATCHER, this is a mess.
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		
		if (uri.compareTo(WORDS_URI) == 0 || uri.compareTo(LETTER_URI) == 0) {
			String[] words;
			
			if (selectionArgs.length == 0)
				return null;
			
			if (uri.compareTo(WORDS_URI) == 0)
				words = idx.getRawWords(selectionArgs[0]);
			else
				words = idx.getRawLetter(selectionArgs[0]);
			
			// TODO: To improve speed, maybe I should write my own cursor.
			// This kind of cursor often has A LOT of records here.
			MatrixCursor cursor = new MatrixCursor(new String[] {"name"});
			
			for (int i = 0; i < words.length; i++) {
				cursor.addRow(new String[] {words[i]});
			}
			return cursor;
		} else if (uri.compareTo(HISTORY_URI) == 0) {
			SQLiteDatabase db = idx.getIndexDB().getReadableDatabase();
			Cursor c = db.rawQuery("SELECT word FROM recent_history", new String[]{});
			return c;
		} else if (uri.compareTo(HISTORY_LIVEFOLDER_URI) == 0) {
			SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			qb.setTables("recent_history");
			qb.setProjectionMap(LIVE_FOLDER_PROJECTION_MAP);
			
			SQLiteDatabase db = idx.getIndexDB().getReadableDatabase();
			Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, null);
			c.setNotificationUri(getContext().getContentResolver(), uri);
			
			return c;
		} else {
			Log.e("stardictprovider", "UNKNOWN URI!");
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
