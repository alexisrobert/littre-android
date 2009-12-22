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

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;

// TODO: Use singleTop.
public class SearchActivity extends WordListActivity {
	private Vector<String> words;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
    	// We only search if we have no backup.
    	if (savedInstanceState != null) {
    		setResultsTitle();
    		return;
    	}
    	
    	// We only accept valid search intents
    	if (getIntent().getStringExtra(SearchManager.QUERY) == null ||
    			(getIntent().getAction().equals(Intent.ACTION_VIEW) == true && getIntent().getDataString() == null))
    		Log.d("littre", String.format("SearchActivity received an invalid intent : %s", getIntent().toURI()));
    	
        setProgressBarIndeterminateVisibility(true);
        
        if (getIntent().getAction().equals(Intent.ACTION_VIEW) == true) {
        	fireShowIntent(getIntent().getDataString(), true);
        	return;
        }
        
    	new Thread(new Runnable() {
    		public void run() {
    			String search = getIntent().getExtras().getString(SearchManager.QUERY);
    			
    			words = new Vector<String>();
    			Cursor c = managedQuery(StardictProvider.WORDS_URI, null, null, new String[] {search}, null);
    			c.moveToFirst();
    			while (!c.isAfterLast()) {
    				words.add(c.getString(0));
    				c.moveToNext();
    			}
    			
    			runOnUiThread(new Runnable() {
    				public void run() {
						setProgressBarIndeterminateVisibility(false);
						
						switch(words.size()) {
						// No results.
						case 0:
							noResultDialog();
							break;
						
						// 1 result = show the word and close the search
						case 1:
							fireShowIntent(words.get(0), true);
							break;
						
						default:
							setWords((List<String>)words);
							setResultsTitle();
							break;
						}
    				}
    			});
    		}
    	}).start();
    }
    
    // Method called when no search has returned zero results.
    private void noResultDialog() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(getString(R.string.no_result_title));
		alert.setMessage(getString(R.string.no_result_message));
		alert.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				finish();
			}
		});
		alert.show();
    }
    
    // Warning, this method uses getWord() which refers to WordListActivity's words children, not ours.
    private void setResultsTitle() {
    	if (getWords() != null)
    		setTitle(String.format(getString(R.string.results_title), getString(R.string.app_name), getWords().size()));
    }
}
