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

import org.alexis.libstardict.Index;
import org.alexis.libstardict.Word;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

public class WordListActivity extends ListActivity {
	public static final String INTENT_GET_HISTORY = "org.alexis.littre.GetHistory";
	public static final String INTENT_DEFINITION = "org.alexis.littre.DEFINITION";
	
    static final String BUNDLE_WORDS_KEY = "words";
    
    protected Index idx;
    private List<String> words;
    private GetDefinitionTask task;
    protected boolean mTextFiltering = true;
    
    private class MyAdapter extends ArrayAdapter<String> implements SectionIndexer {
    	String[] sections;
    	Integer[] positions;
    	
		public MyAdapter(Context context, int resource, int textViewResourceId,
				List<String> objects) {
			super(context, resource, textViewResourceId, objects);
			
			Vector<String> sections = new Vector<String>();
			Vector<Integer> positions = new Vector<Integer>();
			
			String last_section = "";
			int maxoffset = 0;
			
			for (int i = 0; i < objects.size(); i++) {
				maxoffset = Math.min(2, objects.get(i).length());
				if (!objects.get(i).substring(0, maxoffset).equals(last_section)){
					last_section = objects.get(i).substring(0, maxoffset);
					sections.add(last_section.endsWith("-") ? last_section : (last_section+"-"));
					
					positions.add(i);
				}
			}
			
			this.sections = (String[])sections.toArray(new String[sections.size()]);
			this.positions = (Integer[])positions.toArray(new Integer[positions.size()]);
		}
		
		@Override
		public int getPositionForSection(int section) {
			return positions[section];
		}
		
		@Override
		public int getSectionForPosition(int position) {
			int section = 0;
			for (int i = 0; i < positions.length; i++) {
				if (positions[i] == position) {
					section = i;
					break;
				}
			}
			
			return section;
		}
		
		@Override
		public Object[] getSections() {
			return this.sections;
		}
    }
    
    private class GetDefinitionTask extends AsyncTask<Object, Object, Boolean> {
    	Intent i;
    	ProgressDialog d;
    	Word word = null;
    	boolean finish;
    	
		@Override
		protected Boolean doInBackground(Object... params) {
			// Parameter checking
			if (!(params.length >= 4) || (!(params[0] instanceof String) && !(params[0] instanceof Integer))
					|| !(params[1] instanceof Index)
					|| !(params[2] instanceof ProgressDialog)
					|| !(params[3] instanceof Boolean)) {
				return false;
			}
			
			Index idx = (Index)params[1];
			d = (ProgressDialog)params[2];
			finish = (Boolean)params[3];
			
			if (params[0] instanceof String) {
				word = idx.getWord((String)params[0]);
			} else {
				word = idx.getWordFromId((Integer)params[0]);
			}
			
			i = new Intent(INTENT_DEFINITION, null, getApplicationContext(), Definition.class);
			i.putExtra("word", word);
			
			return true;
		}
		
		protected void onPostExecute(Boolean result) {
			d.dismiss();
			setProgressBarIndeterminateVisibility(false);
			
			if (result == true) {
				/* History saving is here to ENSURE that we store it
				 * when it is currently showed. In fact, the above thread
				 * CAN be interrupted, in case of device's rotation for instance,
				 * so : be careful to siding effects! */
				if (word != null) idx.storeHistory(word);
				
				startActivity(i);
			}
			
			if (finish == true)
				finish();
		}
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
        
        setContentView(R.layout.list);
        
        /* Restoring serialized state */
        // 1. Index
        if (getLastNonConfigurationInstance() == null) {
        	idx = new Index(this); // TODO: Why do we instanciate index each time ? Maybe a singleton ?
        } else {
        	// If we were rotating, we just need to refresh Index's context
        	idx = (Index)getLastNonConfigurationInstance();
        }
		
		// 2. Word list
		if (savedInstanceState != null) {
			String[] wordarray = savedInstanceState.getStringArray(BUNDLE_WORDS_KEY);
			
			// If wordarray.length == 0, it was searching. So, re-run the search.
			if (wordarray.length > 0) {
				words = new Vector<String>();
				for (int i = 0; i < wordarray.length; i++) {
					words.add(wordarray[i]);
				}
				
				setWords(words);
			} else {
				savedInstanceState = null; // Else, drop it, and tell the subclass we have nothing for him.
			}
		}
		
		// Enables the little thumb fast scroll widget
        getListView().setFastScrollEnabled(true);
        
        // Enables filtering
        getListView().setTextFilterEnabled(mTextFiltering);
    }
    
    // Update the ArrayAdapter containing the words in the ListActivity
    public void setWords(List<String> words) {
    	// If words list is empty, fill-in with the alphabet
    	MyAdapter wordlist = new MyAdapter(this, R.layout.wordlistitem, R.id.word, words);
    	
    	// Yes we compare pointers adresses.
    	if (words != this.words)
    		this.words = words;
    	
    	setListAdapter(wordlist);
    }
    
    public List<String> getWords() {
    	return this.words;
    }
    
    public boolean isTextFilterEnabled() {
    	return mTextFiltering;
    }
    
    public void setTextFilterEnabled(boolean state) {
    	this.mTextFiltering = state;
    	getListView().setTextFilterEnabled(state);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
    	MenuItem menuit_search = menu.add(0, Menu.FIRST, 0, getString(R.string.menu_search));
    	menuit_search.setIcon(android.R.drawable.ic_menu_search);
        
        return true;
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
        // Warning !! Menu.FIRST+1xy is used by history for context menu purposes. Don't overlap!!
        case Menu.FIRST:
        	onSearchRequested();
        	return true;
        	
        case Menu.FIRST+1:
        	Intent i = new Intent(INTENT_GET_HISTORY, null, getApplicationContext(), HistoryActivity.class);
        	startActivity(i);
        	return true;
        }
        
        return super.onMenuItemSelected(featureId, item);
    }
    
    protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	
    	if (words != null) {
    		Log.d("littre", "Saving instance state");
    		
    		String[] wordsarray = new String[0];
    		wordsarray = words.toArray(wordsarray);
    		outState.putStringArray(BUNDLE_WORDS_KEY, wordsarray);
    	}
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        String word = (String)((TextView)v.findViewById(R.id.word)).getText();
        
        fireShowIntent(word);
    }
    
    protected void fireShowIntent(String word) { fireShowIntent(word, false); }
    protected void fireShowIntent(int wordid) { fireShowIntent(new Integer(wordid), false); }
    protected void fireShowIntent(int wordid, boolean finish) { fireShowIntent(new Integer(wordid), finish); }
    
    protected void fireShowIntent(Object word, boolean finish) {
    	ConnectivityManager c = (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    	
		if (c.getActiveNetworkInfo() == null || c.getActiveNetworkInfo().isAvailable() == false) {
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setTitle(getString(R.string.no_network_title));
			alert.setMessage(getString(R.string.no_network_message));
			alert.show();
			return;
		}
    	
    	setProgressBarIndeterminateVisibility(true);
    	task = new GetDefinitionTask();
    	
    	ProgressDialog d = new ProgressDialog(this);
		d.setTitle(getString(R.string.loadingdef_title));
		d.setMessage(getString(R.string.loadingdef_message));
		d.setIndeterminate(true);
		d.setCancelable(false);
		d.show();
		
    	task.execute(word, idx, d, new Boolean(finish));
    }
}
