//========================================================================
//$Id$
//Copyright 2004 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at 
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================

package org.mortbay.util;

import java.io.PrintStream;

/**
 * @author gregw
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class LogStream extends PrintStream
{
    public static class STDERR extends LogStream
    {STDERR() {super("STDERR ");}}

    public static class STDOUT extends LogStream
    {STDOUT() {super("STDOUT ");}} 

    /*-------------------------------------------------------------------*/
    final static PrintStream STDERR_STREAM=System.err;
    final static PrintStream STDOUT_STREAM=System.out;
    
    /* ------------------------------------------------------------ */
    /** Log standard error stream.
     * If set to true, output to stderr will be directed to an instance
     * of LogStream and logged.  Beware of log loops from logs that write to stderr.
     */
    public static void setLogStdErr(boolean log)
    {
        if (log)
        {
            if (!(System.err instanceof LogStream))
                System.setErr(new LogStream.STDERR());
        }
        else
            System.setErr(STDERR_STREAM);
    }

    /* ------------------------------------------------------------ */
    public static boolean getLogStdErr()
    {
        return System.err instanceof LogStream;
    }
    
    /* ------------------------------------------------------------ */
    /** Log standard output stream.
     * If set to true, output to stdout will be directed to an instance
     * of LogStream and logged.  Beware of log loops from logs that write to stdout.
     */
    public static void setLogStdOut(boolean log)
    {
        if (log)
        {
            if (!(System.out instanceof LogStream))
                System.setOut(new LogStream.STDOUT());
        }
        else
            System.setOut(STDOUT_STREAM);
    }

    /* ------------------------------------------------------------ */
    public static boolean getLogStdOut()
    {
        return System.out instanceof LogStream;
    }

    /* ------------------------------------------------------------ */
    private String tag;
    private ByteArrayOutputStream2 bout;
    
    /* 
     */
    public void flush()
    {
        super.flush();
        if (bout.size()>0)
        {
            String s=new String(bout.getBuf(),0,bout.size()).trim();
            if (s.length()>0)
                Log.message(tag,s,new Frame());
        }
        bout.reset();
    }
 
    /**
     * @param out
     * @param autoflush
     */
    public LogStream(String tag)
    {
        super(new ByteArrayOutputStream2(128), true);
        bout=(ByteArrayOutputStream2)this.out;
        this.tag=tag;
    }
    
    public void close()
    {
        flush();
        super.close();
    }
    public void println()
    {
        super.println();
        flush();
    }
    public void println(boolean arg0)
    {
        super.println(arg0);
        flush();
    }
    public void println(char arg0)
    {
        super.println(arg0);
        flush();
    }
    public void println(char[] arg0)
    {
        super.println(arg0);
        flush();
    }
    public void println(double arg0)
    {
        super.println(arg0);
        flush();
    }
    public void println(float arg0)
    {
        super.println(arg0);
        flush();
    }
    public void println(int arg0)
    {
        super.println(arg0);
        flush();
    }
    public void println(long arg0)
    {
        super.println(arg0);
        flush();
    }
    public void println(Object arg0)
    {
        super.println(arg0);
        flush();
    }
    public void println(String arg0)
    {
        super.println(arg0);
        flush();
    }
    public void write(byte[] arg0, int arg1, int arg2)
    {
        super.write(arg0, arg1, arg2);
        flush();
    }

}
