/*
  GNUJSP - a free JSP1.0 implementation
  Copyright (C) 1999, Yaroslav Faybishenko <yaroslav@cs.berkeley.edu>

  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  as published by the Free Software Foundation; either version 2
  of the License, or (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
*/
package org.gjt.jsp;

import java.io.*;
import java.util.*;

/**
 * This type of reader allows you to stack a stream onto the current one.
 * So if you are currently reading stream A, then push stream B on the
 * stack, then all future read() calls will be done to stream B, until
 * stream B is read fully (until EOF).  When stream B has been read fully,
 * all future reads will continue to be from stream A... until either
 * A has been read fully or another stream is pushed on the stack.
 *
 * This is meant to facilitate static recursive server-side includes that
 * are possible when compiling JSP files.
 */

class StackedReader extends Reader {
    Stack readers;
    Stack includes;
    Vector visitedIncludes;

    LineNumberReader currentReader;
    String currentInclude;


    StackedReader (Reader r, String o) {
	readers  = new Stack();
	includes = new Stack();
	visitedIncludes = new Vector();
	pushReader(r,o);
    }
	
    int getLineNumber () {
	return currentReader.getLineNumber();
    }
    /**
     * we are a StackedReader, so multiple files may be involved
     */ 
    Pos getPos() {
	return new Pos(getCurrentInclude().toString(), currentReader.getLineNumber()+1);
    }

    public void pushReader (Reader r, String o) {
	if (currentReader != null) {
	    readers.push(currentReader);
	    includes.push(currentInclude);
	}
	currentReader = (r instanceof LineNumberReader) ? (LineNumberReader) r : new LineNumberReader(r);
	visitedIncludes.addElement(o);
	currentInclude = o;
    }

    private void pop () {
	currentReader = (LineNumberReader) readers.pop();
	currentInclude = (String) includes.pop();
    }

    public String getCurrentInclude () {
	return currentInclude;
    }

    public String[] getAllIncludes() {
	String[] all = new String [visitedIncludes.size()];
	visitedIncludes.copyInto(all);
	return all;
    }

    public final int read () throws IOException {
	if (currentReader == null) return -1;
	int c = currentReader.read ();
	if (c == -1) {
	    if (!readers.empty()) {
		currentReader.close ();
	        pop();
		return read ();
	    }
	} 
	return c;
    }

    public final int read (char[] buf) throws IOException {
	return read (buf, 0, buf.length);
    }

    public final int read (char[] buf, int off, int len) throws IOException {
	int ret = currentReader.read (buf, off, len);

	if (ret == -1) {
	    if (!readers.empty()) {
		currentReader.close ();
		pop();
		return read (buf, off, len);
	    }
	} 
	return ret;
    }

    public boolean ready () throws IOException {
	return currentReader.ready ();
    }

    public void reset () throws IOException {
	currentReader.reset ();
    }

    public void close () throws IOException {
	if (currentReader != null) currentReader.close ();
	if (!readers.empty()) {
	    pop();
	}
    }

    public void mark (int readAheadLimit) throws IOException {
	currentReader.mark (readAheadLimit);
    }
    
    
    public boolean markSupported () {
	return currentReader.markSupported ();
    }

    public long skip (long n) throws IOException {
	long skipped = currentReader.skip (n);

	if (skipped < n) {
	    if (!readers.empty()) {
		currentReader.close ();
		pop();
		return currentReader.skip (n - skipped);
	    }
	}

	return skipped;
    }
}
