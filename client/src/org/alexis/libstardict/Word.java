package org.alexis.libstardict;

import java.io.Serializable;

public class Word implements Serializable {
	private static final long serialVersionUID = 4473046261681389738L;
	
	private String name;
	private long offset;
	private int size;
	
	public Word() {}
	
	public String getName() { return this.name; }
	public long getOffset() { return this.offset; }
	public int getSize() { return this.size; }
	
	public void setName(String name) { this.name = name; }
	public void setOffset(long offset) { this.offset = offset; }
	public void setSize(int size) { this.size = size; }
	
	public String toString() {
		return String.format("<Word: \"%s\" (offset=0x%h/size=0x%h)>",name,offset,size);
	}
}
