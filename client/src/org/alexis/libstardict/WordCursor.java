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

package org.alexis.libstardict;

import android.database.AbstractCursor;

/* 
 * For performance reasons, we have our own Cursor which spare us
 * browsing the HUGE word list to use MatrixCursor.
 */
public class WordCursor extends AbstractCursor {
	private String[] words;
	private int position = 0;
	
	public WordCursor(String[] words) {
		this.words = words;
	}
	
	@Override
	public String[] getColumnNames() {
		return new String[] {"word"};
	}

	@Override
	public int getCount() {
		return words.length;
	}

	@Override
	public double getDouble(int column) {
		throw new NullPointerException("no 'double' columns"); // TODO: Find a better exception than this :D
	}

	@Override
	public float getFloat(int column) {
		throw new NullPointerException("no 'float' columns");
	}

	@Override
	public int getInt(int column) {
		throw new NullPointerException("no 'int' columns");
	}

	@Override
	public long getLong(int column) {
		throw new NullPointerException("no 'long' columns");
	}

	@Override
	public short getShort(int column) {
		throw new NullPointerException("no 'short' columns");
	}

	@Override
	public String getString(int column) {
		if (column > 0)
			throw new NullPointerException("no column index > 0");
		
		position++;
		
		return this.words[position-1];
	}

	@Override
	public boolean isNull(int column) {
		if (column > 0)
			return true;
		else
			return false;
	}

}
