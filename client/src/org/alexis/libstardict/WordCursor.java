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
