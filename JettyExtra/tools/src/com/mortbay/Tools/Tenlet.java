// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.Tools;
import com.mortbay.Util.Code;
import com.mortbay.Util.IO;
import com.mortbay.Util.InetAddrPort;
import com.mortbay.Util.ThreadedServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

/* ------------------------------------------------------------ */
/** Reverse Telnet class
 * 
 * 
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class Tenlet extends ThreadedServer 
{
    /* ------------------------------------------------------------ */
    public Tenlet(InetAddrPort addr)
        throws IOException
    {
        super(addr);
        setName("Tenlet");
    }
    
    /* ------------------------------------------------------------ */
    protected void handleConnection(InputStream in,OutputStream out)
    {
        try
        {
            System.err.println("Conected "+Thread.currentThread());
            IO.copyThread(System.in,out);
            IO.copy(in,System.err);
        }
        catch(IOException e)
        {
            Code.warning(e);
        }
        finally
        {
            System.err.println("EOF\n");
        }
    }
    
    
    /* ------------------------------------------------------------ */
    public static void main(String[] arg)
    {
        try
        {    
            if (arg.length!=1)
            {
                System.err.println("Usage - java com.mortbay.Util.Tenlet [addr]:port");
                System.exit(1);
            }
            
            Tenlet tenlet=new Tenlet(new InetAddrPort(arg[0]));
            tenlet.start();
            tenlet.join();
        }
        catch(Exception e)
        {
            Code.warning(e);
        }
    }
}
