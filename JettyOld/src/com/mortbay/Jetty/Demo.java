// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.Jetty;

import com.mortbay.Base.*;
import com.mortbay.Util.*;
import com.mortbay.Servlets.*;
import com.mortbay.HTML.*;
import com.mortbay.HTTP.*;
import com.mortbay.HTTP.Handler.*;
import com.mortbay.HTTP.Filter.*;
import com.mortbay.HTTP.Configure.*;
import java.io.*;
import java.net.*;
import javax.servlet.*;
import java.util.*;


/* ------------------------------------------------------------ */
/** Demo Jetty Server
 *
 *
 * @version 1.0 Sun Jun 27 1999
 * @author Greg Wilkins (gregw)
 */
public class Demo
{
    /* -------------------------------------------------------- */
    /** Main
     * Configures the Dump servlet and starts the server
     */
    public static void main(String args[])
    {
        if (args.length==1)
            Code.warning("Port argument no longer supported. See etc/JettyDemo.prp");

        String filename = "JettyDemo.prp";
        {
            File cwd = new File(System.getProperty("user.dir"));
            File filebase = new File(cwd,"FileBase");
            Code.assert(filebase.isDirectory(),"Directory \"" + filebase.getAbsolutePath() + "\" not found!");
            File etc = new File(cwd,"etc");
            Code.assert(etc.isDirectory(),"Directory \"" + etc.getAbsolutePath() + "\" not found!");
            File prp = new File(etc,filename);
            Code.assert(prp.exists(),"File \"" + prp.getAbsolutePath() + "\" not found!");
            filename = prp.getAbsolutePath();
        }

        String demoArgs[] = {filename};
        Server.main(demoArgs);
    }
}
