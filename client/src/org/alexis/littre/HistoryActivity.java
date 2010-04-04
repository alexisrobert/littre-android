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

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class HistoryActivity extends WordListActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setTitle(String.format(getString(R.string.history_title),getString(R.string.app_name)));
	    
	    registerForContextMenu(this.getListView());
	    
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
	
	@Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, v, menuInfo);
    	
    	menu.setHeaderTitle(getString(R.string.history_context_title));
    	menu.add(0, Menu.FIRST+100, 0, getString(R.string.history_context_delete));
    }
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (item.getItemId() == Menu.FIRST+100) {
			AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();
			String word = (String)getListView().getItemAtPosition(info.position);
			
			Log.i("littre", String.format("Deleting %s from history...", word));
			
			getContentResolver().delete(StardictProvider.HISTORY_URI, "word = ?", new String[] {word});
			
			// We have deleted an item : we restart the HistoryActivity to propagate changes
        	Intent i = new Intent(INTENT_GET_HISTORY, null, getApplicationContext(), HistoryActivity.class);
        	startActivity(i);
        	finish();
        	
			return true;
		}
		return false;
	}
}