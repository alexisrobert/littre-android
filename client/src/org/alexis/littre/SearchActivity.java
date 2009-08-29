package org.alexis.littre;

import java.util.List;
import java.util.Vector;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;

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
    	if (getIntent().getStringExtra(SearchManager.QUERY) == null)
    		return;
    	
        setProgressBarIndeterminateVisibility(true);
    	
    	new Thread(new Runnable() {
    		public void run() {
    			String search = getIntent().getExtras().getString(SearchManager.QUERY);
    			
    			words = idx.getWords(search);
    			
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
