/*
 */

package org.gjt.jsp.jsdk20;

/*
import java.io.OutputStream;
import java.io.CharConversionException;
*/
import java.io.IOException;
import java.io.PrintWriter;
//import javax.servlet.jsp.JspWriter;
import javax.servlet.ServletOutputStream;

/**
 * A ServletOutputStream wrapper for JspWriter for servlet includes
 * on jsdk 2.0.
 */

public class ServletOutputStreamImpl extends ServletOutputStream
{
    /**
     * the wrapped Writer
     */
    private PrintWriter out = null;

    public ServletOutputStreamImpl (PrintWriter jspWriter) { 
	out = jspWriter;
    }
    
    public void write(int i) {
	out.write(i);
    }
    
    public void print(String s) throws IOException {
	out.print(s);
    }

    public void print(boolean b) throws IOException {
	out.print(b);
    }

    public void print(char c) throws IOException {
	out.print(String.valueOf(c));
    }

    public void print(int i) throws IOException {
	out.print(i);
    }
 
    public void print(long l) throws IOException {
	out.print(l);
    }

    public void print(float f) throws IOException {
	out.print(f);
    }

    public void print(double d) throws IOException {
	out.print(d);
    }

    public void println() throws IOException {
	out.println();
    }
}
