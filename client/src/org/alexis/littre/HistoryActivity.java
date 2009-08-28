package org.alexis.littre;
import android.os.Bundle;

public class HistoryActivity extends WordListActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setTitle("Historique");
	    setShowHistory(false);
	    
	    // We only search if we have no backup.
	    if (savedInstanceState == null) {
	    	setWords(idx.getHistory());
	    }
	}
}