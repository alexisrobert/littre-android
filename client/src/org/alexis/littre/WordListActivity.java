package org.alexis.littre;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Vector;

import org.alexis.libstardict.Index;

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
import android.widget.TextView;

public class WordListActivity extends ListActivity {
	static final String INTENT_GET_HISTORY = "org.alexis.littre.GetHistory";
    static final String BUNDLE_WORDS_KEY = "words";
    protected Index idx;
    private List<String> words;
    private GetDefinitionTask task;
    private boolean mShowHistory = true;
    
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
    public void onCreate(Bundle savedInstanceState) {
    	requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
        
        setContentView(R.layout.list);
        
        /* Restoring serialized state */
        // 1. Index
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
    }
    
    // Update the ArrayAdapter containing the words in the ListActivity
    public void setWords(List<String> words) {
    	// If words list is empty, fill-in with the alphabet
    	ArrayAdapter<String> wordlist = new ArrayAdapter<String>(this, R.layout.wordlistitem, R.id.word, words);
    	
    	// Yes we compare pointers adresses.
    	if (words != this.words)
    		this.words = words;
    	
    	setListAdapter(wordlist);
    }
    
    public boolean isShowingHistory() {
    	return mShowHistory;
    }
    
    public void setShowHistory(boolean state) {
    	this.mShowHistory = state;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuItem menuit = menu.add(0, Menu.FIRST, 0, getString(R.string.menu_search));
        menuit.setIcon(android.R.drawable.ic_menu_search);
        
        if (mShowHistory == true) {
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
        	Intent i = new Intent(INTENT_GET_HISTORY, null, getApplicationContext(), HistoryActivity.class);
        	startActivity(i);
        	return true;
        }
        
        return super.onMenuItemSelected(featureId, item);
    }
    
    protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	
    	Log.d("littre", "Saving instance state");
    	
    	String[] wordsarray = new String[0];
    	wordsarray = words.toArray(wordsarray);
    	outState.putStringArray(BUNDLE_WORDS_KEY, wordsarray);
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        String word = (String)((TextView)v.findViewById(R.id.word)).getText();
        
        fireShowIntent(word);
    }
    
    protected void fireShowIntent(String word) { fireShowIntent(word, false); }
    
    protected void fireShowIntent(String word, boolean finish) {
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
