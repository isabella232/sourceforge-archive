// ========================================================================
// Copyright (c) 2000 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.jetty.servlet;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.io.UnsupportedEncodingException;
import org.mortbay.util.IO;
import org.mortbay.util.Code;


/* ------------------------------------------------------------ */
/** Servlet PrintWriter.
 * This writer can be disabled.
 * It is crying out for optimization.
 *
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
class ServletWriter extends PrintWriter
{
    String encoding=null;
    OutputStream os=null;
    boolean written=false;
    
    /* ------------------------------------------------------------ */
    ServletWriter(OutputStream os, String encoding)
        throws IOException
    {
        super(new OutputStreamWriter(os,encoding));
        this.os=os;
        this.encoding=encoding;
    }

    /* ------------------------------------------------------------ */
    public void disable()
    {
        out=IO.getNullWriter();
    }
    
    /* ------------------------------------------------------------ */
    public void reset()
    {
        try{
            out=IO.getNullWriter();
            super.flush();
            out=new OutputStreamWriter(os,encoding);
            written=false;
        }
        catch(UnsupportedEncodingException e)
        {
            Code.fail(e);
        }
    }
    

    /* ------------------------------------------------------------ */
    public boolean isWritten()
    {
        return written;
    }

    /* ------------------------------------------------------------ */
    public void print(boolean p)  {written=true;super.print(p);}
    public void print(char p)     {written=true;super.print(p);}
    public void print(char[] p)   {written=true;super.print(p);}
    public void print(double p)   {written=true;super.print(p);}
    public void print(float p)    {written=true;super.print(p);}
    public void print(int p)      {written=true;super.print(p);}
    public void print(long p)     {written=true;super.print(p);}
    public void print(Object p)   {written=true;super.print(p);}
    public void print(String p)   {written=true;super.print(p);}
    public void println()         {written=true;super.println();}
    public void println(boolean p){written=true;super.println(p);}
    public void println(char p)   {written=true;super.println(p);}
    public void println(char[] p) {written=true;super.println(p);}
    public void println(double p) {written=true;super.println(p);}
    public void println(float p)  {written=true;super.println(p);}
    public void println(int p)    {written=true;super.println(p);}
    public void println(long p)   {written=true;super.println(p);}
    public void println(Object p) {written=true;super.println(p);}
    public void println(String p) {written=true;super.println(p);}
    public void write(int c)      {written=true;super.write(c);}
    public void write(char[] cbuf, int off, int len){written=true;super.write(cbuf,off,len);}
    public void write(char[] cbuf){written=true;super.write(cbuf);}
    public void write(String s, int off, int len){written=true;super.write(s,off,len);}
    public void write(String s)   {written=true;super.write(s);}
}





