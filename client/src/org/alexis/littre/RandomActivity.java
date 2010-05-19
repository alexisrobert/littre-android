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

import android.app.ProgressDialog;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;

public class RandomActivity extends WordListActivity {
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		new RandomTask(this).execute(new Object());
	}
	
	private static class RandomTask extends AsyncTask<Object, Integer, Integer> {
		private static RandomActivity context = null;
		private ProgressDialog dialog = null;
		
		RandomTask(RandomActivity context) {
			RandomTask.context = context;
		}
		
		@Override
		protected void onPreExecute() {
			dialog = new ProgressDialog(context);
			dialog.setTitle(context.getString(R.string.loadingrandom_title));
			dialog.setMessage(context.getString(R.string.loadingrandom_message));
			dialog.setCancelable(false);
			dialog.show();
		}
		
		@Override
		protected Integer doInBackground(Object... params) {
			// First, get the number of words
			int words_number = 0;
			
			Cursor c = context.managedQuery(StardictProvider.WORDS_NUMBER_URI, null, null, null, null);
			c.moveToFirst();
			
			words_number = c.getInt(0);
			
			// Then, get an integer in the segment [0,words_number-1]
			return (int) (Math.random()*(words_number-1));
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			dialog.dismiss();
			context.fireShowIntent(result, true);
		}
	}
}
