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