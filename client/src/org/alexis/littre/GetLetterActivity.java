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

import java.util.List;
import java.util.Vector;

import android.app.SearchManager;
import android.database.Cursor;
import android.os.Bundle;

public class GetLetterActivity extends WordListActivity {
	private Vector<String> words;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	this.mTextFiltering = false; // Text filtering uses too much CPU here, we have too big lists.
    	
    	super.onCreate(savedInstanceState);
    	
    	// We only search if we have no backup.
    	if (savedInstanceState != null)
    		return;
    	
    	// We only accept valid search intents
    	if (getIntent().getStringExtra(SearchManager.QUERY) == null)
    		return;
    	
        setProgressBarIndeterminateVisibility(true);
    	
    	new Thread(new Runnable() {
    		public void run() {
    			String search = getIntent().getExtras().getString(SearchManager.QUERY);
    			
    			words = new Vector<String>();
    			Cursor c = managedQuery(StardictProvider.LETTER_URI, null, null, new String[] {search}, null);
    			c.moveToFirst();
    			while (!c.isAfterLast()) {
    				words.add(c.getString(0));
    				c.moveToNext();
    			}
    			
    			runOnUiThread(new Runnable() {
    				public void run() {
    					setWords((List<String>)words);
    					setProgressBarIndeterminateVisibility(false);
    				}
    			});
    		}
    	}).start();
    }
}
