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

import java.io.Serializable;

public class Word implements Serializable {
	private static final long serialVersionUID = 4473046261681389738L;
	
	public int id; // It's public to be more easily read/written from JNI.
	public String name;
	public long offset;
	public int size;
	
	public Word() {}
	
	public int getId() { return this.id; }
	public String getName() { return this.name; }
	public long getOffset() { return this.offset; }
	public int getSize() { return this.size; }
	
	public void setId(int id) { this.id = id; }
	public void setName(String name) { this.name = name; }
	public void setOffset(long offset) { this.offset = offset; }
	public void setSize(int size) { this.size = size; }
	
	public String toString() {
		return String.format("<Word: \"%s\" (id=%d/offset=0x%h/size=0x%h)>",name,id,offset,size);
	}
}
