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
