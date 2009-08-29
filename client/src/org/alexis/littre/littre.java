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

import java.io.FileNotFoundException;
import java.util.Vector;

import org.alexis.libstardict.Index;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

/* TODO: Re-program this class FROM SCRATCH to be AlphabetActivity 
 * 		 and dissociate Index initialisation from this activity,
 * 		 we don't need an index instance here, nor serialization */

public class littre extends ListActivity {
    Index idx;
    Intent intent;
    Vector<String> words;
    
    static final char A_ASCII_CODE = 65;
    static final char Z_ASCII_CODE = 90;
    
    static final String INTENT_GET_HISTORY = "org.alexis.littre.GetHistory";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
        
        setContentView(R.layout.list);
        
        intent = getIntent();
        
        /* Restoring serialized state */
        if (getLastNonConfigurationInstance() == null) {
        	idx = new Index(this);
        } else {
        	// If we were rotating, we just need to refresh Index's context
        	idx = (Index)getLastNonConfigurationInstance();
        	idx.setContext(this);
        }
        	
        idx.open();
		
		updateList();
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        
        Intent i = new Intent(Intent.ACTION_SEARCH, null, this.getApplicationContext(), GetLetterActivity.class);    
        i.putExtra(SearchManager.QUERY, ((TextView)v.findViewById(R.id.word)).getText());
        startActivity(i);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuItem menuit_hist = menu.add(0, Menu.FIRST, 0, "Historique");
        menuit_hist.setIcon(android.R.drawable.ic_menu_recent_history);
        
        return true;
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
        case Menu.FIRST:
        	Intent i = new Intent(INTENT_GET_HISTORY, null, getApplicationContext(), HistoryActivity.class);
        	startActivity(i);
        	return true;
        }
        
        return super.onMenuItemSelected(featureId, item);
    }
    
    // Update the ArrayAdapter containing the words in the ListActivity
    public void updateList() {
    	words = new Vector<String>();
    	
    	// If words list is empty, fill-in with the alphabet
    	for (char i = A_ASCII_CODE; i <= Z_ASCII_CODE; i++) {
    		words.add(String.valueOf(i));
    	}
    	
    	ArrayAdapter<String> wordlist = new ArrayAdapter<String>(this, R.layout.wordlistitem, R.id.word, words);
    	
    	setListAdapter(wordlist);
    }
    
    @Override
    public Object onRetainNonConfigurationInstance() {
    	idx.prepareConfigurationChange();
    	
    	return idx;
    }
}