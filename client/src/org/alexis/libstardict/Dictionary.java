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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/* Currently unused, but will be used if a local Stardict dictionnary is used */
public class Dictionary {
	RandomAccessFile f;
	
	public Dictionary(String filename) throws FileNotFoundException {
		f = new RandomAccessFile(new File(filename),"r");
	}
	
	public String getWord(int offset, int size) throws IOException {
		byte[] data = new byte[size];
		f.seek(offset);
		
		if (f.read(data) == -1)
			throw new IOException("Unexpected end of file");
		
		return new String(data);
	}
}
