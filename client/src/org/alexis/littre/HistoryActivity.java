package org.alexis.littre;
import java.util.Vector;

import android.database.Cursor;
import android.os.Bundle;

public class HistoryActivity extends WordListActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setTitle("Historique");
	    setShowHistory(false);
	    
	    // We only search if we have no backup.
	    if (savedInstanceState == null) {
			Vector<String> words = new Vector<String>();
			Cursor c = managedQuery(StardictProvider.HISTORY_URI, null, null, null, null);
			c.moveToFirst();
			while (!c.isAfterLast()) {
				words.add(c.getString(0));
				c.moveToNext();
			}
	    	
			setWords(words);
	    }
	}
}