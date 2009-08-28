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
import java.util.List;
import java.util.Vector;

import org.alexis.libstardict.Index;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.SearchManager;
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
import android.widget.TextView;

/* TODO: Make a subclass of ListActivity called WordActivity
 *		 this would be MUCH cleaner. (I don't have a lot of time right now).
 *		 This class is beginning to be a REAL mess... */

public class littre extends ListActivity {
    Index idx;
    Intent intent;
    Vector<String> words;
    boolean alphabet = false;
    
    GetDefinitionTask task;
    
    char mode = MODE_NORMAL;
    
    static final char MODE_NORMAL = 0;
    static final char MODE_HISTORY = 1;
    static final char A_ASCII_CODE = 65;
    static final char Z_ASCII_CODE = 90;
    static final String BUNDLE_ALPHABET_KEY = "alphabet";
    static final String BUNDLE_WORDS_KEY = "words";
    static final String INTENT_GET_HISTORY = "org.alexis.littre.GetHistory";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
        
        setContentView(R.layout.list);
        
        intent = getIntent();
        
        /* Restoring serialized state */
        
        try {
        	if (getLastNonConfigurationInstance() == null) {
        		idx = new Index(this);
        	} else {
        		// If we were rotating, we just need to refresh Index's context
        		idx = (Index)getLastNonConfigurationInstance();
        		idx.setContext(this);
        	}
        	
        	idx.open();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		if (savedInstanceState != null) {
			String[] wordarray = savedInstanceState.getStringArray(BUNDLE_WORDS_KEY);
			
			// If wordarray.length == 0, it was searching. So, re-run the search.
			if (wordarray.length > 0) {
				words = new Vector<String>();
				for (int i = 0; i < wordarray.length; i++) {
					words.add(wordarray[i]);
				}
				updateList(words);
				
				this.alphabet = savedInstanceState.getBoolean(BUNDLE_ALPHABET_KEY);
				
				return;
			}
		}
		
		setProgressBarIndeterminateVisibility(true);
		
        new Thread(new Runnable() {
        	public void run() {
        		words = new Vector<String>();
        		
        		if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_SEARCH)) {
        			String search = intent.getExtras().getString(SearchManager.QUERY);
        			Log.d("littre", String.format("Received search intent, will search %s", search));
        			
        			if (search.length() > 0) {
        				words = idx.getWords(search);
        				
        				if (words.size() == 1) {
        					runOnUiThread(new Runnable() {
        						public void run() {
        							fireShowIntent(words.get(0), true);
        						}
        					});
        				} else if (words.size() == 0) {
        					runOnUiThread(new Runnable() {
        						public void run() {
        							noResultDialog();
        						}
        					});
        				}
        			}
        		} else if (intent.getAction() != null && intent.getAction().equals(INTENT_GET_HISTORY)) {
        			words = idx.getHistory();
        			
        			runOnUiThread(new Runnable() {
        				public void run() {
        					mode = MODE_HISTORY;
        					setTitle("Historique");
        				}
        			});
        		}
        		
        		runOnUiThread(new Runnable() {
        			public void run() {
        				updateList((List<String>)words);
        				setProgressBarIndeterminateVisibility(false);
        			}
        		});
        	}
        }).start();
        
        getListView().setFastScrollEnabled(true);
    }
    
    protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	
    	Log.d("littre", "Saving instance state");
    	
    	String[] wordsarray = new String[0];
    	wordsarray = words.toArray(wordsarray);
    	outState.putStringArray(BUNDLE_WORDS_KEY, wordsarray);
    	outState.putBoolean(BUNDLE_ALPHABET_KEY, alphabet);
    }
    
    // Method called when no search has returned zero results.
    private void noResultDialog() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(getString(R.string.no_result_title));
		alert.setMessage(getString(R.string.no_result_message));
		alert.show();
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        String word = (String)((TextView)v.findViewById(R.id.word)).getText();
        
        // If we were in alphabet-mode, launch a search
        if (alphabet) {
            Intent i = new Intent(Intent.ACTION_SEARCH, null, this.getApplicationContext(), littre.class);
            
            i.putExtra(SearchManager.QUERY, ((TextView)v.findViewById(R.id.word)).getText());
            startActivity(i);
        } else {
        	// Else, show the article
        	fireShowIntent(word);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuItem menuit = menu.add(0, Menu.FIRST, 0, getString(R.string.menu_search));
        menuit.setIcon(android.R.drawable.ic_menu_search);
        
        if (mode != MODE_HISTORY) {
        	MenuItem menuit_hist = menu.add(0, Menu.FIRST+1, 0, "Historique");
        	menuit_hist.setIcon(android.R.drawable.ic_menu_recent_history);
        }
        
        return true;
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
        case Menu.FIRST:
        	onSearchRequested();
            return true;
        case Menu.FIRST+1:
        	Intent i = new Intent(INTENT_GET_HISTORY, null, getApplicationContext(), littre.class);
        	startActivity(i);
        	return true;
        }
        
        return super.onMenuItemSelected(featureId, item);
    }
    
    // Update the ArrayAdapter containing the words in the ListActivity
    public void updateList(List<String> words) {
    	// If words list is empty, fill-in with the alphabet
    	if (words.size() == 0 && mode == MODE_NORMAL) {
    		for (char i = A_ASCII_CODE; i <= Z_ASCII_CODE; i++) {
    			words.add(String.valueOf(i));
    		}
    		alphabet = true;
    	}
    	
    	ArrayAdapter<String> wordlist = new ArrayAdapter<String>(this, R.layout.wordlistitem, R.id.word, words);
    	
    	setListAdapter(wordlist);
    }
    
    // Fire the intent which is aimed to show the definition
    private void fireShowIntent(String word) { fireShowIntent(word, false); }
    
    private void fireShowIntent(String word, boolean finish) {
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
		d.setTitle("Recherche de votre définition");
		d.setMessage("Veuillez patienter ...");
		d.setIndeterminate(true);
		d.setCancelable(false);
		d.show();
		
    	task.execute(word, idx, d, new Boolean(finish));
    }
    
    private class GetDefinitionTask extends AsyncTask<Object, Object, Boolean> {
    	Intent i;
    	ProgressDialog d;
    	String word;
    	boolean finish;
    	
		@Override
		protected Boolean doInBackground(Object... params) {
			// Parameter checking
			if (!(params.length >= 4) || !(params[0] instanceof String)
					|| !(params[1] instanceof Index)
					|| !(params[2] instanceof ProgressDialog)
					|| !(params[3] instanceof Boolean)) {
				return false;
			}
			
			Index idx = (Index)params[1];
			word = (String)params[0];
			d = (ProgressDialog)params[2];
			finish = (Boolean)params[3];
			
			i = new Intent(Intent.ACTION_VIEW, null, getApplicationContext(), Definition.class);
			i.putExtra("word", idx.getWord(word));
			
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
				idx.storeHistory(word);
				
				startActivity(i);
			}
			
			if (finish == true)
				finish();
		}
    }
    
    @Override
    public Object onRetainNonConfigurationInstance() {
    	idx.prepareConfigurationChange();
    	if (task != null)
    		task.cancel(true);
    	
    	return idx;
    }
}