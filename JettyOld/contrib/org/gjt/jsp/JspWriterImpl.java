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

import java.io.IOException;
import java.io.Writer;
import javax.servlet.jsp.JspWriter;
import javax.servlet.ServletResponse;

public class JspWriterImpl extends JspWriter 
    implements JspMsg
{
    protected ServletResponse response;
    protected Writer out;  
    private String lineSeparator;
    private char cb[];
    private int nChars;
    private int nextChar;
    private boolean flushed;
    /** Should a call to method close be propagated to
	encapsulated stream ? Needed for JSDK 2.0 compat (alph)*/
    protected boolean propagateClose = true;
    /** Should a call throw an exception if buffer was flushed?
	Needed for JSDK 2.0 compat (alph)*/
    protected boolean allowFlushedClear = false;
    
    protected JspWriterImpl (ServletResponse response, int bufferSize, boolean autoFlush)
    {
	super(bufferSize, autoFlush);
	this.response   = response;

	lineSeparator = System.getProperty("line.separator");

	cb = new char[bufferSize];
	nChars = bufferSize;
	nextChar = 0;
	
	flushed = false;
    }
    
    
    
    public int getRemaining () {
	return bufferSize - nextChar;
    }
    
    public void clear () throws IOException {
	//	System.err.println("JspWriterImpl.clear() - flushed="+flushed);
	nextChar = 0;
	// when used as a Wrapper for JspWriter
	// for JSDK 2.0 compat, we must allow clear
	// on flushed streams. (alph)
	if (flushed && !allowFlushedClear) 
	    throw new IOException(JspConfig.getLocalizedMsg
				  (ERR_sp10_2_7_1_illegal_clear_after_write));
    }
    /**
     * "A.3 This method is like clear() except that no exception will
     *      raised if the buffer has been autoflushed."
     */
    public void clearBuffer() throws IOException {
	nextChar = 0;
    }
    
    public void flush () throws IOException {
	flushBuffer (false);
	if (out != null)
	    out.flush ();
    }

    public void close () throws IOException {
	flush ();
	/* if there was nothing to flush, */
	/* we never even got the stream, so there's 
	   nothing to close */
	if (out != null) {
	    // this is for JspWriter wrapping in 
	    // jsdk 2.0 compat code (alph)
	    if(propagateClose) 
		out.close (); 
	    else
		out.flush();
	}
    }
    
    /**
     * Flush the output buffer to the underlying character stream, without
     * flushing the stream itself.  isAutoFlush is true if method is called 
     * if buffer was exceeded on write.
     */
    private void flushBuffer(boolean isAutoFlush) 
	throws IOException
    {
	// System.err.println("JspWriterImpl.flushBuffer("+isAutoFlush+") - nextChar="
	// +nextChar+", bufferSize="+bufferSize); 
	// not a real flush: nothing in buffer
	if (nextChar == 0 && bufferSize > 0)
	    return;

	// no matter how big the buffer is, 
	// it was flushed.
	flushed = true;

	if (bufferSize <= 0)
	    return;
	
	if (nextChar == 0)
	    return;

	if (out == null) 
	    out = response.getWriter ();

	if (isAutoFlush && !autoFlush) {
	    throw new IOException (JspConfig.getLocalizedMsg
				   (ERR_sp10_2_7_1_exceeded_buffer_size)
				   +": "+bufferSize);
	}
	out.write (cb, 0, nextChar);
	nextChar = 0;
    }

    /**
     * Flush the output buffer to the underlying character stream, without
     * flushing the stream itself.  This method is non-private only so that it
     * may be invoked by PrintStream.
     */
    void flushBuffer () throws IOException {
	flushBuffer(false);
    }

    /**
     * Write a single character.
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void write (int c) throws IOException {

	if(bufferSize > 0) {
	    if (nextChar >= nChars)
		flushBuffer (true);
	    cb[nextChar++] = (char) c;
	} else {
	    if (out == null) 
		out = response.getWriter ();
	    out.write(c);
	}
	
    }

    /**
     * Write a portion of an array of characters.
     *
     * <p> Ordinarily this method stores characters from the given array into
     * this stream's buffer, flushing the buffer to the underlying stream as
     * needed.  If the requested length is at least as large as the buffer,
     * however, then this method will flush the buffer and write the characters
     * directly to the underlying stream.  Thus redundant
     * <code>BufferedWriter</code>s will not copy data unnecessarily.
     *
     * @param  cbuf  A character array
     * @param  off   Offset from which to start reading characters
     * @param  len   Number of characters to write
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void write (char cbuf[], int off, int len) throws IOException {
	
	if (len >= nChars) {
	    /* If the request length exceeds the size of the output buffer,
	       flush the buffer and then write the data directly.  In this
	       way buffered streams will cascade harmlessly. */
	    flushBuffer (true);
	    if (out == null) 
		out = response.getWriter ();
	    out.write (cbuf, off, len);
	    return;
	}

	int b = off, t = off + len;
	while (b < t) {
	    int d = Math.min (nChars - nextChar, t - b);
	    System.arraycopy (cbuf, b, cb, nextChar, d);
	    b += d;
	    nextChar += d;
	    if (nextChar >= nChars)
		flushBuffer (true);
	}
    
    }
    
    /** 
     * Write a String
     * @param  s     String to be written
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void write (String s) throws IOException {
	// System.err.println("JspWriterImpl.write("+s+")");
	write(s, 0, s.length());
    }

    /**
     * Write a portion of a String.
     *
     * @param  s     String to be written
     * @param  off   Offset from which to start reading characters
     * @param  len   Number of characters to be written
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void write (String s, int off, int len) throws IOException {

	if(bufferSize > 0) {
	    int b = off, t = off + len;
	    while (b < t) {
		int d = Math.min (nChars - nextChar, t - b);
		s.getChars (b, b + d, cb, nextChar);
		b += d;
		nextChar += d;
		if (nextChar >= nChars)
		    flushBuffer (true);
	    }
	} else {
	    // no buffer means flushed if written to. (alph)
	    flushed = true;
	    if (out == null) 
		out = response.getWriter ();
	    out.write (s, off, len);
	}
    }

    public void newLine () throws IOException {
	write (lineSeparator);
    }
    
/* Methods that do not terminate lines */

/* Print a boolean. */
public void print (boolean b) throws IOException {
    write (b ? "true" : "false");
}

/** Print a character. */
public void print (char c) throws IOException {
    write (String.valueOf (c));
}

/** Print an integer. */
public void print (int i) throws IOException {
    write (String.valueOf (i));
}

/** Print a long. */
public void print (long l) throws IOException {
    write (String.valueOf (l));
}

/** Print a float. */
public void print (float f) throws IOException {
    write (String.valueOf (f));
}

/** Print a double. */
public void print (double d) throws IOException {
    write (String.valueOf (d));
}

/** Print an array of chracters. */
public void print (char s[]) throws IOException {
    write (s);
}

/** Print a String. */
public void print (String s) throws IOException {

    write (s == null ? "null" : s);

}

/** Print an object. */
public void print (Object obj) throws IOException {
    write (String.valueOf (obj));
}


/* Methods that do terminate lines */

/** Finish the line. */
public void println () throws IOException {
    
    newLine ();

}

/** Print a boolean, and then finish the line. */
public void println (boolean x) throws IOException {
	
    print (x);
    newLine ();

}

/** Print a character, and then finish the line. */
public void println (char x) throws IOException {
	
    print (x);
    newLine ();

}

/** Print an integer, and then finish the line. */
public void println (int x) throws IOException {
	
    print (x);
    newLine ();

}

/** Print a long, and then finish the line. */
public void println (long x) throws IOException {
	
    print (x);
    newLine ();

}

/** Print a float, and then finish the line. */
public void println (float x) throws IOException {
	
    print (x);
    newLine ();

}

/** Print a double, and then finish the line. */
public void println (double x) throws IOException {
	
    print (x);
    newLine ();

}

/** Print an array of characters, and then finish the line. */
public void println (char x[]) throws IOException {
	
    print (x);
    newLine ();

}

/** Print a String, and then finish the line. */
public void println (String x) throws IOException {
	
    print (x);
    newLine ();

}

/** Print an Object, and then finish the line. */
public void println (Object x) throws IOException {
	
    print (x);
    newLine ();
	
}

}
